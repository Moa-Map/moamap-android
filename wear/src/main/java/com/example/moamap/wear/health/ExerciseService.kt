package com.example.moamap.wear.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.moamap.wear.R
import com.example.moamap.wear.WearMainActivity

/**
 * 기록이 도는 동안 프로세스를 포그라운드에 붙잡아 두는 서비스.
 *
 * Wear OS 는 화면이 꺼지면 포그라운드 서비스가 없는 프로세스를 얼려버리고, 그 순간
 * [ExerciseUpdateCallback][androidx.health.services.client.ExerciseUpdateCallback] 배달이 멈춘다.
 * 이게 없으면 한 시간짜리 산책이 손목을 내리기 전 몇십 초짜리 기록으로 잘린다.
 *
 * 이 서비스는 센서를 직접 다루지 않는다 — 수집은 [ExerciseRecorder] 가 하고 여기서는 수명만 잡는다.
 * 그래서 바인딩도 받지 않는다.
 */
class ExerciseService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService 로 시작된 뒤 5초 안에 승격하지 않으면 시스템이 앱을 죽인다.
        // 그래서 여기서 다른 일을 하기 전에 가장 먼저 호출한다.
        startForeground(NOTIFICATION_ID, buildNotification(), foregroundServiceType())

        // 프로세스가 죽으면 메모리에 있던 샘플도 함께 사라져 되살릴 세션이 없다.
        // STICKY 로 두면 빈 세션의 알림만 되살아난다.
        return START_NOT_STICKY
    }

    /**
     * health 타입은 API 34 에서 생겼다. minSdk 가 30 이므로 그 아래에서는 location 만 쓴다 —
     * 선언하지 않은 타입을 넘기면 승격 자체가 거부된다.
     */
    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.record_channel_name),
                    // 기록 중에는 매번 소리를 낼 이유가 없다. 존재만 알리면 된다.
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WearMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record_ongoing)
            .setContentTitle(getString(R.string.record_notification_title))
            .setContentText(getString(R.string.record_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "record_ongoing"
        private const val NOTIFICATION_ID = 1

        /**
         * 반드시 앱이 포그라운드일 때 호출해야 한다 — 백그라운드에서 시작하면
         * ForegroundServiceStartNotAllowedException 이 난다. 기록 시작은 사용자 탭이라 안전하다.
         */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, ExerciseService::class.java))
        }

        /** 돌고 있지 않으면 아무 일도 하지 않는다. */
        fun stop(context: Context) {
            context.stopService(Intent(context, ExerciseService::class.java))
        }
    }
}
