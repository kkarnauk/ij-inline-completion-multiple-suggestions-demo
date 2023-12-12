@file:Suppress("UnstableApiUsage")

package io.github.kkarnauk.inline.completion.demo

import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.codeInsight.inline.completion.InlineCompletion
import com.intellij.codeInsight.inline.completion.InlineCompletionEvent
import com.intellij.codeInsight.inline.completion.InlineCompletionRequest
import com.intellij.codeInsight.inline.completion.session.InlineCompletionContext
import com.intellij.codeInsight.inline.completion.session.InlineCompletionSession
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.openapi.util.Key


private enum class InlineCompletionSwitchSuggestionMode {
  Next,
  Prev
}

// Saves information about direction where we switch a suggestion
private val INLINE_COMPLETION_SWITCH_SUGGESTION_MODE_KEY = Key.create<InlineCompletionSwitchSuggestionMode>(
  "inline.completion.switch.suggestion.mode"
)

fun InlineCompletionRequest.isSwitchToNextSuggestion(): Boolean {
  return getUserData(INLINE_COMPLETION_SWITCH_SUGGESTION_MODE_KEY) == InlineCompletionSwitchSuggestionMode.Next
}

fun InlineCompletionRequest.isSwitchToPrevSuggestion(): Boolean {
  return getUserData(INLINE_COMPLETION_SWITCH_SUGGESTION_MODE_KEY) == InlineCompletionSwitchSuggestionMode.Prev
}

// Events

sealed class SwitchInlineCompletionSuggestionEvent(
  private val originalRequest: InlineCompletionRequest,
  private val mode: InlineCompletionSwitchSuggestionMode
) : InlineCompletionEvent {

  override fun toRequest(): InlineCompletionRequest {
    // Copying everything to correctly put information about direction
    val request = originalRequest.apply {
      InlineCompletionRequest(event, file, editor, document, startOffset, endOffset, lookupElement)
    }
    request.putUserData(INLINE_COMPLETION_SWITCH_SUGGESTION_MODE_KEY, mode)
    return request
  }
}

class NextInlineCompletionSuggestionEvent(
  originalRequest: InlineCompletionRequest
) : SwitchInlineCompletionSuggestionEvent(originalRequest, InlineCompletionSwitchSuggestionMode.Next)

class PrevInlineCompletionSuggestionEvent(
  originalRequest: InlineCompletionRequest
) : SwitchInlineCompletionSuggestionEvent(originalRequest, InlineCompletionSwitchSuggestionMode.Prev)


// Actions

sealed class SwitchInlineCompletionSuggestionAction(
  mode: InlineCompletionSwitchSuggestionMode
) : EditorAction(Handler(mode)), HintManagerImpl.ActionToIgnore {

  private class Handler(private val mode: InlineCompletionSwitchSuggestionMode) : EditorWriteActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
      // Acquiring current inline completion session
      val session = InlineCompletionSession.getOrNull(editor) ?: return

      // We need the original request to create our event
      val request = session.request

      val event = when (mode) {
        InlineCompletionSwitchSuggestionMode.Next -> NextInlineCompletionSuggestionEvent(request)
        InlineCompletionSwitchSuggestionMode.Prev -> PrevInlineCompletionSuggestionEvent(request)
      }

      // Sending our new event to handler
      InlineCompletion.getHandlerOrNull(editor)?.invokeEvent(event)
    }
  }
}

class NextInlineCompletionSuggestionAction :
  SwitchInlineCompletionSuggestionAction(InlineCompletionSwitchSuggestionMode.Next)

class PrevInlineCompletionSuggestionAction :
  SwitchInlineCompletionSuggestionAction(InlineCompletionSwitchSuggestionMode.Prev)


private class SwitchInlineCompletionSuggestionActionsPromoter : ActionPromoter {
  override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction> {
    val editor = CommonDataKeys.EDITOR.getData(context) ?: return emptyList()

    if (InlineCompletionContext.getOrNull(editor) == null) {
      return emptyList()
    }

    return actions.filterIsInstance<SwitchInlineCompletionSuggestionAction>()
  }
}
