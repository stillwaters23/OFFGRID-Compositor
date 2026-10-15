package com.offgrid.compositor.effects

/**
 * The saved setting keys for every effect (final build, milestone 4).
 *
 * Numbers are typed 0 to 100. Toggles are on or off. Choices are one of a few words.
 * Texts are short typed words. Every key starts with "fx." so the panel can hand the
 * camera screen a snapshot of all of them at once. The start value of each one is in
 * PanelTree, next to the folder that shows it.
 */
object FxKeys {
    // Lane C hook: Live and Remote controls.
    const val LIVE_OVERLAY = "fx.live.overlay"
    const val REMOTE_ROLE = "fx.remote.role"
    const val REMOTE_ROLE_OFF = "off"
    const val REMOTE_ROLE_CAMERA = "camera"
    const val REMOTE_ROLE_REMOTE = "remote"
    const val LIVE_PC_ON = "fx.live.pc.on"
    const val LIVE_PC_PROTOCOL = "fx.live.pc.protocol"
    const val LIVE_PC_HOST = "fx.live.pc.host"
    const val LIVE_PC_PORT = "fx.live.pc.port"
    const val LIVE_PC_RES = "fx.live.pc.res"
    const val LIVE_PC_BITRATE = "fx.live.pc.bitrate"
    const val AUTO_CURTAIN_ON = "fx.live.curtain.on"
    const val AUTO_CURTAIN_MODE = "fx.live.curtain.mode"
    const val AUTO_CURTAIN_HEX = "fx.live.curtain.hex"
    const val CURTAIN_BLACK = "black"
    const val CURTAIN_WHITE = "white"
    const val CURTAIN_CUSTOM = "custom"

    const val CAMERA_ON = "fx.cam.on"
    const val MIC_ON = "fx.mic.on"
    const val MIC_FX_ON = "fx.mic.fx.on"
    const val MIC_PITCH = "fx.mic.pitch"
    const val MIC_ROBOT_ON = "fx.mic.robot"
    const val MIC_ROBOT_AMOUNT = "fx.mic.robot.amount"
    const val MIC_ECHO_ON = "fx.mic.echo"
    const val MIC_ECHO_DELAY = "fx.mic.echo.delay"
    const val MIC_ECHO_AMOUNT = "fx.mic.echo.amount"
    const val MIC_RADIO_ON = "fx.mic.radio"
    const val MIC_CAVE_ON = "fx.mic.cave"
    const val MIC_CAVE_AMOUNT = "fx.mic.cave.amount"
    const val MIC_GAIN = "fx.mic.gain"
    const val MIC_GATE = "fx.mic.gate"
    const val MIC_SOUNDS_IN_VIDEO = "fx.mic.sounds_in_video"
    const val MIC_BEAT_GLOW = "fx.mic.beat_glow"
    const val MIC_BLOCKED = "fx.mic.blocked"
    const val MIC_VOICE_SWAP = "fx.mic.voice_swap"
    const val MIC_BYPASS = "fx.mic.bypass"
    const val MIC_WET = "fx.mic.wet"
    const val MIC_REX_PITCH = "fx.mic.rex.pitch"
    const val MIC_FORMANT = "fx.mic.formant"
    const val MIC_LOW_EQ = "fx.mic.eq.low"
    const val MIC_MID_EQ = "fx.mic.eq.mid"
    const val MIC_HIGH_EQ = "fx.mic.eq.high"

    // 📁 Camera: the no-face modes (features 9, 10, 11).
    const val CAMERA_MODE = "fx.camera.mode"
    const val MODE_LIVE = "live"
    const val MODE_BLACKOUT = "blackout"
    const val MODE_CLOAK = "cloak"
    const val MODE_STATUS = "status"
    const val MODE_GHOST = "ghost"

    const val CLOAK_STRENGTH = "fx.cloak.strength"
    const val CLOAK_FADE = "fx.cloak.fade"
    const val CLOAK_SHIMMER = "fx.cloak.shimmer"

    const val STATUS_TEXT = "fx.status.text"
    const val STATUS_SIZE = "fx.status.size"
    const val STATUS_FADE = "fx.status.fade"

    const val GHOST_STYLE = "fx.ghost.style"
    const val GHOST_FROZEN = "frozen"
    const val GHOST_BLANK = "blank"

    // The person mask (Lane E). MediaPipe cuts the body out of the video box.
    const val BODY_MODE = "fx.body.mode"
    const val BODY_OFF = "off"
    const val BODY_SEE = "see"
    const val BODY_REPLACE = "replace"
    const val BODY_CLOAK = "cloak"
    const val BODY_HOLO = "hologram"
    const val BODY_PLATE = "fx.body.plate"
    const val BODY_PLATE_BLACK = "black"
    const val BODY_PLATE_VOID = "void"
    const val BODY_EDGE = "fx.body.edge"

    // The face diamond: a diamond that covers the face.
    const val DIAMOND_ON = "fx.diamond.on"
    const val DIAMOND_STYLE = "fx.diamond.style"
    const val DIAMOND_CLASSIC = "classic"
    const val DIAMOND_LASER = "laser"
    const val DIAMOND_BLACK = "black"
    const val DIAMOND_SIZE = "fx.diamond.size"
    const val DIAMOND_X = "fx.diamond.x"
    const val DIAMOND_Y = "fx.diamond.y"
    const val DIAMOND_FOLLOW = "fx.diamond.follow"
    const val FACE_FLIP_X = "fx.face.flip_x"

    // Screen takeover and flip-flop (feature 12), inside Royalty's own video.
    const val TAKEOVER_ON = "fx.takeover.on"
    const val TAKEOVER_ZOOM = "fx.takeover.zoom"
    const val TAKEOVER_INSET = "fx.takeover.inset"
    const val TAKEOVER_INSET_SIZE = "fx.takeover.inset_size"
    const val TAKEOVER_FLIP = "fx.takeover.flip"

    // 📁 Effects.
    const val EMOJI_ON = "fx.emoji.on"
    const val EMOJI_DENSITY = "fx.emoji.density"
    const val EMOJI_SPEED = "fx.emoji.speed"
    const val EMOJI_SIZE = "fx.emoji.size"
    const val EMOJI_SET = "fx.emoji.set"

    const val FIREWORKS_ON = "fx.fireworks.on"
    const val FIREWORKS_SIZE = "fx.fireworks.size"
    const val FIREWORKS_COUNT = "fx.fireworks.count"
    const val FIREWORKS_DURATION = "fx.fireworks.duration"

    const val BORDER_ON = "fx.border.on"
    const val BORDER_SPEED = "fx.border.speed"
    const val BORDER_THICKNESS = "fx.border.thickness"
    const val BORDER_GLOW = "fx.border.glow"
    const val BORDER_COLOR = "fx.border.color"
    const val COLOR_PANEL = "panel"
    const val COLOR_WHITE = "white"
    const val COLOR_RAINBOW = "rainbow"
    const val COLOR_HUE = "hue"
    const val BORDER_HUE = "fx.border.hue"

    const val INK_ON = "fx.ink.on"
    const val INK_SIZE = "fx.ink.size"
    const val INK_GLOW = "fx.ink.glow"
    const val INK_HUE = "fx.ink.hue"
    const val INK_PIN = "fx.ink.pin"

    const val WRITE_TEXT = "fx.write.text"
    const val WRITE_SPEED = "fx.write.speed"
    const val WRITE_SIZE = "fx.write.size"
    const val WRITE_GLOW = "fx.write.glow"
    const val WRITE_STAY = "fx.write.stay"
    const val WRITE_HUE = "fx.write.hue"

    const val PORTAL_ON = "fx.portal.on"
    const val PORTAL_STYLE = "fx.portal.style"
    const val PORTAL_SCENE = "scene"
    const val PORTAL_WORMHOLE = "wormhole"
    const val PORTAL_VOID = "void"
    const val PORTAL_SIZE = "fx.portal.size"
    const val PORTAL_SPIN = "fx.portal.spin"
    const val PORTAL_X = "fx.portal.x"
    const val PORTAL_Y = "fx.portal.y"
    const val PORTAL_GLOW = "fx.portal.glow"

    const val HEROES_ON = "fx.heroes.on"
    const val HEROES_HERO = "fx.heroes.hero"
    const val HERO_NOVA = "nova"
    const val HERO_VOLT = "volt"
    const val HERO_COMET = "comet"
    const val HERO_PIXEL = "pixel"
    const val HEROES_COUNT = "fx.heroes.count"
    const val HEROES_SIZE = "fx.heroes.size"
    const val HEROES_SPEED = "fx.heroes.speed"

    const val COASTER_ON = "fx.coaster.on"
    const val COASTER_VIEW = "fx.coaster.view"
    const val COASTER_WATCH = "watch"
    const val COASTER_RIDE = "ride"
    const val COASTER_SPEED = "fx.coaster.speed"
    const val COASTER_SIZE = "fx.coaster.size"
    const val COASTER_CARS = "fx.coaster.cars"

    const val CLOUDS_ON = "fx.clouds.on"
    const val CLOUDS_PLACE = "fx.clouds.place"
    const val CLOUDS_FULL = "full"
    const val CLOUDS_AROUND = "around"
    const val CLOUDS_SPEED = "fx.clouds.speed"
    const val CLOUDS_THICKNESS = "fx.clouds.thickness"
    const val CLOUDS_HEIGHT = "fx.clouds.height"

    const val BEACH_ON = "fx.beach.on"
    const val BEACH_WAVES = "fx.beach.waves"
    const val BEACH_BRIGHTNESS = "fx.beach.brightness"

    const val FREEZE_LENGTH = "fx.freeze.length"
    const val FREEZE_GLITCH = "fx.freeze.glitch"

    const val ILLUSION_ON = "fx.illusion.on"
    const val ILLUSION_KIND = "fx.illusion.kind"
    const val ILLUSION_TUNNEL = "tunnel"
    const val ILLUSION_TRIANGLE = "triangle"
    const val ILLUSION_STAIRS = "stairs"
    const val ILLUSION_DRIFT = "drift"
    const val ILLUSION_SIZE = "fx.illusion.size"
    const val ILLUSION_SPEED = "fx.illusion.speed"
    const val ILLUSION_STRENGTH = "fx.illusion.strength"

    const val SWEEP_SPEED = "fx.sweep.speed"
    const val SWEEP_WIDTH = "fx.sweep.width"
    const val SWEEP_LENGTH = "fx.sweep.length"

    /** Each platform folder's own Red sweep toggle: "fx.sweep.go." plus the platform id. */
    const val SWEEP_GO_PREFIX = "fx.sweep.go."

    const val DEPTH_PUSH = "fx.depth.push"
    const val DEPTH_SPEED = "fx.depth.speed"
    const val DEPTH_HOLD = "fx.depth.hold"
    const val DEPTH_SNAP = "fx.depth.snap"

    const val RADAR_ON = "fx.radar.on"
    const val RADAR_MODE = "fx.radar.mode"
    const val RADAR_FACE = "face"
    const val RADAR_ROOM = "room"
    const val RADAR_SPEED = "fx.radar.speed"
    const val RADAR_SIZE = "fx.radar.size"
    const val RADAR_GLOW = "fx.radar.glow"
    const val RADAR_HUE = "fx.radar.hue"

    const val ECHO_ON = "fx.echo.on"
    const val ECHO_LENGTH = "fx.echo.length"
    const val ECHO_COPIES = "fx.echo.copies"
    const val ECHO_FADE = "fx.echo.fade"

    const val MIRROR_ON = "fx.mirror.on"
    const val MIRROR_SPEED = "fx.mirror.speed"
    const val MIRROR_BLIPS = "fx.mirror.blips"

    const val QUIET_DARK = "fx.quiet.dark"
    const val QUIET_FADE = "fx.quiet.fade"
    const val QUIET_FLICKERS = "fx.quiet.flickers"
    const val QUIET_LIGHT = "fx.quiet.light"
    const val QUIET_THEN = "fx.quiet.then"
    const val THEN_NOTHING = "nothing"
    const val THEN_FIREWORKS = "fireworks"
    const val THEN_SWEEP = "sweep"
    const val THEN_SHATTER = "shatter"
    const val THEN_WRITE = "write"

    const val SHATTER_PIECES = "fx.shatter.pieces"
    const val SHATTER_FALL = "fx.shatter.fall"
    const val SHATTER_BROKEN = "fx.shatter.broken"
    const val SHATTER_BACK = "fx.shatter.back"
    const val SHATTER_BEHIND = "fx.shatter.behind"
    const val BEHIND_BLACK = "black"
    const val BEHIND_VOID = "void"
    const val BEHIND_BEACH = "beach"

    // 📁 Effects › 📁 Soundboard (feature 6). Sounds play on Royalty's phone only.
    const val SOUND_ON = "fx.sound.on"
    const val SOUND_VOLUME = "fx.sound.volume"
    const val SOUND_PREFIX = "fx.sound.for."

    // 📁 Record (feature 7 and the record bar).
    const val RECORD_SECONDS = "fx.record.seconds"
    const val RECORD_GALLERY = "fx.record.gallery"
    const val REPLAY_ON = "fx.replay.on"
    const val REPLAY_SECONDS = "fx.replay.seconds"

    // AR Video Deck. A local clip, looped, drawn over the live selfie. No player chrome.
    const val AR_ON = "fx.ar.on"
    const val AR_LOOP = "fx.ar.loop"
    const val AR_OPACITY = "fx.ar.opacity"
    const val AR_CLIP = "fx.ar.clip"
}
