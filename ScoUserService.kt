package com.emran.scohelper

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import rikka.shizuku.SystemServiceHelper

/**
 * این سرویس داخل پروسه Shizuku (uid=2000) اجرا میشه
 * و می‌تونه startBluetoothSco رو بدون محدودیت صدا بزنه
 */
class ScoUserService : IUserService.Stub() {

    companion object {
        private const val TAG = "ScoUserService"
    }

    override fun destroy() {
        Log.d(TAG, "ScoUserService destroyed")
        System.exit(0)
    }

    override fun startSco(): Boolean {
        return try {
            Log.d(TAG, "startSco called - uid: ${android.os.Process.myUid()}")

            // گرفتن AudioManager از طریق Context سیستمی
            val context = getSystemContext()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // مهم: startBluetoothSco باید روی main thread صدا زده بشه
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                    Log.d(TAG, "startBluetoothSco() called successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error calling startBluetoothSco", e)
                }
            }, 500) // تأخیر 500ms - حیاتی برای Android 12

            true
        } catch (e: Exception) {
            Log.e(TAG, "startSco failed", e)
            false
        }
    }

    override fun stopSco(): Boolean {
        return try {
            val context = getSystemContext()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            Handler(Looper.getMainLooper()).post {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                Log.d(TAG, "stopBluetoothSco() called")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "stopSco failed", e)
            false
        }
    }

    override fun isScoOn(): Boolean {
        return try {
            val context = getSystemContext()
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isBluetoothScoOn
        } catch (e: Exception) {
            false
        }
    }

    /**
     * گرفتن Context سیستمی با دسترسی shell
     */
    private fun getSystemContext(): Context {
        // از ActivityThread برای گرفتن Context سیستمی استفاده می‌کنیم
        val activityThread = Class.forName("android.app.ActivityThread")
        val systemMain = activityThread.getMethod("systemMain").invoke(null)
        val getSystemContext = activityThread.getMethod("getSystemContext")
        return getSystemContext.invoke(systemMain) as Context
    }
}
