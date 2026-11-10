package com.reelguard.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.view.KeyEvent

/**
 * Pauses the Reel/Short that keeps playing behind the block screen.
 *
 * Taking permanent audio focus makes the player that currently holds it (YouTube, Instagram) receive AUDIOFOCUS_LOSS, which
 * well-behaved players answer by pausing. It needs no permission and only reaches whatever is playing in front, not an unrelated
 * player in the background. If the system refuses the request (Android 15+ limits who may take focus), a media "pause" key is sent
 * instead, but only while something is actually playing.
 */
class PlaybackPauser(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var request: AudioFocusRequest? = null

    /** Pauses playback and keeps audio focus until [release], so the video stays paused. Safe to call repeatedly. */
    fun pause() {
        if (request != null) return
        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).build()
        if (audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            request = focus
        } else if (audio.isMusicActive) {
            sendPauseKey()
        }
    }

    /** Gives audio focus back. The paused video stays paused until the user taps it. */
    fun release() {
        request?.let { audio.abandonAudioFocusRequest(it) }
        request = null
    }

    private fun sendPauseKey() {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }
}
