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
import androidx.wear.compose.material3.MaterialTheme
import com.example.moamap.wear.record.RecordScreen
import com.example.moamap.wear.record.RecordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: RecordViewModel = hiltViewModel()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { results ->
                    if (results.values.any { granted -> !granted }) {
                        viewModel.onPermissionDenied("위치와 심박 권한이 있어야 기록할 수 있어요")
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(requiredPermissions())
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
}
