package com.offgrid.compositor.audio

/**
 * Microphone Effects, on the phone only. One chain: volume, noise gate, pitch, robot, radio,
 * echo, cave, then a hard stop at full scale. A stage that is off costs nothing, and its old
 * sound is cleared so it never comes back later. Pitch adds about 15 ms of delay. The others add none.
 * Call [process] from one thread. Settings can change from any thread.
 */
class VoiceFx(private val sampleRate: Int = SAMPLE_RATE) {
    @Volatile
    var settings = VoiceSettings()

    private val gate = NoiseGate(sampleRate)
    private val pitch = PitchShifter(sampleRate * PITCH_WINDOW_MS / 1000)
    private val robot = RobotStage(sampleRate)
    private val radio = RadioStage(sampleRate)
    private val echo = EchoStage(sampleRate)
    private val cave = ReverbStage(sampleRate)
    private var lastApplied = VoiceSettings()

    fun process(block: FloatArray, count: Int) {
        val now = settings
        clearStagesTurnedOff(now)
        lastApplied = now
        if (now.gain != 1f) for (i in 0 until count) block[i] *= now.gain
        gate.process(block, count, now.gateLevel)
        if (now.pitchOn) pitch.process(block, count, now.pitchRatio)
        if (now.robotOn) robot.process(block, count, now.robotAmount)
        if (now.radioOn) radio.process(block, count)
        if (now.echoOn) echo.process(block, count, now.echoDelayMs, now.echoFeedback)
        if (now.caveOn) cave.process(block, count, now.caveAmount)
        for (i in 0 until count) {
            val v = block[i]
            block[i] = if (v != v) 0f else v.coerceIn(-1f, 1f)
        }
    }

    private fun clearStagesTurnedOff(now: VoiceSettings) {
        if (lastApplied.pitchOn && !now.pitchOn) pitch.reset()
        if (lastApplied.robotOn && !now.robotOn) robot.reset()
        if (lastApplied.radioOn && !now.radioOn) radio.reset()
        if (lastApplied.echoOn && !now.echoOn) echo.reset()
        if (lastApplied.caveOn && !now.caveOn) cave.reset()
        if (lastApplied.gateLevel > 0f && now.gateLevel == 0f) gate.reset()
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        private const val PITCH_WINDOW_MS = 30
    }
}
