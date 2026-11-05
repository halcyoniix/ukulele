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
import dev.arbjerg.ukulele.audio.filters.ScropRotationPcmAudioFilter
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.sign

class SmoothedParam(private val getCallback: () -> Double, private val setCallback: (Double) -> Unit) {
    private class Ramp(val target: Double, val seconds: Float)

    private val pending = AtomicReference<Ramp?>(null)
    private var target: Double = 0.0
    private var perSecond = 0f

    fun rampTo(target: Double, seconds: Float) = pending.set(Ramp(target, seconds))
    fun set(target: Double) = rampTo(target, 0f)
    fun get() = getCallback()

    internal fun step(dt: Float) {
        pending.getAndSet(null)?.let { r ->
            target = r.target
            val dist = abs(r.target - getCallback())
            perSecond = if (r.seconds <= 0f) Float.POSITIVE_INFINITY else dist.toFloat() / r.seconds
        }
        val cur = getCallback()
        if (cur == target) return
        val delta = perSecond * dt

        var newVal = target
        if (abs(target - cur) > delta)
        {
            newVal = cur + sign(target - cur) * delta
        }

        setCallback(newVal)
    }
}

class SmoothingFilterWrapper<T : FloatPcmAudioFilter>(
    val inner: T,
    private val sampleRate: Int,
    private val params: List<SmoothedParam>
) : FloatPcmAudioFilter {
    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
        val dt = length.toFloat() / sampleRate
        for (p in params)
        {
            p.step(dt)
        }
        inner.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) = inner.seekPerformed(requestedTime, providedTime)
    override fun flush() = inner.flush()
    override fun close() = inner.close()
}

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

    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
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

    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
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
    private var timescale: SmoothingFilterWrapper<JewelTimescalePcmAudioFilter>? = null
    private var tremolo: SmoothingFilterWrapper<BypassableTremoloPcmAudioFilter>? = null
    private var vibrato: SmoothingFilterWrapper<BypassableVibratoPcmAudioFilter>? = null
    private var eq: SmoothingFilterWrapper<JewelEqualizer>? = null
    private var clipper: SmoothingFilterWrapper<JewelClipperPcmAudioFilter>? = null
    private var rotation: SmoothingFilterWrapper<ScropRotationPcmAudioFilter>? = null

    val timescaleSpeedParam = SmoothedParam({ timescale?.inner!!.speed }, { timescale?.inner!!.setSpeed(it.coerceAtLeast(0.001)) })
    val timescalePitchParam = SmoothedParam({ timescale?.inner!!.getSemitonesFromPitch(timescale?.inner!!.pitch) }, { timescale?.inner!!.setPitchSemiTones(it) })
    val tremoloFrequencyParam = SmoothedParam({ tremolo?.inner!!.getFrequency().toDouble() }, { tremolo?.inner!!.setFrequency(it.toFloat()) })
    val tremoloDepthParam = SmoothedParam({ tremolo?.inner!!.getDepth().toDouble() }, { tremolo?.inner!!.setDepth(it.toFloat().coerceAtLeast(0.0f).coerceAtMost(1.0f)) })
    val vibratoFrequencyParam = SmoothedParam({ vibrato?.inner!!.getFrequency().toDouble() }, { vibrato?.inner!!.setFrequency(it.toFloat()) })
    val vibratoDepthParam = SmoothedParam({ vibrato?.inner!!.getDepth().toDouble() }, { vibrato?.inner!!.setDepth(it.toFloat()) })
    val bassBoostParam = SmoothedParam({ eq?.inner!!.getGain(1).toDouble() }, { eq?.inner!!.setGain(1, it.toFloat()) })
    val clipperThresholdParam = SmoothedParam({ clipper?.inner!!.threshold.toDouble() }, { clipper?.inner!!.threshold = it.toFloat() })
    val rotationParam = SmoothedParam({ rotation?.inner!!.rotationHz.toDouble() }, { rotation?.inner!!.setRotationSpeed(it.toDouble()) })

    override fun buildChain(
        track: AudioTrack?,
        format: AudioDataFormat,
        output: UniversalPcmAudioFilter
    ): MutableList<AudioFilter> {
        val clpr = JewelClipperPcmAudioFilter(output, 1.0f)
        clipper = SmoothingFilterWrapper(clpr, format.sampleRate, listOf(clipperThresholdParam))

        val t = BypassableTremoloPcmAudioFilter(clipper as FloatPcmAudioFilter, format.channelCount, format.sampleRate)
        tremolo = SmoothingFilterWrapper(t, format.sampleRate, listOf(tremoloDepthParam, tremoloFrequencyParam))

        val vib = BypassableVibratoPcmAudioFilter(tremolo as FloatPcmAudioFilter, format.channelCount, format.sampleRate)
        vibrato = SmoothingFilterWrapper(vib, format.sampleRate, listOf(vibratoDepthParam, vibratoFrequencyParam))

        val ts = JewelTimescalePcmAudioFilter(vibrato as FloatPcmAudioFilter, format.channelCount, format.sampleRate)
        timescale = SmoothingFilterWrapper(ts, format.sampleRate, listOf(timescaleSpeedParam, timescalePitchParam))

        val tr = ScropRotationPcmAudioFilter(timescale as FloatPcmAudioFilter, format.sampleRate)
        rotation = SmoothingFilterWrapper(tr, format.sampleRate, listOf(rotationParam))

        val tbb = JewelEqualizer(format.channelCount, rotation as FloatPcmAudioFilter)
        eq = SmoothingFilterWrapper(tbb, format.sampleRate, listOf(bassBoostParam))


        timescaleSpeedParam.set(1.0);
        timescalePitchParam.set(0.0);
        tremoloFrequencyParam.set(2.0);
        tremoloDepthParam.set(0.0);
        vibratoFrequencyParam.set(2.0);
        vibratoDepthParam.set(0.0);
        bassBoostParam.set(0.0);
        clipperThresholdParam.set(1.0);
        rotationParam.set(0.0);

        @Suppress("UNCHECKED_CAST")
        return mutableListOf(eq, timescale, vibrato, tremolo, rotation, clipper) as MutableList<AudioFilter>
    }
}
