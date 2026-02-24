package com.example.iwawwaalarm

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.icu.util.Calendar
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private fun getTopicName(): String {
            return "iwawwaalarm_topic"
        }
        public fun subscribeToTopic() {
            FirebaseMessaging.getInstance().subscribeToTopic(getTopicName())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("FCM", "Переподписка на новый токен успешна")
                    }
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM_DEBUG", "Сообщение получено!")
        val prefs = getSharedPreferences(AlarmPrefs.PREFS_NAME, MODE_PRIVATE)

        val isOn = prefs.getBoolean(AlarmPrefs.KEY_IS_ON, false)
        val startHour = prefs.getInt(AlarmPrefs.KEY_START_HOUR, 8)
        val startMinute = prefs.getInt(AlarmPrefs.KEY_START_MINUTE, 0)
        val endHour = prefs.getInt(AlarmPrefs.KEY_END_HOUR, 23)
        val endMinute = prefs.getInt(AlarmPrefs.KEY_END_MINUTE, 0)

        // Если мониторинг выключен — ничего не делаем
        if (!isOn) return
        Log.d("FCM_DEBUG", "Мониторинг работает")
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        // Проверка попадания в диапазон
        var isAllowed = isTimeAllowed(startHour,startMinute,endHour,endMinute)
        // TODO: красивые разметки
        if (!isAllowed)  return

        // Проверяем, что в данных (data) пришел наш тип
        if (remoteMessage.data["type"] == "TWITCH_ALARM") {
            val streamTitle = remoteMessage.data["streamTitle"] ?: "Стрим начался!"
            Log.d("FCM_DEBUG", "Имя стрима: ${streamTitle}")
            AlarmHelper.getTriggerNotification(this, "Тестовый будильник!")
        }
    }

    private fun isTimeAllowed(startH: Int, startM: Int, endH: Int, endM: Int): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentH = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentM = calendar.get(java.util.Calendar.MINUTE)

        // Переводим всё в минуты от начала суток
        val now = currentH * 60 + currentM
        val start = startH * 60 + startM
        val end = endH * 60 + endM

        return if (start < end) {
            // Обычный интервал: сейчас внутри [start, end)
            now in start until end
        } else if (start > end) {
            // Интервал через полночь: сейчас либо ПОСЛЕ старта, либо ДО конца
            now >= start || now < end
        } else {
            // Если старт и конец равны — разрешено всегда (или запрещено, на ваш выбор)
            true
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Этот метод вызывается, когда Firebase выдает устройству новый уникальный токен.
        // Поскольку мы используем подписку на ТЕМУ (topic), нам нужно
        // переподписаться, чтобы гарантировать доставку на новый токен.

        subscribeToTopic()
    }

}
