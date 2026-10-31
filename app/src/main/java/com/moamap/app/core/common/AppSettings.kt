package com.moamap.app.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** 권한을 두 번 거부하면 팝업이 다시 뜨지 않는다. 그때는 설정에서 직접 켜야 한다. */
internal fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
