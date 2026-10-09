package com.offgrid.compositor.effects

object FxKeys {
    const val CAMERA_ON = "fx.cam.on"
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
