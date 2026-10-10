package com.offgrid.compositor.audio

/** The cave voice: a Freeverb style room. Eight comb filters in parallel, then four all-pass filters. */
class ReverbStage(sampleRate: Int) {
    private val scale = sampleRate / REFERENCE_RATE
    private val combs = COMB_TUNING.map { Comb((it * scale).toInt()) }
    private val allPasses = ALL_PASS_TUNING.map { AllPass((it * scale).toInt()) }

    /** [amount] is 0 to 1: how big the room is and how much of it is heard. */
    fun process(block: FloatArray, count: Int, amount: Float) {
        val feedback = 0.7f + amount * 0.28f
        val wet = 0.15f + amount * 0.3f
        for (i in 0 until count) {
            val input = block[i] * INPUT_GAIN
            var room = 0f
            for (comb in combs) room += comb.next(input, feedback, DAMP)
            for (pass in allPasses) room = pass.next(room)
            block[i] = block[i] * (1f - wet * 0.5f) + room * wet
        }
    }

    fun reset() {
        combs.forEach { it.reset() }
        allPasses.forEach { it.reset() }
    }

    private class Comb(size: Int) {
        private val line = FloatArray(size)
        private var index = 0
        private var damped = 0f

        fun next(input: Float, feedback: Float, damp: Float): Float {
            val out = line[index]
            damped = out * (1f - damp) + damped * damp
            line[index] = input + damped * feedback
            index = (index + 1) % line.size
            return out
        }

        fun reset() {
            line.fill(0f)
            damped = 0f
        }
    }

    private class AllPass(size: Int) {
        private val line = FloatArray(size)
        private var index = 0

        fun next(input: Float): Float {
            val buffered = line[index]
            line[index] = input + buffered * 0.5f
            index = (index + 1) % line.size
            return buffered - input
        }

        fun reset() = line.fill(0f)
    }

    private companion object {
        const val REFERENCE_RATE = 44100f
        const val INPUT_GAIN = 0.015f
        const val DAMP = 0.2f
        val COMB_TUNING = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        val ALL_PASS_TUNING = intArrayOf(556, 441, 341, 225)
    }
}
