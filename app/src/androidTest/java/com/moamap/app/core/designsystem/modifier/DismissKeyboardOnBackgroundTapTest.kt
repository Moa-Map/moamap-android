package com.moamap.app.core.designsystem.modifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DismissKeyboardOnBackgroundTapTest {
    @get:Rule val compose = createComposeRule()
    private var clicks = 0

    private fun showContent() {
        compose.setContent {
            Column(Modifier.fillMaxSize().dismissKeyboardOnBackgroundTap()) {
                repeat(2) { index ->
                    val text = remember { mutableStateOf("") }
                    BasicTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        modifier = Modifier.size(160.dp, 56.dp).testTag("field$index"),
                    )
                }
                Box(Modifier.size(80.dp).testTag("button").clickable { clicks++ })
                Box(Modifier.size(80.dp).testTag("background"))
            }
        }
    }

    @Test fun backgroundTapClearsFocusAndPreservesText() {
        showContent()
        compose.onNodeWithTag("field0").performTouchInput { click() }
        compose.onNodeWithTag("field0").performTextInput("draft")
        compose.onNodeWithTag("background").performTouchInput { click() }
        compose.onNodeWithTag("field0").assertIsNotFocused().assertTextEquals("draft")
        compose.onNodeWithTag("field0").performTouchInput { click() }
        compose.onNodeWithTag("field0").assertIsFocused()
    }

    @Test fun fieldAndButtonTapsAreHandledByChildren() {
        showContent()
        compose.onNodeWithTag("field0").performTouchInput { click() }
        compose.onNodeWithTag("field1").performTouchInput { click() }
        compose.onNodeWithTag("field1").assertIsFocused()
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
