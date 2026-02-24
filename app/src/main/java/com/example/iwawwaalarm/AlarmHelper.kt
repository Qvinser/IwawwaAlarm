package com.example.iwawwaalarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground



object AlarmHelper {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private const val CHANNEL_ID = "ALARM_CHANNEL" // Обновили ID для сброса настроек
    public const val NOTIFICATION_ID = 1

    fun getTriggerNotification(context: Context, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Создание канала (обязательно для Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Iwawwa Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о начале стримов"
                setSound(null, null) // Отключаем системный писк, играем свой звук
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                setBypassDnd(true) // Пробивать режим "Не беспокоить"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Подготовка Intent для открытия экрана будильника
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("title", title)
            putExtra("ACTION", "ALARM_START")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Подготовка Intent для кнопки "ОТКЛЮЧИТЬ" в уведомлении
        val stopIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("ACTION", "STOP_ALARM")
        }
        val stopPendingIntent = PendingIntent.getActivity(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent, который сработает ПРИ УДАЛЕНИИ (смахивании) уведомления
        val deleteIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ACTION", "STOP_ALARM")
        }
        val deletePendingIntent = PendingIntent.getActivity(
            context, 2, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Сборка уведомления
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Стрим начался!")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Авто-запуск экрана
            .addAction(R.drawable.ic_alarm, "ОТКЛЮЧИТЬ", stopPendingIntent) // Кнопка в шторке
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)

        // 5. Запуск звука
        startSound(context)

        val notification = builder.build()

        // 6. Вывод уведомления
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun startSound(context: Context) {
        if (mediaPlayer == null) {
            try {
                // Берем WakeLock, чтобы телефон не уснул в процессе
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmApp:WakeLock")
                wakeLock?.acquire(10 * 60 * 1000L) // Максимум на 10 минут

                val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alert)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopAlarm(context: Context) {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
            mediaPlayer = null

            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAppIconBitmap(context: Context): Bitmap {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)

        // Если это уже BitmapDrawable, просто возвращаем bitmap
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            return drawable.bitmap
        }

        // Если это AdaptiveIcon (XML), рисуем его на холсте
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
