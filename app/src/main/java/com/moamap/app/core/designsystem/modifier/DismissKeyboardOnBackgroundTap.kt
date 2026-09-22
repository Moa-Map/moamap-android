package com.moamap.app.core.designsystem.modifier

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** 자식이 처리하지 않은 배경 탭에서만 입력을 종료한다. 모달 내부에서 호출한다. */
@Composable
fun Modifier.dismissKeyboardOnBackgroundTap(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return pointerInput(focusManager, keyboardController) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
            keyboardController?.hide()
        })
    }
}
