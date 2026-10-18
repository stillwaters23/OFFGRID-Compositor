package com.offgrid.compositor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
import android.view.RoundedCorner
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.offgrid.compositor.audio.CaptureAction
import com.offgrid.compositor.audio.MicKeys
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.terminal.ScreenEdges
import java.io.File

private val LIVE_MESSAGES = setOf(
    R.string.shape_ready,
    R.string.camera_ready,
    R.string.ghost_on,
    R.string.effects_failed,
    R.string.camera_off,
    R.string.mic_denied,
    R.string.mic_failed,
)

internal fun MainActivity.showLivePicture() {
        if (recording || playRequested) {
            return
        }
        val settings = effects.engine.settings
        val hidingFace = settings.on(FxKeys.DIAMOND_ON) ||
            !settings.chose(FxKeys.CAMERA_MODE, FxKeys.MODE_LIVE) ||
            MaskGate.hidesFace(settings.choice(FxKeys.BODY_MODE))
        val message = when {
            effectsBroken && hidingFace && !cameraDark -> R.string.effects_failed
            micProblem != 0 -> micProblem
            cameraDark -> R.string.camera_off
            settings.chose(FxKeys.CAMERA_MODE, FxKeys.MODE_GHOST) -> R.string.ghost_on
            terminalPanel.testDiamondOn -> R.string.shape_ready
            else -> R.string.camera_ready
        }
        showMessage(message, MainActivity.ButtonMode.LIVE)
    }

    /** The Test diamond toggle in Panel settings. Off paints nothing extra into the video. */
internal fun MainActivity.onTestDiamondChanged(on: Boolean) {
        processor?.testShapeOn = on
        describeCamera(on)
        if (shownMessage in LIVE_MESSAGES) {
            showLivePicture()
        }
    }

    /** Effect settings changed: the GHOST ON line and the Record button's length follow at once. */
internal fun MainActivity.onEffectSettings() {
        if (!effectsReady()) return
        processor?.reducedMotion = reducedMotion()
        applyCapture()
        if (shownMessage in LIVE_MESSAGES) {
            showLivePicture()
        } else if (!recording) {
            setRecordLabel()
        }
    }

internal fun MainActivity.hasMicPermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

/** The phone denied the mic. The toggle goes off and the record bar says how to allow it. */
internal fun MainActivity.onMicPermission(granted: Boolean) {
    micAsking = false
    if (granted) {
        capture.clearMicDenial()
        micProblem = 0
        applyCapture()
        return
    }
    capture.denyMic()
    micProblem = R.string.mic_denied
    if (effects.engine.settings.on(MicKeys.MIC_ON)) {
        terminalPanel.setToggleSetting(MicKeys.MIC_ON, false)
    }
    applyCapture()
}

    /**
     * Makes the camera and the mic match the two toggles in the panel, and the mic's Microphone Effects
     * match their typed values. The camera and the mic are both let go while the app is hidden.
     */
internal fun MainActivity.applyCapture() {
        if (!afterFirstFrame || !effectsReady() || !micReady()) return
        val settings = effects.engine.settings
        val cameraWanted = settings.on(MicKeys.CAM_ON, true)
        val blocked = settings.on(FxKeys.MIC_BLOCKED)
        if (blocked && settings.on(MicKeys.MIC_ON)) {
            terminalPanel.setMicSettings(false, true, notify = false)
        }
        val micToggle = settings.on(MicKeys.MIC_ON) && !blocked
        if (micToggle && capture.micDenied) {
            capture.clearMicDenial()
            micProblem = 0
        } else if (!micToggle && !capture.micDenied) {
            micProblem = 0
        }
        mic.apply(settings)
        // The system permission dialog is chrome on air. Phone 2 can turn fx.mic.on on mid share.
        if (resumed && !lockStore.locked && micToggle && micProblem == 0 && !capture.micDenied && !hasMicPermission() && !micAsking) {
            askForMic()
        }
        val micWanted = micToggle && micProblem == 0 && !capture.micDenied
        val actions = capture.update(cameraWanted, micWanted, hasMicPermission(), resumed)
        for (action in actions) {
            when (action) {
                CaptureAction.UNBIND_CAMERA -> unbindCamera()
                CaptureAction.BIND_CAMERA -> if (hasCameraPermission()) startCamera()
                CaptureAction.START_MIC -> if (blocked || !mic.start()) {
                    if (!blocked) {
                        capture.micFailed()
                        micProblem = R.string.mic_failed
                    }
                }
                CaptureAction.RELEASE_MIC -> mic.stop()
            }
        }
        showDark(resumed && !cameraWanted)
        processor?.setReplayAudio(if (capture.micRunning && resumed) mic.engine else null)
        if (resumed && shownMessage in LIVE_MESSAGES) showLivePicture()
    }

    /** Says why first, then asks the phone for the mic. */
internal fun MainActivity.askForMic() {
        micAsking = true
        showMessage(R.string.mic_needs_ok, MainActivity.ButtonMode.LIVE)
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /** Black picture with the effects drawn over it, instead of the camera. */
internal fun MainActivity.showDark(on: Boolean) {
        if (!darkStageReady()) return
        if (on == cameraDark) return
        cameraDark = on
        if (on) {
            ensureProcessor().setCameraOff(true)
            darkStage.show()
            showLivePicture()
        } else {
            darkStage.hide()
            processor?.setCameraOff(false)
        }
    }

    /** True when the phone's Remove animations setting is on. Effects stay, but calmer. */
internal fun MainActivity.reducedMotion(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

internal fun MainActivity.setRecordLabel() {
        val seconds = if (effectsReady()) recordSeconds() else 0
        if (seconds > 0) {
            recordButton.text = getString(R.string.record_seconds, seconds)
        } else {
            recordButton.setText(R.string.record_open)
        }
    }

internal fun MainActivity.describeCamera(diamondOn: Boolean) {
        previewView.contentDescription = getString(
            if (diamondOn) R.string.camera_picture else R.string.camera_picture_plain,
        )
    }

internal fun MainActivity.showPermissionHelp() {
        val canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        if (canAskAgain) {
            showMessage(R.string.camera_denied, MainActivity.ButtonMode.TRY_AGAIN)
        } else {
            showMessage(R.string.camera_blocked, MainActivity.ButtonMode.SETTINGS)
        }
    }

/**
 * Phase 1.2 (V2 section 7.2): the stage carries no words. Every message is typed on the panel's
 * status line and sent to Phone 2 as `fx.status.line`. The one button Royalty may need to tap,
 * Try again or Open Settings, sits under the status line. The record bar stays GONE for good.
 */
internal fun MainActivity.showMessage(textRes: Int, buttonMode: MainActivity.ButtonMode) {
        if (playRequested && buttonMode != MainActivity.ButtonMode.LIVE && buttonMode != MainActivity.ButtonMode.RECORDING) {
            hidePlayback()
        }
        val current = terminalPanel.statusLine.contentDescription?.toString().orEmpty()
        val cameraBind =
            textRes == R.string.camera_starting ||
                textRes == R.string.camera_missing ||
                textRes == R.string.camera_failed
        if (cameraBind && current.startsWith("Locked:")) {
            shownMessage = textRes
            return
        }
        shownMessage = textRes
        val words = if (buttonMode == MainActivity.ButtonMode.RECORDING) {
            recordSeconds().takeIf { it > 0 }?.let { getString(R.string.recording_timed, it) } ?: getString(textRes)
        } else {
            getString(textRes)
        }
        sayStatus(words)
        when (buttonMode) {
            MainActivity.ButtonMode.NONE, MainActivity.ButtonMode.LIVE, MainActivity.ButtonMode.RECORDING -> {
                terminalPanel.showNotice(null)
            }
            MainActivity.ButtonMode.TRY_AGAIN -> terminalPanel.showNotice(getString(R.string.try_again)) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            MainActivity.ButtonMode.SETTINGS -> terminalPanel.showNotice(getString(R.string.open_settings)) {
                openAppSettings()
            }
        }
    }

internal fun MainActivity.refreshPlayButton() {
        val file = recordingFile()
        playButton.isEnabled = !recording && file.exists() && file.length() > 0L
    }

internal fun MainActivity.recordingFile(): File = File(filesDir, RECORDING_SAVED_NAME)

internal fun MainActivity.openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    /**
     * The bars, camera hole, gesture areas, and round corners, for the panel (final build, M2).
     * Bars are measured as if they show, so pulling the status bar down never moves the panel.
     */
internal fun MainActivity.screenEdges(insets: WindowInsetsCompat): ScreenEdges {
        val bars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val gestures = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemGestures())
        val mandatory = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.mandatorySystemGestures())
        val platform = insets.toWindowInsets()
        val corner = if (platform != null) {
            listOf(
                RoundedCorner.POSITION_TOP_LEFT,
                RoundedCorner.POSITION_TOP_RIGHT,
                RoundedCorner.POSITION_BOTTOM_LEFT,
                RoundedCorner.POSITION_BOTTOM_RIGHT,
            ).maxOf { platform.getRoundedCorner(it)?.radius ?: 0 }
        } else {
            0
        }
        return ScreenEdges(
            top = maxOf(bars.top, cutout.top),
            bottom = maxOf(bars.bottom, cutout.bottom),
            left = maxOf(bars.left, cutout.left),
            right = maxOf(bars.right, cutout.right),
            gestureLeft = gestures.left,
            gestureRight = gestures.right,
            gestureBottom = maxOf(gestures.bottom, mandatory.bottom),
            cornerRadius = corner,
        )
    }

/** Immersive only while Broadcast Lock hid the panel. Editing must keep the bars. */
internal fun shouldHideSystemBars(broadcastLocked: Boolean): Boolean = broadcastLocked

internal fun MainActivity.showSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

internal fun MainActivity.hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private const val RECORDING_SAVED_NAME = "last-recording.mp4"
