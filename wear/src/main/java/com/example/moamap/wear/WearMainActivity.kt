package com.example.moamap.wear

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moamap.wear.record.RecordScreen
import com.example.moamap.wear.record.RecordViewModel
import com.example.moamap.wear.theme.MoaWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoaWearTheme {
                val viewModel: RecordViewModel = hiltViewModel()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { results ->
                    // 알림 권한이 거부돼도 기록은 그대로 돌아간다. 필수 권한만 보고 막는다 -
                    // 여기서 전체를 보면 알림을 거절한 사용자가 기록 자체를 못 하게 된다.
                    if (requiredPermissions().any { results[it] == false }) {
                        viewModel.onPermissionDenied("위치와 심박 권한이 있어야 기록할 수 있어요")
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(requiredPermissions() + optionalPermissions())
                }

                RecordScreen(viewModel = viewModel)
            }
        }
    }

    /**
     * 심박 권한 이름이 API 레벨에 따라 다르다.
     * API 35 이하는 BODY_SENSORS, API 36 이상은 health.READ_HEART_RATE 를 쓴다.
     */
    private fun requiredPermissions(): Array<String> {
        val heartRatePermission = if (Build.VERSION.SDK_INT >= 36) {
            "android.permission.health.READ_HEART_RATE"
        } else {
            Manifest.permission.BODY_SENSORS
        }
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, heartRatePermission)
    }

    /**
     * 없어도 기록은 되는 권한. 알림이 없으면 기록 중 포그라운드 알림이 보이지 않을 뿐,
     * 프로세스는 그대로 살아 있어 수집은 계속된다.
     */
    private fun optionalPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
}
