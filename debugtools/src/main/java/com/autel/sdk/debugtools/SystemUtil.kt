package com.autel.sdk.debugtools

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.POWER_SERVICE
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import com.autel.drone.sdk.log.SDKLog

/**
 *
 * system control tool class
 */
object SystemUtil {
    const val TAG = "SystemUtil"

    /**
     * system sleep, screen off
     */
    fun goToSleep(ctx: Context) {
        SDKLog.i(TAG, "goToSleep -> ")
        val powerManager: PowerManager = ctx.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        try {
            powerManager.javaClass.getMethod("goToSleep", Long::class.javaPrimitiveType)
                .invoke(powerManager, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            e.cause?.printStackTrace()
        }
    }

    /**
     * system wake up, screen on
     */
    fun goToAlive(ctx: Context) {
        SDKLog.i(TAG, "goToAlive -> ")
        val powerManager: PowerManager = ctx.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        try {
            powerManager.javaClass.getMethod("wakeUp", Long::class.javaPrimitiveType)
                .invoke(powerManager, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            e.cause?.printStackTrace()
        }
    }

    /**
     * volume setting
     */
    fun setStreamVolume(ctx: Context, index: Int) {
        SDKLog.i(TAG, "setStreamVolume -> index=$index")
        var volume = index
        if (volume < 0) volume = 0
        if (volume > 31) volume = 31
        val audioManager = ctx.applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0)
    }
}