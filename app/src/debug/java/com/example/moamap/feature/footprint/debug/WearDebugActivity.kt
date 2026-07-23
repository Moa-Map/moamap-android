package com.example.moamap.feature.footprint.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 워치에서 받은 세션을 확인하고 JSON 을 빼내는 개발용 화면.
 * debug 소스셋에만 존재하므로 release 빌드에는 포함되지 않는다.
 */
@AndroidEntryPoint
class WearDebugActivity : ComponentActivity() {

    // by viewModels() 는 프로퍼티 위임이라 함수 안에서 선언할 수 없다.
    private val viewModel: WearDebugViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoaMapTheme {
                WearDebugScreen(onShare = ::shareSession, viewModel = viewModel)
            }
        }
    }

    private fun shareSession(session: ReceivedWalkSession) {
        lifecycleScope.launch {
            val file = viewModel.exportForShare(session)
            val uri = FileProvider.getUriForFile(this@WearDebugActivity, "$packageName.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "세션 JSON 공유"))
        }
    }
}
