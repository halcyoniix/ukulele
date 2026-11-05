package dev.arbjerg.ukulele.audio.filters

import com.github.natanbc.lavadsp.Converter
import com.github.natanbc.lavadsp.ConverterPcmAudioFilter
import com.github.natanbc.lavadsp.util.FloatToFloatFunction
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import java.util.function.Supplier
import kotlin.concurrent.Volatile
import kotlin.math.sin


class JewelVibratoConverter(sampleRate: Int) : Converter {
    private val sampleRate: Float
    private val lfo: Lfo
    private val buffer: RingBuffer
    private var depth = VIBRATO_DEPTH_DEFAULT_PERCENT / 100

    init {
        require(sampleRate >= 1) { "Sample rate < 1" }
        this.sampleRate = sampleRate.toFloat()
        this.lfo = Lfo(sampleRate.toFloat())
        this.buffer = RingBuffer((BASE_DELAY_SEC * sampleRate * 2).toInt())
    }

    fun setDepth(depth: Float) {
        val newDepth = depth.coerceAtLeast(0.0f)
        this.depth = newDepth
    }

    fun setFrequency(frequency: Float) {
        val newFrequency = frequency.coerceAtLeast(0.0f)
        lfo.frequency = newFrequency
    }

    override fun process(input: FloatArray, inputOffset: Int, output: FloatArray, outputOffset: Int, samples: Int) {
        for (i in 0..<samples) {
            val lfoValue = lfo.value
            val maxDelay = (BASE_DELAY_SEC * sampleRate).toInt()

            val delay = lfoValue * depth * maxDelay + ADDITIONAL_DELAY

            output[outputOffset + i] = buffer.getHermiteAt(delay)

            buffer.writeMargined(input[inputOffset + i])
        }
    }

    private class Lfo(private val sampleRate: Float) {
        var frequency: Float = VIBRATO_FREQUENCY_DEFAULT_HZ
        var phase: Float = 0f

        val value: Float
            get() {
                val dp = 2 * Math.PI.toFloat() * frequency / sampleRate
                val value = ((sin(phase.toDouble()) + 1) * 0.5).toFloat()
                phase += dp
                while (phase > 2 * Math.PI) {
                    phase -= (2 * Math.PI).toFloat()
                }
                return value
            }
    }

    private class RingBuffer(private val size: Int) {
        private val buffer: FloatArray
        private var writeIndex = 0

        init {
            this.buffer = FloatArray(size + INTERPOLATOR_MARGIN)
        }

        fun writeMargined(sample: Float) {
            buffer[writeIndex] = sample

            if (writeIndex < INTERPOLATOR_MARGIN) {
                buffer[size + writeIndex] = sample
            }

            writeIndex++
            if (writeIndex == size) {
                writeIndex = 0
            }
        }

        fun getHermiteAt(delay: Float): Float {
            var fReadIndex = writeIndex - 1 - delay
            while (fReadIndex < 0) {
                fReadIndex += size.toFloat()
            }
            while (fReadIndex >= size) {
                fReadIndex -= size.toFloat()
            }

            val iPart = fReadIndex.toInt() // integer part of the delay
            val fPart = fReadIndex - iPart // fractional part of the delay

            return getSampleHermite4p3o(fPart, buffer, iPart)
        }

        companion object {
            private const val INTERPOLATOR_MARGIN = 3

            // Hermite polynomial interpolation
            // 4-point, 3rd-order Hermite (x-form)
            private fun getSampleHermite4p3o(x: Float, buffer: FloatArray, offset: Int): Float {
                val y0 = buffer[offset]
                val y1 = buffer[offset + 1]
                val y2 = buffer[offset + 2]
                val y3 = buffer[offset + 3]


                //c0 = y[1];
                //c1 = (1.0/2.0)*(y[2]-y[0]);
                val c1 = (1f / 2f) * (y2 - y0)
                //c2 = (y[0] - (5.0/2.0)*y[1]) + (2.0*y[2] - (1.0/2.0)*y[3]);
                val c2 = (y0 - (5f / 2f) * y1) + (2f * y2 - (1f / 2f) * y3)
                //c3 = (1.0/2.0)*(y[3]-y[0]) + (3.0/2.0)*(y[1]-y[2]);
                val c3 = (1f / 2f) * (y3 - y0) + (3f / 2f) * (y1 - y2)
                return ((c3 * x + c2) * x + c1) * x + y1
            }
        }
    }

    companion object {
        private const val ADDITIONAL_DELAY = 3
        private const val BASE_DELAY_SEC = 0.002f // 2 ms
        private const val VIBRATO_FREQUENCY_DEFAULT_HZ = 2f
        private const val VIBRATO_FREQUENCY_MAX_HZ = 14f
        private const val VIBRATO_DEPTH_DEFAULT_PERCENT = 50f
    }
}


/**
 * [Vibrato](https://en.wikipedia.org/wiki/Vibrato) filter implementation.
 *
 * Ported from [BerVibrato](https://github.com/Bershov/Vibrato-effect)
 */
class JewelVibratoPcmAudioFilter(downstream: FloatPcmAudioFilter?, channelCount: Int, sampleRate: Int) :
    ConverterPcmAudioFilter<JewelVibratoConverter>(Supplier { JewelVibratoConverter(sampleRate) }, downstream, channelCount) {
    /**
     * Returns the effect frequency.
     *
     * @return The effect frequency.
     */
    //values taken from BerVibrato.h
    @Volatile
    var frequency: Float = 2f
        set(value) {
            converters().forEach { it.setFrequency(value) }
            field = value
        }

    /**
     * Returns the effect depth.
     *
     * @return The effect depth.
     */
    @Volatile
    var depth: Float = 0.5f
        set(value) {
            converters().forEach { it.setDepth(value) }
            field = value
        }
}