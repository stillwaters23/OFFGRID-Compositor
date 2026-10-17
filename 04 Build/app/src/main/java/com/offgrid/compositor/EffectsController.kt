package com.offgrid.compositor

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.MotionEvent
import android.view.View
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.InkStroke
import com.offgrid.compositor.effects.StageMath
import com.offgrid.compositor.terminal.PanelAction
import com.offgrid.compositor.terminal.TerminalPanel
import java.io.File

/**
 * Joins the terminal panel to the effects (final build, M4). Settings flow from the panel to
 * the effect engine, buttons fire effects, and the answers are typed back on the panel in
 * plain words. Drawing with a finger and the soundboard live here too.
 *
 * It runs on the main thread. Saving files runs on a small helper thread.
 */
internal class EffectsController(
    private val context: Context,
    private val panel: TerminalPanel,
    val engine: EffectEngine,
) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = GallerySaver.background
    private val sounds = SoundBoard()

    /** The soundboard's sounds, ready to be mixed into a recording's audio. */
    val videoSounds get() = sounds.videoMix
    private val handshake by lazy { Handshake(context) }

    /** The camera's painter, once the camera starts. */
    var processor: CompositorProcessor? = null
        set(value) {
            field = value
            pushReplay()
            pushDeck()
        }

    /** Clips picked on this phone. The painter uses this same list. */
    internal val deckCatalog = ArDeckCatalog()

    /** The last recorded video, for Save last video to gallery. */
    var lastVideo: () -> File? = { null }

    /** Called after settings change, so the camera screen can update its words, like GHOST ON. */
    var onSettings: (() -> Unit)? = null

    // Lane C hook
    var onLiveAction: ((String) -> Unit)? = null
    // Lane C hook
    var onLiveSettings: (() -> Unit)? = null

    /** Phase 1.2: Record / Stop and Play last video live in the panel now. The camera screen runs them. */
    var onScreenAction: ((PanelAction) -> Unit)? = null

    private var chained: Runnable? = null
    private val radarPing = object : Runnable {
        override fun run() {
            val s = engine.settings
            if (!s.on(FxKeys.RADAR_ON) && !s.on(FxKeys.MIRROR_ON)) return
            sounds.playRadar(s)
            main.postDelayed(this, radarTurnMs())
        }
    }

    // Ink being drawn with a finger, as shares of the video.
    private var inkPoints = FloatArray(INK_START)
    private var inkCount = 0

    fun start() {
        val savedClip = panel.store.text(FxKeys.AR_CLIP)
        val savedUri = panel.store.text(ArDeckCatalog.URI_PREF)
        if (savedClip.isNotBlank()) deckCatalog.restore(savedClip, savedUri)
        refresh()
        panel.onSettingChanged = { refresh() }
        panel.onToggled = { key, on -> toggled(key, on) }
        panel.onAction = { action, arg -> act(action, arg) }
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
        sounds.release()
    }

    /** The app went to the background: no sounds or chained effects until it comes back. */
    fun pause() {
        main.removeCallbacks(radarPing)
        chained?.let { main.removeCallbacks(it) }
        chained = null
        processor?.setDeckForeground(false)
    }

    fun resume() {
        val s = engine.settings
        main.removeCallbacks(radarPing)
        if (s.on(FxKeys.RADAR_ON) || s.on(FxKeys.MIRROR_ON)) main.postDelayed(radarPing, radarTurnMs())
        processor?.setDeckForeground(true)
    }

    /** Reads every effect setting from the panel again and hands it to the effects. */
    fun refresh() {
        val before = engine.settings
        engine.update(panel.effectSnapshot())
        engine.panelColor = panel.glowColor
        val now = engine.settings
        pushReplay()
        pushDeck()
        val radar = now.on(FxKeys.RADAR_ON) || now.on(FxKeys.MIRROR_ON)
        val radarBefore = before.on(FxKeys.RADAR_ON) || before.on(FxKeys.MIRROR_ON)
        if (radar && !radarBefore) {
            main.removeCallbacks(radarPing)
            main.postDelayed(radarPing, radarTurnMs())
        }
        if (now.on(FxKeys.EMOJI_ON) && !before.on(FxKeys.EMOJI_ON)) sounds.play(SOUND_EMOJI, now)
        onSettings?.invoke()
        // Lane C hook
        onLiveSettings?.invoke()
    }

    private fun pushReplay() {
        val s = engine.settings
        processor?.setReplay(s.on(FxKeys.REPLAY_ON), s.raw(FxKeys.REPLAY_SECONDS).coerceAtLeast(MIN_REPLAY_SECONDS))
    }

    private fun pushDeck() {
        val s = engine.settings
        processor?.applyDeck(s.on(FxKeys.AR_ON), s.on(FxKeys.AR_LOOP), s.text(FxKeys.AR_CLIP))
    }

    /** A video chosen with the system file picker. The id is what Phone 2 sends. */
    fun addArClip(name: String, uri: String) {
        if (!ArDeckCatalog.isLocal(uri)) return
        val clip = deckCatalog.add(name, uri)
        panel.store.setText(FxKeys.AR_CLIP, clip.id)
        panel.store.setText(ArDeckCatalog.URI_PREF, clip.uri)
        panel.store.setToggle(FxKeys.AR_ON, true)
        refresh()
    }

    /** Phone 2: play, pause, or skip, on the existing remote channel. */
    fun arRemote(action: String) {
        when (action) {
            ArDeckCatalog.PLAY -> panel.store.setToggle(FxKeys.AR_ON, true)
            ArDeckCatalog.PAUSE -> panel.store.setToggle(FxKeys.AR_ON, false)
            ArDeckCatalog.NEXT -> {
                val clip = deckCatalog.next() ?: return
                panel.store.setText(FxKeys.AR_CLIP, clip.id)
                panel.store.setText(ArDeckCatalog.URI_PREF, clip.uri)
                panel.store.setToggle(FxKeys.AR_ON, true)
            }
            else -> return
        }
        refresh()
    }

    private fun radarTurnMs(): Long {
        val s = engine.settings
        val speed = if (s.on(FxKeys.MIRROR_ON) && !s.on(FxKeys.RADAR_ON)) s.value(FxKeys.MIRROR_SPEED) else s.value(FxKeys.RADAR_SPEED)
        return ((6f - 5f * speed) * 1000f).toLong()
    }

    private fun toggled(key: String, on: Boolean) {
        val s = engine.settings
        sounds.play(SOUND_TOGGLE, s)
        // Feature 18: fireworks burst when a toggle flips. Turning fireworks off stays quiet.
        if (s.on(FxKeys.FIREWORKS_ON) && !(key == FxKeys.FIREWORKS_ON && !on)) fire(Fire.FIREWORKS)
    }

    /** Fires a one-shot effect with its sound. Quiet mode can chain the next effect. */
    fun fire(kind: Fire, arg: String? = null) {
        val s = engine.settings
        val word = when {
            kind == Fire.LASER_WRITE -> (arg ?: s.text(FxKeys.WRITE_TEXT)).ifEmpty { DEFAULT_WORDS }
            // Shatter cracks out from the face when one is seen, or from the middle.
            kind == Fire.SHATTER && arg == null ->
                StageMath.faceInVideo(engine.freshFace(engine.now()), s)?.let { "${it.x},${it.y}" }
            else -> arg
        }
        engine.fire(kind, word)
        FIRE_SOUNDS[kind]?.let { sounds.play(it, s) }
        if (kind == Fire.QUIET) chainAfterQuiet()
    }

    private fun chainAfterQuiet() {
        chained?.let { main.removeCallbacks(it) }
        val s = engine.settings
        val next = when (s.choice(FxKeys.QUIET_THEN)) {
            FxKeys.THEN_FIREWORKS -> Fire.FIREWORKS
            FxKeys.THEN_SWEEP -> Fire.RED_SWEEP
            FxKeys.THEN_SHATTER -> Fire.SHATTER
            FxKeys.THEN_WRITE -> Fire.LASER_WRITE
            else -> return
        }
        val quiet = EffectTimes.quiet(s)
        // The next effect fires the moment the light comes back on.
        val delay = ((quiet.fade + quiet.dark + quiet.flicker) * 1000f).toLong()
        val task = Runnable { fire(next) }
        chained = task
        main.postDelayed(task, delay)
    }

    private fun act(action: PanelAction, arg: String?) {
        when (action) {
            PanelAction.FIRE -> {
                val kind = Fire.entries.firstOrNull { it.name == arg } ?: return
                fire(kind)
                panel.announce(context.getString(R.string.fx_fired))
            }
            PanelAction.CLEAR_INK -> {
                engine.clearInk()
                panel.announce(context.getString(R.string.fx_ink_cleared))
            }
            PanelAction.TRY_SOUND -> {
                sounds.play(arg ?: return, engine.settings, force = true)
            }
            PanelAction.TEST_CODE -> engine.fire(Fire.TEST_CODE, arg)
            PanelAction.KICKED -> kicked(arg ?: return)
            PanelAction.OPEN_PLATFORM -> openPlatform(arg ?: return)
            PanelAction.SAVE_TO_GALLERY -> saveLastVideo()
            PanelAction.SAVE_REPLAY -> saveReplay()
            // The phone's locked storage is slow, so the code work runs off the screen's thread.
            PanelAction.HANDSHAKE_SHOW -> inBackground({ handshake.myCode() }) { showMyCode(it) }
            PanelAction.HANDSHAKE_NEW -> inBackground({ handshake.newCode() }) { showMyCode(it, fresh = true) }
            PanelAction.HANDSHAKE_ADD -> withCopiedCode { code -> inBackground({ handshake.trust(code) }) { say(handshakeLine(it)) } }
            PanelAction.HANDSHAKE_BLOCK -> withCopiedCode { code -> inBackground({ handshake.block(code) }) { say(handshakeLine(it)) } }
            PanelAction.HANDSHAKE_CHECK -> withCopiedCode { code -> inBackground({ handshake.check(code) }) { say(handshakeLine(it)) } }
            PanelAction.RECORD_TOGGLE, PanelAction.PLAY_LAST -> onScreenAction?.invoke(action)
            // Lane C hook
            else -> onLiveAction?.invoke(action.name)
        }
    }

    /** Feature 28: flags the kick, says what to do, and runs the red sweep. It never rejoins. */
    private fun kicked(platform: String) {
        val title = panel.folderTitle(platform) ?: platform
        if (engine.settings.on(FxKeys.SWEEP_GO_PREFIX + platform)) fire(Fire.RED_SWEEP)
        panel.announce(context.getString(R.string.fx_kicked, title))
    }

    private fun openPlatform(platform: String) {
        val title = panel.folderTitle(platform) ?: platform
        val message = when (PlatformLinks.open(context, platform, title)) {
            PlatformLinks.Opened.APP -> R.string.fx_opened_app
            PlatformLinks.Opened.STORE -> R.string.fx_opened_store
            PlatformLinks.Opened.NOTHING -> R.string.fx_open_failed
        }
        panel.announce(context.getString(message, title))
    }

    private fun saveLastVideo() {
        val file = lastVideo()
        if (file == null || !file.exists() || file.length() == 0L) {
            say(context.getString(R.string.fx_no_video))
            return
        }
        say(context.getString(R.string.fx_saving))
        worker.execute {
            val name = GallerySaver.save(context, file)
            main.post {
                say(if (name != null) context.getString(R.string.fx_gallery_saved, name) else context.getString(R.string.fx_gallery_failed))
            }
        }
    }

    private fun saveReplay() {
        val painter = processor
        if (painter == null || !engine.settings.on(FxKeys.REPLAY_ON)) {
            say(context.getString(R.string.fx_replay_off))
            return
        }
        say(context.getString(R.string.fx_saving))
        worker.execute {
            val file = File(context.cacheDir, REPLAY_FILE)
            val limit = painter.replayMemoryLimitSeconds()
            val kept = painter.saveReplay(file)
            val name = if (kept) GallerySaver.save(context, file, kind = "Replay") else null
            file.delete()
            main.post {
                say(
                    when {
                        !kept -> context.getString(R.string.fx_replay_empty)
                        name == null -> context.getString(R.string.fx_gallery_failed)
                        limit != null -> context.getString(R.string.fx_gallery_saved, name) + " " +
                            context.getString(R.string.replay_memory_limited, limit)
                        else -> context.getString(R.string.fx_gallery_saved, name)
                    },
                )
            }
        }
    }

    private fun <T> inBackground(work: () -> T, then: (T) -> Unit) {
        worker.execute {
            val result = work()
            main.post { then(result) }
        }
    }

    private fun showMyCode(code: String?, fresh: Boolean = false) {
        if (code == null) {
            say(context.getString(R.string.fx_code_locked_failed))
            return
        }
        val clip = ClipData.newPlainText(context.getString(R.string.fx_code_label), code)
        // Marked private, so the phone never shows the code in its copy preview.
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        say(context.getString(if (fresh) R.string.fx_code_new else R.string.fx_code_copied))
    }

    private fun withCopiedCode(use: (String) -> Unit) {
        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text.isNullOrBlank()) {
            say(context.getString(R.string.fx_code_copy_first))
            return
        }
        use(text)
    }

    private fun handshakeLine(status: Handshake.Status): String = context.getString(
        when (status) {
            Handshake.Status.TRUSTED -> R.string.fx_code_trusted
            Handshake.Status.BLOCKED -> R.string.fx_code_blocked
            Handshake.Status.UNKNOWN -> R.string.fx_code_unknown
            Handshake.Status.NOT_A_CODE -> R.string.fx_code_not_code
            Handshake.Status.MINE -> R.string.fx_code_mine
        },
    )

    private fun say(text: String) = panel.announce(text)

    /**
     * Feature 1: draws ink with a finger on the camera picture. Only while Draw on the video
     * is on. Otherwise touches pass through as before.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun attachInk(view: View) {
        view.setOnTouchListener { touched, event -> onInkTouch(touched, event) }
    }

    private fun onInkTouch(view: View, event: MotionEvent): Boolean {
        val s = engine.settings
        if (!s.on(FxKeys.INK_ON) || view.width == 0 || view.height == 0) return false
        val x = (event.x / view.width).coerceIn(0f, 1f)
        val y = (event.y / view.height).coerceIn(0f, 1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                inkCount = 0
                addPoint(x, y)
                engine.liveStroke = stroke()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    addPoint((event.getHistoricalX(i) / view.width).coerceIn(0f, 1f), (event.getHistoricalY(i) / view.height).coerceIn(0f, 1f))
                }
                addPoint(x, y)
                engine.liveStroke = stroke()
            }
            MotionEvent.ACTION_UP -> {
                addPoint(x, y)
                engine.liveStroke = null
                engine.addStroke(stroke())
                sounds.play(SOUND_INK, s)
            }
            MotionEvent.ACTION_CANCEL -> engine.liveStroke = null
        }
        return true
    }

    private fun addPoint(x: Float, y: Float) {
        if (inkCount + 2 > inkPoints.size) inkPoints = inkPoints.copyOf(inkPoints.size * 2)
        inkPoints[inkCount++] = x
        inkPoints[inkCount++] = y
    }

    private fun stroke(): InkStroke {
        val s = engine.settings
        val face = if (s.on(FxKeys.INK_PIN)) StageMath.faceInVideo(engine.freshFace(engine.now()), s) else null
        return InkStroke(
            points = inkPoints.copyOf(inkCount),
            count = inkCount,
            faceAnchor = face,
            size = s.value(FxKeys.INK_SIZE),
            glow = s.value(FxKeys.INK_GLOW),
            color = EffectMath.hueColor(s.value(FxKeys.INK_HUE)),
        )
    }

    private companion object {
        const val INK_START = 256
        const val MIN_REPLAY_SECONDS = 5
        const val REPLAY_FILE = "replay-now.mp4"
        const val DEFAULT_WORDS = "OFFGRID"
        const val SOUND_EMOJI = "emoji"
        const val SOUND_INK = "ink"
        const val SOUND_TOGGLE = "toggle"

        val FIRE_SOUNDS = mapOf(
            Fire.FIREWORKS to "fireworks",
            Fire.RED_SWEEP to "sweep",
            Fire.TIME_FREEZE to "freeze",
            Fire.QUIET to "quiet",
            Fire.SHATTER to "shatter",
            Fire.DEPTH_SHIFT to "depth",
            Fire.LASER_WRITE to "write",
        )
    }
}
