package dev.arbjerg.ukulele.audio

import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter
import com.github.natanbc.lavadsp.ConverterPcmAudioFilter
import com.github.natanbc.lavadsp.util.FloatToFloatFunction
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.github.natanbc.lavadsp.Converter
import dev.arbjerg.ukulele.audio.filters.JewelEqualizer
import dev.arbjerg.ukulele.audio.filters.JewelVibratoPcmAudioFilter
import dev.arbjerg.ukulele.audio.filters.JewelClipperPcmAudioFilter
import dev.arbjerg.ukulele.audio.filters.JewelTimescalePcmAudioFilter

// stupid hack for a tremolo filter that supports 0 (bypass)
class BypassableTremoloPcmAudioFilter(
    private val downstream: FloatPcmAudioFilter,
    channelCount: Int,
    sampleRate: Int
) : FloatPcmAudioFilter {
    private val tremolo = TremoloPcmAudioFilter(downstream, channelCount, sampleRate)
    private var desiredTremoloFrequency = 0.0f
    private var desiredTremoloDepth = 0.0f

    fun setFrequency(frequency: Float) = apply {
        desiredTremoloFrequency = frequency
        if (desiredTremoloFrequency > 0.0f) {
            tremolo.frequency = frequency
        }
    }
    fun getFrequency() = desiredTremoloFrequency
    fun setDepth(depth: Float) = apply {
        desiredTremoloDepth = depth
        if (desiredTremoloDepth > 0.0f) {
            tremolo.depth = depth
        }
    }
    fun getDepth() = desiredTremoloDepth

    override fun process(input: Array<FloatArray>, offset: Int, length: Int) {
        if (desiredTremoloDepth <= 0f || desiredTremoloFrequency <= 0f) {
            this.downstream.process(input, offset, length)
        } else {
            tremolo.process(input, offset, length)
        }
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) = tremolo.seekPerformed(requestedTime, providedTime)
    override fun flush() = tremolo.flush()
    override fun close() = tremolo.close()
}

class BypassableVibratoPcmAudioFilter(
    private val downstream: FloatPcmAudioFilter,
    channelCount: Int,
    sampleRate: Int
) : FloatPcmAudioFilter {
    private val tremolo = JewelVibratoPcmAudioFilter(downstream, channelCount, sampleRate)
    private var desiredTremoloFrequency = 0.0f
    private var desiredTremoloDepth = 0.0f

    fun setFrequency(frequency: Float) = apply {
        desiredTremoloFrequency = frequency
        if (desiredTremoloFrequency > 0.0f) {
            tremolo.frequency = frequency
        }
    }
    fun getFrequency() = desiredTremoloFrequency
    fun setDepth(depth: Float) = apply {
        desiredTremoloDepth = depth
        if (desiredTremoloDepth > 0.0f) {
            tremolo.depth = depth
        }
    }
    fun getDepth() = desiredTremoloDepth

    override fun process(input: Array<FloatArray>, offset: Int, length: Int) {
        if (desiredTremoloDepth <= 0f || desiredTremoloFrequency <= 0f) {
            this.downstream.process(input, offset, length)
        } else {
            tremolo.process(input, offset, length)
        }
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) = tremolo.seekPerformed(requestedTime, providedTime)
    override fun flush() = tremolo.flush()
    override fun close() = tremolo.close()
}

class PCMFilterFactory : PcmFilterFactory {
    private var timescale: JewelTimescalePcmAudioFilter? = null
    private var tremolo: BypassableTremoloPcmAudioFilter? = null
    private var vibrato: BypassableVibratoPcmAudioFilter? = null
    private var eq: JewelEqualizer? = null
    private var clipper: JewelClipperPcmAudioFilter? = null

    var speed = 1.0
        get() = field
        set(value) {
            field = value.coerceAtLeast(0.001)
            timescale?.setSpeed(field)
        }

    var pitch = 0.0
        set(value) {
            field = value
            timescale?.setPitchSemiTones(value)
        }

    var tremoloFrequency = 2f
        set(value) {
            field = value
            tremolo?.setFrequency(value)
        }

    var tremoloDepth = 0.0f
        set(value) {
            field = value
            tremolo?.setDepth(value.coerceAtLeast(0.0f))
        }

    var vibratoFrequency = 2f
        set(value) {
            field = value
            vibrato?.setFrequency(value)
        }

    var vibratoDepth = 0.0f
        set(value) {
            field = value
            vibrato?.setDepth(value.coerceAtLeast(0.0f))
        }

    var bassBoost = 0.0f
    	set(value) {
     		field = value
       		eq?.setGain(1, value)
        }

    var clipperThreshold = 1.0f
        set(value) {
            field = value
            clipper?.threshold = clipperThreshold
        }

    override fun buildChain(
        track: AudioTrack?,
        format: AudioDataFormat,
        output: UniversalPcmAudioFilter
    ): MutableList<AudioFilter> {
        val clpr = JewelClipperPcmAudioFilter(
            output, clipperThreshold
        )

        val t = BypassableTremoloPcmAudioFilter(
            clpr, format.channelCount, format.sampleRate
        ).setFrequency(tremoloFrequency)
            .setDepth(tremoloDepth)

        val vib = BypassableVibratoPcmAudioFilter(
            t, format.channelCount, format.sampleRate
        ).setFrequency(vibratoFrequency)
            .setDepth(vibratoDepth)

        val ts = JewelTimescalePcmAudioFilter(
            vib, format.channelCount, format.sampleRate
        ).setSpeed(speed)
            .setPitchSemiTones(pitch)

        val tbb = JewelEqualizer(
            format.channelCount, ts
        )
        tbb.setGain(1, bassBoost)

        clipper = clpr
        tremolo = t
        timescale = ts
        eq = tbb
        vibrato = vib

        return mutableListOf(tbb, ts, vib, t, clpr)
    }
}
