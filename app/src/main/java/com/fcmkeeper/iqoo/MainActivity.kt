package com.fcmkeeper.iqoo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        status = TextView(this).apply { textSize = 15f }
        root.addView(status)

        fun button(text: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                this.text = text
                setOnClickListener {
                    action()
                    refresh()
                }
            })
        }

        button("1. Cấp quyền thông báo") { requestNotificationPermission() }
        button("2. Bỏ tối ưu pin cho app này") { requestIgnoreBattery() }
        button("3. Mở cài đặt chạy nền / tự khởi động (vivo)") { openRomSettings() }
        button("4. Mở cài đặt Google Play Services") { openAppInfo(GmsHelper.GMS) }
        button("5. Bật / tắt dịch vụ giữ kết nối") { toggleService() }
        button("6. Kiểm tra kết nối & gửi ngay") {
            val app = applicationContext
            Thread { Tick.run(app) }.start()
            Toast.makeText(this, "Đang kiểm tra… bấm nút 7 sau vài giây", Toast.LENGTH_SHORT).show()
        }
        button("7. Làm mới trạng thái") { }
        button("8. Bật / tắt giữ liên kết với GMS (thử nghiệm)") {
            val on = !Prefs.bindEnabled(this)
            Prefs.setBindEnabled(this, on)
            if (on) {
                if (Prefs.isEnabled(this)) GmsBinder.start(this)
            } else {
                GmsBinder.stop(this)
            }
        }

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val gms = GmsHelper.isInstalled(this)
        val gmsSystem = GmsHelper.isSystemApp(this)
        val notif = NotificationManagerCompat.from(this).areNotificationsEnabled()
        val pm = getSystemService(PowerManager::class.java)
        val battery = pm.isIgnoringBatteryOptimizations(packageName)
        val enabled = Prefs.isEnabled(this)
        val last = Prefs.lastBeat(this)
        val lastText = if (last == 0L) "chưa có"
        else DateFormat.format("HH:mm:ss dd/MM", last).toString()

        val next = Prefs.nextBeat(this)
        val nextText = if (!enabled || next == 0L) "—"
        else DateFormat.format("HH:mm:ss dd/MM", next).toString()
        val every = Prefs.intervalMin(this)
        val net = Adaptive.hasNetwork(this)

        val conn = Prefs.connState(this)
        val code = Prefs.statusCode(this)
        val err = Prefs.statusError(this)
        val lastRe = Prefs.lastReconnect(this)
        val reText = if (lastRe == 0L) "chưa có"
        else DateFormat.format("HH:mm:ss dd/MM", lastRe).toString()

        fun mark(ok: Boolean) = if (ok) "✅" else "❌"

        status.text = buildString {
            appendLine("${mark(gms)} Google Play Services: " + if (gms) "đã cài" else "chưa cài")
            if (gms) appendLine("    Loại: " + if (gmsSystem) "app hệ thống" else "app cài thêm (người dùng)")
            appendLine("${mark(notif)} Thông báo của app này")
            appendLine("${mark(battery)} Bỏ tối ưu pin (app này)")
            appendLine("${mark(enabled)} Dịch vụ giữ kết nối")
            val bs = GmsBinder.state
            appendLine(
                (if (bs == "bound") "✅" else if (bs == "failed") "❌" else "❔") +
                    " Liên kết với GMS (chống đóng băng): " +
                    when (bs) {
                        "bound" -> "đã liên kết"
                        "connecting" -> "đang kết nối"
                        "failed" -> "không liên kết được"
                        else -> "tắt"
                    }
            )
            appendLine("${mark(net)} Mạng: " + if (net) "có" else "không")
            appendLine(
                when (conn) { 1 -> "✅"; 0 -> "❌"; else -> "❔" } +
                    " Trạng thái FCM trong GMS: " +
                    when (conn) {
                        1 -> "đang kết nối (mã ${code ?: "?"})"
                        0 -> "mất kết nối" + (code?.let { " (mã $it)" } ?: (err?.let { " ($it)" } ?: ""))
                        else -> "chưa đọc được" + (err?.let { " ($it)" } ?: "")
                    }
            )
            appendLine("Reconnect gần nhất: $reText")
            appendLine("Heartbeat gần nhất: $lastText")
            appendLine("Heartbeat tiếp theo: $nextText" + if (every > 0) " (mỗi ~$every phút)" else "")
            appendLine()
            appendLine("Lưu ý: cũng cần bật chạy nền + tự khởi động cho Google Play Services và app đang mất thông báo.")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            Toast.makeText(this, "Android này không cần cấp riêng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestIgnoreBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Đã bỏ tối ưu pin rồi", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openRomSettings() {
        if (!RomSettings.openBackgroundSettings(this)) {
            RomSettings.openAppDetails(this, packageName)
            Toast.makeText(
                this,
                "Không mở thẳng được. Vào Pin → chạy nền tiêu thụ cao và Quyền → Tự khởi động.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openAppInfo(pkg: String) {
        if (!GmsHelper.isInstalled(this, pkg)) {
            Toast.makeText(this, "Chưa cài $pkg", Toast.LENGTH_SHORT).show()
            return
        }
        RomSettings.openAppDetails(this, pkg)
    }

    private fun toggleService() {
        if (Prefs.isEnabled(this)) {
            Prefs.setEnabled(this, false)
            HeartbeatReceiver.cancel(this)
            stopService(Intent(this, KeepAliveService::class.java))
        } else {
            Prefs.setEnabled(this, true)
            try {
                ContextCompat.startForegroundService(this, Intent(this, KeepAliveService::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Không khởi động được: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
