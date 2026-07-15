package com.example.moamap.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.moamap.core.designsystem.theme.MoaMapTheme

/**
 * 모음 탭. 화면 구성은 후속 작업에서 채운다.
 */
@Composable
fun CollectionScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "모음",
            style = MoaMapTheme.typography.title2,
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionScreenPreview() {
    MoaMapTheme {
        CollectionScreen()
    }
}
