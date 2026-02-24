package com.example.iwawwaalarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch


object AlarmPrefs {
    const val PREFS_NAME = "alarm_settings"
    const val KEY_IS_ON = "is_monitoring_on"
    const val KEY_START_HOUR = "start_hour"
    const val KEY_START_MINUTE = "start_minute"
    const val KEY_END_HOUR = "end_hour"
    const val KEY_END_MINUTE = "end_minute"
}

class AlarmActivity : AppCompatActivity() {
    private var yDown = 0f // Точка, где палец коснулся экрана

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Если мы пришли сюда по кнопке "ОТКЛЮЧИТЬ"
        if (intent.getStringExtra("ACTION") == "STOP_ALARM") {
            AlarmHelper.stopAlarm(this)
            finish()
            return
        }

        // Специально для Xiaomi/Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm)

        val rootLayout = findViewById<View>(R.id.alarmRoot)

        rootLayout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    yDown = event.rawY // Запоминаем старт
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - yDown
                    // Если тянем ВВЕРХ (deltaY отрицательный)
                    if (deltaY < 0) {
                        view.translationY = deltaY // Двигаем экран за пальцем
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Если протащили больше чем на 1/4 экрана вверх — закрываем
                    if (view.translationY < -view.height / 4) {
                        animateExit(view)
                    } else {
                        // Иначе возвращаем экран на место с пружинкой
                        view.animate()
                            .translationY(0f)
                            .setDuration(200)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }

        val ivArrow = findViewById<ImageView>(R.id.ivArrowUp)

        // Анимация движения вверх-вниз (плавная подсказка)
        val animation = android.view.animation.TranslateAnimation(0f, 0f, 0f, -30f)
        animation.duration = 800
        animation.repeatMode = android.view.animation.Animation.REVERSE
        animation.repeatCount = android.view.animation.Animation.INFINITE
        ivArrow.startAnimation(animation)
    }

    private fun animateExit(view: View) {
        view.animate()
            .translationY(-view.height.toFloat()) // Вылетаем до конца вверх
            .alpha(0f) // Исчезаем
            .setDuration(300)
            .withEndAction {
                AlarmHelper.stopAlarm(this)
                finish()
            }
            .start()
    }
}
