package com.reelguard.ui.guide

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.reelguard.MainActivity
import com.reelguard.R
import com.reelguard.data.datastore.ThemeMode
import com.reelguard.ui.theme.ReelGuardTheme
import com.reelguard.work.isServiceEnabled
import java.util.Locale

/**
 * Walks the user through switching the accessibility service on. It explains the three taps, then opens Android Settings and shrinks
 * itself into a small picture-in-picture window that keeps looping the steps (and reading them aloud) on top of the real Settings
 * screen. When the service turns on it brings ReelGuard back to the front by itself.
 */
class AccessibilityGuideActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private val inPip = mutableStateOf(false)
    private val voiceOn = mutableStateOf(true)
    private var observer: ContentObserver? = null
    private var handedBack = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (isServiceEnabled(this)) {
            handBack(delayMs = 0L)
            return
        }
        voiceOn.value = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_VOICE, true)
        startSpeech()
        watchService()
        val appsList = GuideSteps.appsListLabel(Build.MANUFACTURER)
        val restrictedHint = GuideSteps.needsRestrictedHint(Build.VERSION.SDK_INT, installerPackage())
        setContent {
            ReelGuardTheme(ThemeMode.SYSTEM) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GuideScreen(
                        state = GuideState(appsList, restrictedHint, inPip.value, voiceOn.value),
                        onVoiceChange = ::setVoice,
                        onOpenSettings = { beginGuiding(appsList) },
                        onClose = { finishAndRemoveTask() },
                        onSpeak = ::speak,
                    )
                }
            }
        }
    }

    /** Shrinks into the floating window first, then opens Settings so the window ends up on top of it. */
    private fun beginGuiding(appsList: AppsListLabel) {
        val settings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (enterPip()) {
            handler.postDelayed({ startActivity(settings) }, SETTINGS_DELAY_MS)
        } else {
            // No picture-in-picture on this phone: show the steps once as a message instead.
            val first =
                getString(if (appsList == AppsListLabel.INSTALLED_APPS) R.string.guide_label_installed else R.string.guide_label_downloaded)
            val message = getString(R.string.guide_no_window, first, getString(R.string.app_name), getString(R.string.guide_mock_switch))
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            startActivity(settings)
        }
    }

    private fun enterPip(): Boolean {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        val params = PictureInPictureParams.Builder().setAspectRatio(Rational(PIP_WIDTH, PIP_HEIGHT)).build()
        return runCatching { enterPictureInPictureMode(params) }.getOrDefault(false)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
        // The user swiped the small window away: the guide is over.
        if (!isInPictureInPictureMode && lifecycle.currentState == Lifecycle.State.CREATED) finishAndRemoveTask()
    }

    /** The moment the switch is flipped Android updates this setting, so there is no need to poll. */
    private fun watchService() {
        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val o =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    if (isServiceEnabled(applicationContext)) onServiceOn()
                }
            }
        observer = o
        contentResolver.registerContentObserver(uri, false, o)
    }

    private fun onServiceOn() {
        if (handedBack) return
        speak(getString(R.string.guide_done), force = true)
        handBack()
    }

    /** Brings ReelGuard back to the front and closes the guide. */
    private fun handBack(delayMs: Long = HAND_BACK_DELAY_MS) {
        handedBack = true
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        if (delayMs == 0L) finishAndRemoveTask() else handler.postDelayed({ finishAndRemoveTask() }, delayMs)
    }

    private fun setVoice(on: Boolean) {
        voiceOn.value = on
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_VOICE, on).apply()
        if (!on) tts?.stop()
    }

    private fun startSpeech() {
        tts =
            TextToSpeech(applicationContext) { status ->
                if (status != TextToSpeech.SUCCESS) return@TextToSpeech
                val engine = tts ?: return@TextToSpeech
                // The guide text is in English, so use an English voice whatever the phone's language is.
                engine.setLanguage(Locale.ENGLISH)
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                ttsReady = true
                pendingSpeech?.let { speak(it) }
            }
    }

    private fun speak(
        text: String,
        force: Boolean = false,
    ) {
        if (!voiceOn.value && !force) return
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guide")
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val PREFS = "guide"
        private const val KEY_VOICE = "voice"
        private const val PIP_WIDTH = 3
        private const val PIP_HEIGHT = 4
        private const val SETTINGS_DELAY_MS = 350L
        private const val HAND_BACK_DELAY_MS = 1_800L

        /** Opens the guide in its own task so it can float over Settings without dragging ReelGuard along. */
        fun launch(context: Context) {
            context.startActivity(Intent(context, AccessibilityGuideActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
