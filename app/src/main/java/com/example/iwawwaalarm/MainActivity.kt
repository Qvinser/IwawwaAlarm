package com.example.iwawwaalarm

import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val general_prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        // val isFirstRun = true
        val isFirstRun = general_prefs.getBoolean("is_first_run", true)

        if (isFirstRun) {
            MyFirebaseMessagingService.subscribeToTopic()
            general_prefs.edit().putBoolean("is_first_run", false).apply()
        }


        val prefs = getSharedPreferences(AlarmPrefs.PREFS_NAME, MODE_PRIVATE)

        // 1. Установка начальных значений при открытии экрана
        val switchService = findViewById<MaterialSwitch>(R.id.switchService)
        val btnStart = findViewById<Button>(R.id.btnStartTime)
        val btnEnd = findViewById<Button>(R.id.btnEndTime)
        val testAlarm = findViewById<Button>(R.id.testAlarm)

        switchService.isChecked = prefs.getBoolean(AlarmPrefs.KEY_IS_ON, false)
        btnStart.text = String.format("%02d:%02d",
            prefs.getInt(AlarmPrefs.KEY_START_HOUR, 8),
            prefs.getInt(AlarmPrefs.KEY_START_MINUTE, 0))

        btnEnd.text = String.format("%02d:%02d",
            prefs.getInt(AlarmPrefs.KEY_END_HOUR, 23),
            prefs.getInt(AlarmPrefs.KEY_END_MINUTE, 0))

        // 2. Сохранение состояния переключателя
        switchService.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(AlarmPrefs.KEY_IS_ON, isChecked).apply()
        }

        // 3. Сохранение времени (пример для выбора часа)
        btnStart.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                btnStart.text = String.format("%02d:%02d", hour, minute)
                prefs.edit().putInt(AlarmPrefs.KEY_START_HOUR, hour).apply()
                prefs.edit().putInt(AlarmPrefs.KEY_START_MINUTE, minute).apply()
            },
                prefs.getInt(AlarmPrefs.KEY_START_HOUR, 8),
                prefs.getInt(AlarmPrefs.KEY_START_MINUTE, 0),
                true).show()
        }

        btnEnd.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                btnEnd.text = String.format("%02d:%02d", hour, minute)
                prefs.edit().putInt(AlarmPrefs.KEY_END_HOUR, hour).apply()
                prefs.edit().putInt(AlarmPrefs.KEY_END_MINUTE, minute).apply()
            },
                prefs.getInt(AlarmPrefs.KEY_END_HOUR, 8),
                prefs.getInt(AlarmPrefs.KEY_END_MINUTE, 0),
                true).show()
        }

        testAlarm.setOnClickListener {
            AlarmHelper.getTriggerNotification(this, "Тестовый будильник!")
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets }

        // Запросить Разрешения
        val packageName = packageName

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        //val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        //    data = Uri.fromParts("package", packageName, null)
        //}
        //startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}