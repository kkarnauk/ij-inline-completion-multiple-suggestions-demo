@file:Suppress("UnstableApiUsage")

package io.github.kkarnauk.inline.completion.demo

import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.openapi.util.Key
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MultipleSuggestionInlineCompletionProvider : InlineCompletionProvider {

  // Maintains the current index of suggestion
  private var currentIndex = 0

  override val id = InlineCompletionProviderID("MultipleSuggestionInlineCompletionProvider")

  override fun isEnabled(event: InlineCompletionEvent): Boolean {
    return event is SwitchInlineCompletionSuggestionEvent || // custom event to switch suggestions
            event is InlineCompletionEvent.DocumentChange && event.typing is TypingEvent.NewLine // type a new line
  }

  override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
    when {
      request.isSwitchToNextSuggestion() -> currentIndex++
      request.isSwitchToPrevSuggestion() -> currentIndex--
      else -> currentIndex = 0
    }
    currentIndex = (currentIndex + suggestions.size) % suggestions.size
    return object : InlineCompletionSuggestion() {
      override val suggestionFlow: Flow<InlineCompletionElement> = flow {
        putUserData(SUGGESTION_INDEX_KEY, currentIndex)
        emit(InlineCompletionGrayTextElement(suggestions[currentIndex]))
      }
    }
  }

  companion object {
    private val suggestions = listOf(
      "fun method_first(arguments: Array<String>)",
      "fun method_second(arguments: Array<String>)",
      "fun method_third(arguments: Array<String>)",
      "fun method_fourth(arguments: Array<String>)"
    )

    private val SUGGESTION_INDEX_KEY = Key.create<Int>("SUGGESTION_INDEX")
  }
}
