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