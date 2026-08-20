package com.moamap.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.navigation.MoaMapNavHost
import com.moamap.app.feature.collection.share.SharedLink
import com.moamap.app.feature.collection.share.SharedLinkParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** 다른 앱이 공유해 온 링크. 화면이 소비할 때까지 들고 있는다. */
    private var pendingShare by mutableStateOf<SharedLink?>(null)

    /**
     * 이 인텐트의 공유를 이미 처리했는지.
     *
     * 화면을 돌리면 Activity 가 다시 만들어지는데 인텐트는 그대로 남아 있다. 이 값이
     * 없으면 회전할 때마다 같은 링크로 장소 가져오기에 또 들어간다.
     */
    private var shareHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shareHandled = savedInstanceState?.getBoolean(KEY_SHARE_HANDLED) == true
        if (!shareHandled) {
            pendingShare = intent.toSharedLink()
        }

        enableEdgeToEdge()
        setContent {
            MoaMapTheme {
                MoaMapNavHost(
                    modifier = Modifier.fillMaxSize(),
                    pendingShare = pendingShare,
                    onShareHandled = {
                        shareHandled = true
                        pendingShare = null
                    },
                )
            }
        }
    }

    /**
     * 앱이 떠 있는 동안 들어온 공유.
     *
     * `launchMode="singleTask"` 라 인스턴스가 하나로 유지되고 여기로 들어온다. 표준
     * 모드면 [MainActivity] 가 하나 더 생겨 보던 화면과 백스택이 갈라진다.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // 이후 회전으로 재생성될 때 읽는 인텐트도 새것이어야 한다.
        setIntent(intent)
        shareHandled = false
        pendingShare = intent.toSharedLink()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SHARE_HANDLED, shareHandled)
    }

    private companion object {
        const val KEY_SHARE_HANDLED = "share_handled"
    }
}

/**
 * 공유가 아닌 인텐트는 null 이다.
 *
 * 여기서 걸러야 런처로 연 평범한 실행이 공유로 오해되지 않는다. 링크를 알아보지
 * 못한 경우는 null 이 아니라 [SharedLink.Unsupported] 이며, 그건 안내로 이어진다.
 */
private fun Intent.toSharedLink(): SharedLink? =
    if (action == Intent.ACTION_SEND && type == MIME_TEXT_PLAIN) {
        SharedLinkParser.parse(getStringExtra(Intent.EXTRA_TEXT))
    } else {
        null
    }

/** 인스타그램·네이버 지도·카카오맵·구글 지도 모두 이 타입 하나로 공유해 온다. */
private const val MIME_TEXT_PLAIN = "text/plain"
