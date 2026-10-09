package com.offgrid.compositor.effects

object FxKeys {
    const val CAMERA_ON = "fx.cam.on"
    const val CAMERA_MODE = "fx.cam.mode"
    const val FACE_DIAMOND = "fx.cam.face_diamond"
    const val CLOAK_VOID = "fx.cam.cloak_void"
    const val STATUS_LABEL = "fx.cam.status_label"
    const val GHOST = "fx.cam.ghost"
    const val TAKEOVER = "fx.cam.takeover"
    const val MIRROR_SCAN = "fx.cam.mirror_scan"
    const val TIME_FREEZE = "fx.cam.time_freeze"
    const val ECHO_TRAIL = "fx.cam.echo_trail"
    const val DEPTH_SHIFT = "fx.cam.depth_shift"
    const val SHATTER = "fx.cam.shatter"
    const val QUIET_MODE = "fx.cam.quiet_mode"
    const val SCREEN_BEACH = "fx.screen.beach"
    const val SCREEN_CLOUDS = "fx.screen.clouds"
    const val SCREEN_PORTAL = "fx.screen.portal"
    const val SCREEN_OPTICAL_ILLUSIONS = "fx.screen.optical_illusions"
    const val SCREEN_ROLLER_COASTER = "fx.screen.roller_coaster"
    const val PARTICLE_EMOJI_RAIN = "fx.particle.emoji_rain"
    const val PARTICLE_FIREWORKS = "fx.particle.fireworks"
    const val PARTICLE_FLYING_HEROES = "fx.particle.flying_heroes"
    const val GLOW_BORDER = "fx.light.glow_border"
    const val LASER_WRITING = "fx.light.laser_writing"
    const val RADAR_SWEEP = "fx.light.radar_sweep"
    const val RED_SWEEP = "fx.light.red_sweep"
    const val DRAWING = "fx.light.drawing"
    const val SOUNDBOARD_ON = "fx.soundboard.on"
    const val MIC_ON = "fx.mic.on"
    const val MIC_EFFECTS_ON = "fx.mic.fx.on"
    const val MIC_PITCH = "fx.mic.pitch"
    const val MIC_ROBOT = "fx.mic.robot"
    const val MIC_ROBOT_AMOUNT = "fx.mic.robot.amount"
    const val MIC_ECHO = "fx.mic.echo"
    const val MIC_ECHO_DELAY = "fx.mic.echo.delay"
    const val MIC_ECHO_AMOUNT = "fx.mic.echo.amount"
    const val MIC_RADIO = "fx.mic.radio"
    const val MIC_CAVE = "fx.mic.cave"
    const val MIC_CAVE_AMOUNT = "fx.mic.cave.amount"
    const val MIC_GAIN = "fx.mic.gain"
    const val MIC_GATE = "fx.mic.gate"
    const val MIC_SOUNDS_IN_VIDEO = "fx.mic.sounds_in_video"
    const val MIC_BEAT_GLOW = "fx.mic.beat_glow"
    const val LIVE_OVERLAY = "fx.live.overlay"
    const val REMOTE_ROLE = "fx.remote.role"

    val all = setOf(
        CAMERA_ON,
        MIC_ON,
        MIC_EFFECTS_ON,
        MIC_PITCH,
        MIC_ROBOT,
        MIC_ROBOT_AMOUNT,
        MIC_ECHO,
        MIC_ECHO_DELAY,
        MIC_ECHO_AMOUNT,
        MIC_RADIO,
        MIC_CAVE,
        MIC_CAVE_AMOUNT,
        MIC_GAIN,
        MIC_GATE,
        MIC_SOUNDS_IN_VIDEO,
        MIC_BEAT_GLOW,
        LIVE_OVERLAY,
        REMOTE_ROLE,
    )
}
