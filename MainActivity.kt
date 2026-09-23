package com.emran.scohelper

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ScoHelper"
        private const val REQUEST_CODE = 1001
    }

    private var userService: IUserService? = null
    private var isBound = false

    // اتصال به UserService
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(packageName, ScoUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("sco_service")
        .debuggable(false)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "UserService connected")
            userService = IUserService.Stub.asInterface(service)
            isBound = true
            updateStatus("✅ متصل به Shizuku UserService")

            // بررسی خودکار اتصال
            if (userService?.isScoOn() == true) {
                updateStatus("🎧 SCO فعال است")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "UserService disconnected")
            userService = null
            isBound = false
            updateStatus("❌ اتصال قطع شد")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI ساده
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val statusText = TextView(this).apply {
            text = "در حال راه‌اندازی..."
            textSize = 16f
        }

        val startButton = Button(this).apply {
            text = "🎧 فعال‌سازی SCO"
            setOnClickListener {
                if (isBound) {
                    val result = userService?.startSco()
                    Toast.makeText(this@MainActivity,
                        if (result == true) "SCO فعال شد!" else "خطا در فعال‌سازی",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity,
                        "Shizuku متصل نیست!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val stopButton = Button(this).apply {
            text = "⏹️ غیرفعال‌سازی SCO"
            setOnClickListener {
                if (isBound) {
                    userService?.stopSco()
                    Toast.makeText(this@MainActivity, "SCO غیرفعال شد", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(statusText)
        layout.addView(startButton)
        layout.addView(stopButton)
        setContentView(layout)

        // بررسی و درخواست دسترسی Shizuku
        checkShizukuPermission()
    }

    private fun checkShizukuPermission() {
        if (Shizuku.isPreV11()) {
            updateStatus("❌ Shizuku نسخه قدیمی است")
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateStatus("✅ دسترسی Shizuku داده شده")
            bindUserService()
        } else {
            updateStatus("⏳ در انتظار دسترسی Shizuku...")
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    private fun bindUserService() {
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (e: Exception) {
            Log.e(TAG, "bindUserService failed", e)
            updateStatus("❌ خطا در اتصال: ${e.message}")
        }
    }

    private fun updateStatus(msg: String) {
        runOnUiThread {
            (findViewById<TextView>(android.R.id.text1))?.text = msg
            Log.d(TAG, msg)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateStatus("✅ دسترسی Shizuku داده شد")
                bindUserService()
            } else {
                updateStatus("❌ دسترسی Shizuku رد شد")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // دریافت دستور از Termux
        if (intent?.action == "com.emran.scohelper.START_SCO") {
            Log.d(TAG, "Received START_SCO from Termux")
            if (isBound) {
                userService?.startSco()
                Toast.makeText(this, "🎧 SCO از Termux فعال شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Shizuku متصل نیست!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
