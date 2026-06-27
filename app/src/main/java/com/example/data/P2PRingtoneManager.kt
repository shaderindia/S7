package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object P2PRingtoneManager {
    private const val TAG = "P2PRingtoneManager"
    private var ringtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var toneJob: Job? = null

    fun playIncomingRingtone(context: Context) {
        stop()
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
            Log.d(TAG, "Playing incoming ringtone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone", e)
        }
    }

    fun playOutgoingRingback() {
        stop()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100)
            toneJob = CoroutineScope(Dispatchers.IO).launch {
                while (toneJob?.isActive == true) {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
                    delay(2000)
                    toneGenerator?.stopTone()
                    delay(2000)
                }
            }
            Log.d(TAG, "Playing outgoing ringback tone via ToneGenerator")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play outgoing ringback tone", e)
        }
    }

    fun stop() {
        toneJob?.cancel()
        toneJob = null
        try {
            ringtone?.stop()
        } catch (e: Exception) {}
        ringtone = null

        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) {}
        toneGenerator = null
        Log.d(TAG, "Stopped all ringtones and ringbacks")
    }
}
