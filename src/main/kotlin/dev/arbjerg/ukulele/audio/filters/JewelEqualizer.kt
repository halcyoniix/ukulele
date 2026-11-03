package dev.arbjerg.ukulele.audio.filters

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Holder of equalizer configuration.
 */
open class JewelEqualizerConfiguration
/**
 * @param bandMultipliers The band multiplier values. Keeps using this array internally, so the values can be changed
 * externally.
 */(protected val bandMultipliers: FloatArray) {
    /**
     * @param band The index of the band. If this is not a valid band index, the method has no effect.
     * @param value The multiplier for this band. Default value is 0. Valid values are from -0.25 to 1. -0.25 means that
     * the given frequency is completely muted and 0.25 means it is doubled. Note that this may change the
     * volume of the output.
     */
    fun setGain(band: Int, value: Float) {
        if (isValidBand(band)) {
            bandMultipliers[band] = max(value, -0.25f)
        }
    }

    /**
     * @param band The index of the band.
     * @return The multiplier for this band. Default value is 0.
     */
    fun getGain(band: Int): Float {
        if (isValidBand(band)) {
            return bandMultipliers[band]
        } else {
            return 0.0f
        }
    }

    private fun isValidBand(band: Int): Boolean {
        return band >= 0 && band < bandMultipliers.size
    }
}


/**
 * An equalizer PCM filter. Applies the equalizer with configuration specified by band multipliers (either set
 * externally or using [.setGain]).
 */
class JewelEqualizer @JvmOverloads constructor(
    channelCount: Int, private val next: FloatPcmAudioFilter, bandMultipliers: FloatArray = FloatArray(
        BAND_COUNT
    )
) : JewelEqualizerConfiguration(bandMultipliers), FloatPcmAudioFilter {
    private val channels: Array<ChannelProcessor?>

    /**
     * @param channelCount Number of channels in the input.
     * @param next The next filter in the chain.
     * @param bandMultipliers The band multiplier values. Keeps using this array internally, so the values can be changed
     * externally.
     */
    /**
     * @param channelCount Number of channels in the input.
     * @param next The next filter in the chain.
     */
    init {
        this.channels = createProcessors(channelCount, bandMultipliers)
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
        for (channelIndex in channels.indices) {
            channels[channelIndex]!!.process(input[channelIndex]!!, offset, offset + length)
        }

        next.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        for (channelIndex in channels.indices) {
            channels[channelIndex]!!.reset()
        }
    }

    @Throws(InterruptedException::class)
    override fun flush() {
        // Nothing to do here.
    }

    override fun close() {
        // Nothing to do here.
    }

    private class ChannelProcessor(private val bandMultipliers: FloatArray) {
        private val history: FloatArray

        private var current = 0
        private var minusOne = 2
        private var minusTwo = 1

        init {
            this.history = FloatArray(BAND_COUNT * 6)
        }

        fun process(samples: FloatArray, startIndex: Int, endIndex: Int) {
            for (sampleIndex in startIndex..<endIndex) {
                val sample = samples[sampleIndex]
                var result = sample * 0.25f

                for (bandIndex in 0..<BAND_COUNT) {
                    val x = bandIndex * 6
                    val y = x + 3

                    val coefficients = coefficients48000[bandIndex]

                    val bandResult = coefficients.alpha * (sample - history[x + minusTwo]) +
                            coefficients.gamma * history[y + minusOne] -
                            coefficients.beta * history[y + minusTwo]

                    history[x + current] = sample
                    history[y + current] = bandResult

                    result += bandResult * bandMultipliers[bandIndex]
                }

                samples[sampleIndex] = min(max(result * 4.0f, -1.0f), 1.0f)

                if (++current == 3) {
                    current = 0
                }

                if (++minusOne == 3) {
                    minusOne = 0
                }

                if (++minusTwo == 3) {
                    minusTwo = 0
                }
            }
        }

        fun reset() {
            Arrays.fill(history, 0.0f)
        }
    }

    private class Coefficients(val beta: Float, val alpha: Float, val gamma: Float)
    companion object {
        /**
         * Number of bands in the equalizer.
         */
        const val BAND_COUNT: Int = 15

        private const val SAMPLE_RATE = 48000

        private val coefficients48000 = arrayOf<Coefficients>(
            Coefficients(9.9847546664e-01f, 7.6226668143e-04f, 1.9984647656e+00f),
            Coefficients(9.9756184654e-01f, 1.2190767289e-03f, 1.9975344645e+00f),
            Coefficients(9.9616261379e-01f, 1.9186931041e-03f, 1.9960947369e+00f),
            Coefficients(9.9391578543e-01f, 3.0421072865e-03f, 1.9937449618e+00f),
            Coefficients(9.9028307215e-01f, 4.8584639242e-03f, 1.9898465702e+00f),
            Coefficients(9.8485897264e-01f, 7.5705136795e-03f, 1.9837962543e+00f),
            Coefficients(9.7588512657e-01f, 1.2057436715e-02f, 1.9731772447e+00f),
            Coefficients(9.6228521814e-01f, 1.8857390928e-02f, 1.9556164694e+00f),
            Coefficients(9.4080933132e-01f, 2.9595334338e-02f, 1.9242054384e+00f),
            Coefficients(9.0702059196e-01f, 4.6489704022e-02f, 1.8653476166e+00f),
            Coefficients(8.5868004289e-01f, 7.0659978553e-02f, 1.7600401337e+00f),
            Coefficients(7.8409610788e-01f, 1.0795194606e-01f, 1.5450725522e+00f),
            Coefficients(6.8332861002e-01f, 1.5833569499e-01f, 1.1426447155e+00f),
            Coefficients(5.5267518228e-01f, 2.2366240886e-01f, 4.0186190803e-01f),
            Coefficients(4.1811888447e-01f, 2.9094055777e-01f, -7.0905944223e-01f)
        )

        /**
         * @param format Audio output format.
         * @return `true` if the output format is compatible for the equalizer (based on sample rate).
         */
        fun isCompatible(format: AudioDataFormat): Boolean {
            return format.sampleRate == SAMPLE_RATE
        }

        private fun createProcessors(channelCount: Int, bandMultipliers: FloatArray): Array<ChannelProcessor?> {
            val processors = arrayOfNulls<ChannelProcessor>(channelCount)

            for (i in 0..<channelCount) {
                processors[i] = ChannelProcessor(bandMultipliers)
            }

            return processors
        }
    }
}
