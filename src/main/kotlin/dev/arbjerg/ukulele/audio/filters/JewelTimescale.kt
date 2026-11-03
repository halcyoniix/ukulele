package dev.arbjerg.ukulele.audio.filters

import com.github.natanbc.nativeloader.NativeResourceHolder
import com.github.natanbc.lavadsp.natives.TimescaleConverter
import com.github.natanbc.lavadsp.timescale.Setting
import com.github.natanbc.lavadsp.util.DoubleToDoubleFunction
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import kotlin.concurrent.Volatile
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class JewelTimescalePcmAudioFilter(private val downstream: FloatPcmAudioFilter, channels: Int, sampleRate: Int) :
    FloatPcmAudioFilter {
    private val converters: Array<TimescaleConverter>
    private val outputSegments: Array<FloatArray?>

    @Volatile
    var speed: Double = 1.0
        private set

    @Volatile
    var pitch: Double = 1.0
        private set

    @Volatile
    var rate: Double = 1.0
        private set

    init {
        this.converters = arrayOfNulls<TimescaleConverter>(channels) as Array<TimescaleConverter>
        this.outputSegments = arrayOfNulls<FloatArray>(channels)

        for (i in 0..<channels) {
            this.outputSegments[i] = FloatArray(4096)
            this.converters[i] = TimescaleConverter(1, sampleRate)
        }
    }

    fun setSpeed(speed: Double): JewelTimescalePcmAudioFilter {
        for (converter in this.converters) {
            converter.setSpeed(speed)
        }

        this.speed = speed
        return this
    }

    fun updateSpeed(function: DoubleToDoubleFunction): JewelTimescalePcmAudioFilter {
        return this.setSpeed(function.apply(this.speed))
    }

    fun setSpeedChange(change: Double): JewelTimescalePcmAudioFilter {
        return this.setSpeed(1.0 + 0.01 * change)
    }

    fun setPitch(pitch: Double): JewelTimescalePcmAudioFilter {
        for (converter in this.converters) {
            converter.setPitch(pitch)
        }

        this.pitch = pitch
        return this
    }

    fun updatePitch(function: DoubleToDoubleFunction): JewelTimescalePcmAudioFilter {
        return this.setPitch(function.apply(this.pitch))
    }

    fun setPitchOctaves(pitch: Double): JewelTimescalePcmAudioFilter {
        return this.setPitch(exp(0.69314718056 * pitch))
    }

    fun setPitchSemiTones(pitch: Double): JewelTimescalePcmAudioFilter {
        return this.setPitchOctaves(pitch / 12.0)
    }

    fun setRate(rate: Double): JewelTimescalePcmAudioFilter {
        for (converter in this.converters) {
            converter.setRate(rate)
        }

        this.rate = rate
        return this
    }

    fun updateRate(function: DoubleToDoubleFunction): JewelTimescalePcmAudioFilter {
        return this.setRate(function.apply(this.rate))
    }

    fun setRateChange(change: Double): JewelTimescalePcmAudioFilter {
        return this.setRate(1.0 + 0.01 * change)
    }

    val nominalInputSequences: IntArray
        get() {
            val ret = IntArray(this.converters.size)

            for (i in ret.indices) {
                ret[i] = this.converters[i].getNominalInputSequence()
            }

            return ret
        }

    val nominalOutputSequences: IntArray
        get() {
            val ret = IntArray(this.converters.size)

            for (i in ret.indices) {
                ret[i] = this.converters[i].nominalOutputSequence
            }

            return ret
        }

    val initialLatencies: IntArray
        get() {
            val ret = IntArray(this.converters.size)

            for (i in ret.indices) {
                ret[i] = this.converters[i].getInitialLatency()
            }

            return ret
        }

    fun <T> getSetting(setting: Setting<T?>): T? {
        return setting.get(this.converters[0])
    }

    fun <T> setSetting(setting: Setting<T?>, value: T?): Boolean {
        var success = false

        for (converter in this.converters) {
            success = setting.set(converter, value)
        }

        return success
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        for (converter in this.converters) {
            converter.reset()
        }
    }

    override fun flush() {
        for (converter in this.converters) {
            converter.flush()
        }
    }

    override fun close() {
        for (converter in this.converters) {
            converter.close()
        }
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
        var written = 0

        for (i in input.indices) {
            written = this.converters[i].process(input[i], offset, length, this.outputSegments[i], 0, 4096)
        }

        if (written > 0) {
            this.downstream.process(this.outputSegments, 0, written)
        }

        while (true) {
            var allEmpty = true

            for (i in input.indices) {
                written = this.converters[i].read(this.outputSegments[i], 0, 4096)
                if (written != 0) {
                    allEmpty = false
                }
            }

            if (allEmpty) {
                return
            }

            this.downstream.process(this.outputSegments, 0, written)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 4096
    }
}
