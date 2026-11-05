package dev.arbjerg.ukulele.audio.filters

import com.github.natanbc.lavadsp.util.FloatToFloatFunction
import com.github.natanbc.lavadsp.util.VectorSupport
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter

class JewelTremoloPcmAudioFilter(downstream: FloatPcmAudioFilter, channelCount: Int, sampleRate: Int) : FloatPcmAudioFilter {
    private val downstream: FloatPcmAudioFilter
    private val sampleRate: Int
    private val phases: FloatArray
    var frequency: Float = 2.0f
        private set
    private var depth = 0.5f

    init {
        require(sampleRate >= 1) { "Sample rate < 1" }
        this.downstream = downstream
        this.sampleRate = sampleRate
        this.phases = FloatArray(channelCount)
    }

    fun setFrequency(frequency: Float): JewelTremoloPcmAudioFilter {
        val newFrequency = frequency.coerceAtLeast(0.0f)
        this.frequency = newFrequency
        return this
    }

    fun updateFrequency(function: FloatToFloatFunction): JewelTremoloPcmAudioFilter {
        return this.setFrequency(function.apply(this.frequency))
    }

    fun getDepth(): Float {
        return this.depth * 2.0f
    }

    fun setDepth(depth: Float): JewelTremoloPcmAudioFilter {
        val newDepth = depth.coerceAtMost(1.0f).coerceAtLeast(0.0f)
        this.depth = newDepth / 2.0f
        return this
    }

    fun updateDepth(function: FloatToFloatFunction): JewelTremoloPcmAudioFilter {
        return this.setDepth(function.apply(this.depth * 2.0f))
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
        for (channel in input.indices) {
            this.phases[channel] = VectorSupport.tremolo(
                input[channel],
                offset,
                length,
                this.sampleRate,
                this.frequency,
                this.depth,
                this.phases[channel]
            )
        }

        this.downstream.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
    }

    override fun flush() {
    }

    override fun close() {
    }
}