package com.example.nexsus_ai

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : FlutterActivity() {

    companion object {
        private const val CHANNEL = "com.nexsusai.app/system"

        private const val ALARM_REQUEST_CODE = 9001
        private const val ALARM_SHOW_REQUEST_CODE = 9002

        private const val NOTIFICATION_CHANNEL = "nexsus_alarm"
        private const val LARGE_FILE_PREFS = "nexsus_large_files"
        private const val DEFAULT_FILE_APP = "default_file_app"

        private const val MEDIA_PERMISSION_REQUEST = 3001
        private const val NOTIFICATION_PERMISSION_REQUEST = 3002
    }

    private var pendingMediaScanResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(
        flutterEngine: FlutterEngine
    ) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->

            when (call.method) {

                // =====================================================
                // DEVICE
                // =====================================================

                "getDeviceInfo" -> {
                    result.success(getDeviceInfo())
                }

                // =====================================================
                // ALARM
                // =====================================================

                "setAlarm" -> {
                    val hour =
                        call.argument<Int>("hour") ?: 0

                    val minute =
                        call.argument<Int>("minute") ?: 0

                    setAlarm(
                        hour,
                        minute,
                        result
                    )
                }

                "cancelAlarm" -> {
                    cancelAlarm(result)
                }

                // =====================================================
                // STORAGE
                // =====================================================

                "openStorageSettings" -> {
                    openStorageSettings()
                    result.success(true)
                }

                "scanLargeFiles" -> {
                    scanLargeFiles(result)
                }

                "openScannedFile" -> {

                    val uri =
                        call.argument<String>("uri")

                    val mimeType =
                        call.argument<String>("mimeType")

                    if (uri == null) {

                        result.error(
                            "INVALID_URI",
                            "File URI is missing.",
                            null
                        )

                    } else {

                        openScannedFile(
                            uri,
                            mimeType
                        )

                        result.success(true)
                    }
                }

                // =====================================================
                // ALARM SETTINGS
                // =====================================================

                "openExactAlarmSettings" -> {
                    openExactAlarmSettings()
                    result.success(true)
                }

                // =====================================================
                // AOD
                // =====================================================

                "openAodSettings" -> {
                    openAodSettings()
                    result.success(true)
                }

                "startNexsusAod" -> {
                    startNexsusAod(
                        call,
                        result
                    )
                }

                "stopNexsusAod" -> {

                    getSharedPreferences(
                        "nexsus_aod",
                        Context.MODE_PRIVATE
                    )
                        .edit()
                        .putBoolean(
                            "aod_enabled",
                            false
                        )
                        .apply()

                    NexsusAodService.stopService(this)

                    result.success(true)
                }

                "openAodPermission" -> {
                    openAodPermission()
                    result.success(true)
                }

                "openDisplaySettings" -> {
                    openDisplaySettings()
                    result.success(true)
                }

                // =====================================================
                // NOTIFICATION ACCESS
                // =====================================================

                "openNotificationAccess" -> {
                    openNotificationAccessSettings()
                    result.success(true)
                }

                "isNotificationAccessEnabled" -> {
                    result.success(
                        isNotificationAccessEnabled()
                    )
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    // =================================================================
    // DEVICE INFORMATION
    // =================================================================

    private fun getDeviceInfo(): Map<String, Any> {

        val batteryManager =
            getSystemService(
                Context.BATTERY_SERVICE
            ) as BatteryManager

        val battery =
            batteryManager
                .getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )
                .coerceIn(0, 100)

        val activityManager =
            getSystemService(
                Context.ACTIVITY_SERVICE
            ) as ActivityManager

        val memoryInfo =
            ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(
            memoryInfo
        )

        val ramTotal =
            memoryInfo.totalMem
                .coerceAtLeast(0L)

        val ramFree =
            memoryInfo.availMem
                .coerceAtLeast(0L)

        val ramUsed =
            (ramTotal - ramFree)
                .coerceAtLeast(0L)

        val ramPercent =
            if (ramTotal > 0L) {

                (
                    (
                        ramUsed.toDouble() /
                            ramTotal.toDouble()
                    ) * 100.0
                )
                    .toInt()
                    .coerceIn(0, 100)

            } else {
                0
            }

        val statFs =
            StatFs(
                Environment
                    .getDataDirectory()
                    .path
            )

        val storageTotal =
            statFs.totalBytes
                .coerceAtLeast(0L)

        val storageFree =
            statFs.availableBytes
                .coerceAtLeast(0L)

        val storageUsed =
            (storageTotal - storageFree)
                .coerceAtLeast(0L)

        val storagePercent =
            if (storageTotal > 0L) {

                (
                    (
                        storageUsed.toDouble() /
                            storageTotal.toDouble()
                    ) * 100.0
                )
                    .toInt()
                    .coerceIn(0, 100)

            } else {
                0
            }

        val alarmManager =
            getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val systemAlarm =
            try {
                alarmManager.nextAlarmClock
            } catch (_: Exception) {
                null
            }

        val savedAlarm =
            getSharedPreferences(
                "nexsus_alarm",
                Context.MODE_PRIVATE
            )
                .getLong(
                    "alarm_time",
                    0L
                )

        val systemAlarmTime =
            systemAlarm?.triggerTime ?: 0L

        val alarmTime =
            when {

                systemAlarmTime >
                    System.currentTimeMillis() ->
                    systemAlarmTime

                savedAlarm >
                    System.currentTimeMillis() ->
                    savedAlarm

                else ->
                    0L
            }

        val nextAlarm =
            if (alarmTime > 0L) {

                formatAlarmTime(
                    alarmTime
                )

            } else {

                "No alarm detected"
            }

        val deviceName =
            "${Build.MANUFACTURER} ${Build.MODEL}"
                .replaceFirstChar {

                    if (it.isLowerCase()) {
                        it.titlecase(
                            Locale.getDefault()
                        )
                    } else {
                        it.toString()
                    }
                }

        return mapOf(

            "battery" to battery,

            "ramPercent" to ramPercent,
            "ramTotal" to ramTotal,
            "ramUsed" to ramUsed,
            "ramFree" to ramFree,

            "storagePercent" to storagePercent,
            "storageTotal" to storageTotal,
            "storageUsed" to storageUsed,
            "storageFree" to storageFree,

            "nextAlarm" to nextAlarm,
            "deviceName" to deviceName
        )
    }

    private fun formatAlarmTime(
        timestamp: Long
    ): String {

        val now =
            Calendar.getInstance()

        val alarm =
            Calendar.getInstance()

        alarm.timeInMillis =
            timestamp

        val sameDay =
            now.get(Calendar.YEAR) ==
                alarm.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) ==
                alarm.get(Calendar.DAY_OF_YEAR)

        val tomorrow =
            Calendar.getInstance()

        tomorrow.add(
            Calendar.DAY_OF_YEAR,
            1
        )

        val isTomorrow =
            tomorrow.get(Calendar.YEAR) ==
                alarm.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) ==
                alarm.get(Calendar.DAY_OF_YEAR)

        val timeFormat =
            SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            )

        val timeText =
            timeFormat.format(
                Date(timestamp)
            )

        return when {

            sameDay ->
                "Today, $timeText"

            isTomorrow ->
                "Tomorrow, $timeText"

            else ->
                SimpleDateFormat(
                    "EEE, dd MMM • hh:mm a",
                    Locale.getDefault()
                ).format(
                    Date(timestamp)
            )
        }
    }


    // =================================================================
    // STORAGE
    // =================================================================

    private fun openStorageSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                )
            )

        } catch (_: Exception) {

            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }

    // =================================================================
    // LARGE FILE SCANNER
    // =================================================================

    private fun hasPermission(
        permission: String
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scanLargeFiles(
        result: MethodChannel.Result
    ) {

        val missingPermissions =
            getMissingMediaPermissions()

        if (missingPermissions.isNotEmpty()) {

            pendingMediaScanResult =
                result

            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                MEDIA_PERMISSION_REQUEST
            )

            return
        }

        performMediaScan(result)
    }

    private fun getMissingMediaPermissions():
        List<String> {

        val permissions =
            mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                !hasPermission(
                    Manifest.permission
                        .READ_MEDIA_IMAGES
                )
            ) {

                permissions.add(
                    Manifest.permission
                        .READ_MEDIA_IMAGES
                )
            }

            if (
                !hasPermission(
                    Manifest.permission
                        .READ_MEDIA_VIDEO
                )
            ) {

                permissions.add(
                    Manifest.permission
                        .READ_MEDIA_VIDEO
                )
            }

            if (
                !hasPermission(
                    Manifest.permission
                        .READ_MEDIA_AUDIO
                )
            ) {

                permissions.add(
                    Manifest.permission
                        .READ_MEDIA_AUDIO
                )
            }

        } else {

            if (
                !hasPermission(
                    Manifest.permission
                        .READ_EXTERNAL_STORAGE
                )
            ) {

                permissions.add(
                    Manifest.permission
                        .READ_EXTERNAL_STORAGE
                )
            }
        }

        return permissions
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            MEDIA_PERMISSION_REQUEST
        ) {

            val pending =
                pendingMediaScanResult

            pendingMediaScanResult =
                null

            if (pending == null) {
                return
            }

            val anyGranted =
                grantResults.any {
                    it ==
                        PackageManager
                            .PERMISSION_GRANTED
                }

            if (anyGranted) {

                performMediaScan(
                    pending
                )

            } else {

                pending.error(
                    "MEDIA_PERMISSION_DENIED",
                    "Media permission was not granted.",
                    null
                )
            }
        }
    }

    private fun performMediaScan(
        result: MethodChannel.Result
    ) {

        Thread {

            try {

                val files =
                    ArrayList<Map<String, Any>>()

                if (canReadImages()) {

                    scanMediaCollection(
                        MediaStore.Images
                            .Media
                            .EXTERNAL_CONTENT_URI,
                        "image",
                        files
                    )
                }

                if (canReadVideos()) {

                    scanMediaCollection(
                        MediaStore.Video
                            .Media
                            .EXTERNAL_CONTENT_URI,
                        "video",
                        files
                    )
                }

                if (canReadAudio()) {

                    scanMediaCollection(
                        MediaStore.Audio
                            .Media
                            .EXTERNAL_CONTENT_URI,
                        "audio",
                        files
                    )
                }

                files.sortByDescending {
                    (
                        it["size"]
                            as Number
                    ).toLong()
                }

                val topFiles =
                    if (files.size > 100) {

                        ArrayList(
                            files.take(100)
                        )

                    } else {

                        files
                    }

                runOnUiThread {
                    result.success(
                        topFiles
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    result.error(
                        "SCAN_ERROR",
                        e.message
                            ?: "Could not scan files.",
                        null
                    )
                }
            }

        }.start()
    }

    private fun canReadImages(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            hasPermission(
                Manifest.permission
                    .READ_MEDIA_IMAGES
            )

        } else {

            hasPermission(
                Manifest.permission
                    .READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun canReadVideos(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            hasPermission(
                Manifest.permission
                    .READ_MEDIA_VIDEO
            )

        } else {

            hasPermission(
                Manifest.permission
                    .READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun canReadAudio(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            hasPermission(
                Manifest.permission
                    .READ_MEDIA_AUDIO
            )

        } else {

            hasPermission(
                Manifest.permission
                    .READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun scanMediaCollection(
        collection: Uri,
        type: String,
        output: ArrayList<Map<String, Any>>
    ) {

        val projection =
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )

        contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.SIZE} > ?",
            arrayOf("10485760"),
            "${MediaStore.MediaColumns.SIZE} DESC"
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns._ID
                )

            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DISPLAY_NAME
                )

            val sizeIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.SIZE
                )

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(
                        idIndex
                    )

                val name =
                    cursor.getString(
                        nameIndex
                    ) ?: "Unknown file"

                val size =
                    cursor.getLong(
                        sizeIndex
                    )

                val uri =
                    Uri.withAppendedPath(
                        collection,
                        id.toString()
                    )

                val mime =
                    when (type) {

                        "image" ->
                            "image/*"

                        "video" ->
                            "video/*"

                        "audio" ->
                            "audio/*"

                        else ->
                            "*/*"
                    }

                output.add(
                    mapOf(
                        "name" to name,
                        "type" to type,
                        "size" to size,
                        "uri" to uri.toString(),
                        "mimeType" to mime
                    )
                )
            }
        }
    }

    private fun openScannedFile(
        uriString: String,
        mimeType: String?
    ) {
        val uri = try {
            Uri.parse(uriString)
        } catch (_: Exception) {
            return
        }

        val type = mimeType ?: "*/*"
        val baseIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val prefs = getSharedPreferences(
            LARGE_FILE_PREFS,
            Context.MODE_PRIVATE
        )
        val savedPackage = prefs.getString(
            DEFAULT_FILE_APP,
            null
        )

        // If the user previously chose "Always", open directly.
        if (!savedPackage.isNullOrBlank()) {
            try {
                val savedActivity = packageManager
                    .queryIntentActivities(
                        baseIntent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )
                    .firstOrNull {
                        it.activityInfo.packageName == savedPackage
                    }
                    ?.activityInfo

                if (savedActivity != null) {
                    val directIntent = Intent(baseIntent).apply {
                        component = ComponentName(
                            savedActivity.packageName,
                            savedActivity.name
                        )
                    }
                    startActivity(directIntent)
                    return
                }

                prefs.edit()
                    .remove(DEFAULT_FILE_APP)
                    .apply()
            } catch (_: Exception) {
                prefs.edit()
                    .remove(DEFAULT_FILE_APP)
                    .apply()
            }
        }

        chooseFileApp(baseIntent, prefs)
    }

    private fun chooseFileApp(
        intent: Intent,
        prefs: android.content.SharedPreferences
    ) {
        val activities = packageManager
            .queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            .distinctBy { it.activityInfo.packageName }
            .sortedBy {
                it.loadLabel(packageManager).toString().lowercase()
            }

        if (activities.isEmpty()) {
            try {
                startActivity(
                    Intent.createChooser(
                        intent,
                        "Open with"
                    )
                )
            } catch (_: Exception) {
            }
            return
        }

        val labels = activities.map {
            it.loadLabel(packageManager).toString()
        }.toTypedArray()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
        }

        val message = TextView(this).apply {
            text = "Choose an app to open this file."
            setTextColor(
                android.graphics.Color.LTGRAY
            )
            textSize = 14f
            setPadding(0, 0, 0, 12)
        }

        val alwaysCheck = CheckBox(this).apply {
            text = "Always use this app"
            isChecked = false
        }

        layout.addView(message)
        layout.addView(alwaysCheck)

        android.app.AlertDialog.Builder(this)
            .setTitle("Open large file")
            .setView(layout)
            .setItems(labels) { _, which ->
                val activityInfo = activities[which].activityInfo
                val selectedPackage = activityInfo.packageName
                val selectedClass = activityInfo.name

                if (alwaysCheck.isChecked) {
                    prefs.edit()
                        .putString(
                            DEFAULT_FILE_APP,
                            selectedPackage
                        )
                        .apply()
                }

                val selectedIntent = Intent(intent).apply {
                    component = ComponentName(
                        selectedPackage,
                        selectedClass
                    )
                }

                try {
                    startActivity(selectedIntent)
                } catch (_: Exception) {
                    prefs.edit()
                        .remove(DEFAULT_FILE_APP)
                        .apply()

                    try {
                        startActivity(
                            Intent.createChooser(
                                intent,
                                "Open with"
                            )
                        )
                    } catch (_: Exception) {
                    }
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // =================================================================
    // ALARM
    // =================================================================

    private fun setAlarm(
        hour: Int,
        minute: Int,
        result: MethodChannel.Result
    ) {

        val alarmManager =
            getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {

            result.error(
                "EXACT_ALARM_PERMISSION_REQUIRED",
                "Exact alarm permission is required.",
                null
            )

            return
        }

        try {

            val calendar =
                Calendar.getInstance()

            calendar.set(
                Calendar.HOUR_OF_DAY,
                hour
            )

            calendar.set(
                Calendar.MINUTE,
                minute
            )

            calendar.set(
                Calendar.SECOND,
                0
            )

            calendar.set(
                Calendar.MILLISECOND,
                0
            )

            if (
                calendar.timeInMillis <=
                System.currentTimeMillis()
            ) {

                calendar.add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }

            val triggerTime =
                calendar.timeInMillis

            val pendingIntent =
                PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    Intent(
                        this,
                        AlarmReceiver::class.java
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )
                    ?: Intent(
                        this,
                        MainActivity::class.java
                    )

            val showIntent =
                PendingIntent.getActivity(
                    this,
                    ALARM_SHOW_REQUEST_CODE,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    triggerTime,
                    showIntent
                ),
                pendingIntent
            )

            getSharedPreferences(
                "nexsus_alarm",
                Context.MODE_PRIVATE
            )
                .edit()
                .putLong(
                    "alarm_time",
                    triggerTime
                )
                .apply()

            createNotificationChannel()
            requestNotificationPermission()

            result.success(
                mapOf(
                    "success" to true,
                    "timestamp" to triggerTime,
                    "formatted" to
                        formatAlarmTime(
                            triggerTime
                        )
                )
            )

        } catch (e: SecurityException) {

            result.error(
                "EXACT_ALARM_PERMISSION_REQUIRED",
                "Android denied exact alarm access.",
                null
            )

        } catch (e: Exception) {

            result.error(
                "ALARM_ERROR",
                e.message
                    ?: "Could not set alarm.",
                null
            )
        }
    }

    private fun cancelAlarm(
        result: MethodChannel.Result
    ) {

        try {

            val alarmManager =
                getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

            val pendingIntent =
                PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    Intent(
                        this,
                        AlarmReceiver::class.java
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            alarmManager.cancel(
                pendingIntent
            )

            pendingIntent.cancel()

            getSharedPreferences(
                "nexsus_alarm",
                Context.MODE_PRIVATE
            )
                .edit()
                .remove("alarm_time")
                .apply()

            result.success(true)

        } catch (e: Exception) {

            result.error(
                "CANCEL_ALARM_ERROR",
                e.message
                    ?: "Could not cancel alarm.",
                null
            )
        }
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    "Nexsus AI Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.description =
                "Notifications for Nexsus AI alarms"

            channel.enableVibration(true)

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                !hasPermission(
                    Manifest.permission
                        .POST_NOTIFICATIONS
                )
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission
                            .POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun openExactAlarmSettings() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            try {

                startActivity(
                    Intent(
                        Settings
                            .ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    ).apply {

                        data =
                            Uri.parse(
                                "package:$packageName"
                            )
                    }
                )

            } catch (_: Exception) {

                startActivity(
                    Intent(
                        Settings.ACTION_SETTINGS
                    )
                )
            }
        }
    }

    // =================================================================
    // NOTIFICATION ACCESS
    // =================================================================

    private fun isNotificationAccessEnabled():
        Boolean {

        return try {

            val enabledListeners =
                Settings.Secure.getString(
                    contentResolver,
                    "enabled_notification_listeners"
                )

            if (
                enabledListeners.isNullOrBlank()
            ) {

                false

            } else {

                enabledListeners
                    .split(":")
                    .any { component ->

                        ComponentName
                            .unflattenFromString(
                                component
                            )
                            ?.packageName ==
                            packageName
                    }
            }

        } catch (_: Exception) {

            false
        }
    }

    private fun openNotificationAccessSettings() {

        try {

            startActivity(
                Intent(
                    Settings
                        .ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )

        } catch (_: Exception) {

            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }

    // =================================================================
    // AOD
    // =================================================================

    private fun openAodSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )

        } catch (_: Exception) {
        }
    }

    private fun openAodPermission() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_SETTINGS)
            )
        }
    }

    private fun openDisplaySettings() {

        try {
            startActivity(
                Intent(Settings.ACTION_DISPLAY_SETTINGS)
            )
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_SETTINGS)
            )
        }
    }

    private fun startNexsusAod(
    call: MethodCall,
    result: MethodChannel.Result
) {

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.M &&
        !Settings.canDrawOverlays(this)
    ) {

        result.success(
            mapOf(
                "started" to false,
                "permissionRequired" to true
            )
        )

        return
    }

    val clockStyle =
        call.argument<Int>(
            "clockStyle"
        ) ?: 0

    val clockStyleName =
        call.argument<String>(
            "clockStyleName"
        ) ?: "Digital"

    val use24Hour =
        call.argument<Boolean>(
            "use24Hour"
        ) ?: false

    val showDate =
        call.argument<Boolean>(
            "showDate"
        ) ?: true

    val showBattery =
        call.argument<Boolean>(
            "showBattery"
        ) ?: true

    val showNexsus =
        call.argument<Boolean>(
            "showNexsus"
        ) ?: true

    val brightness =
        call.argument<Double>(
            "brightness"
        ) ?: 0.7

    // =============================================================
    // SAVE AOD SETTINGS
    // =============================================================

    getSharedPreferences(
        "nexsus_aod",
        Context.MODE_PRIVATE
    )
        .edit()

        .putBoolean(
            "aod_enabled",
            true
        )

        .putInt(
            "clock_style",
            clockStyle
        )

        .putString(
            "clock_style_name",
            clockStyleName
        )

        .putBoolean(
            "use_24_hour",
            use24Hour
        )

        .putBoolean(
            "show_date",
            showDate
        )

        .putBoolean(
            "show_battery",
            showBattery
        )

        .putBoolean(
            "show_nexsus",
            showNexsus
        )

        .putFloat(
            "brightness",
            brightness.toFloat()
        )

        .apply()

    // =============================================================
    // START AUTOMATIC AOD SERVICE
    // =============================================================

    try {

        NexsusAodService.start(this)

        result.success(
            mapOf(
                "started" to true,
                "permissionRequired" to false,
                "automatic" to true,
                "notificationAccess" to
                    isNotificationAccessEnabled()
            )
        )

    } catch (e: Exception) {

        result.error(
            "AOD_START_ERROR",
            e.message
                ?: "Could not start Nexsus AOD service.",
            null
        )
    }
}
}

// =====================================================================
// ALARM RECEIVER
// =====================================================================

class AlarmReceiver : BroadcastReceiver() {

    companion object {

        private const val CHANNEL_ID =
            "nexsus_alarm"

        private const val NOTIFICATION_ID =
            7777
    }

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        createNotificationChannel(
            context
        )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )

                .setSmallIcon(
                    android.R.drawable
                        .ic_lock_idle_alarm
                )

                .setContentTitle(
                    "Nexsus AI Alarm"
                )

                .setContentText(
                    "Your alarm is ringing."
                )

                .setPriority(
                    NotificationCompat
                        .PRIORITY_HIGH
                )

                .setCategory(
                    NotificationCompat
                        .CATEGORY_ALARM
                )

                .setAutoCancel(true)

                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU ||

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission
                    .POST_NOTIFICATIONS
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {

            manager.notify(
                NOTIFICATION_ID,
                notification
            )
        }

        context.getSharedPreferences(
            "nexsus_alarm",
            Context.MODE_PRIVATE
        )
            .edit()
            .remove("alarm_time")
            .apply()
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Nexsus AI Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.enableVibration(true)

            manager.createNotificationChannel(
                channel
            )
        }
    }
}