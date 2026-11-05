package dev.arbjerg.ukulele.audio

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
import dev.arbjerg.ukulele.audio.filters.JewelTremoloPcmAudioFilter
import dev.arbjerg.ukulele.audio.filters.ScropRotationPcmAudioFilter
import dev.arbjerg.ukulele.audio.filters.echoreverb.EchoPcmAudioFilter
import dev.arbjerg.ukulele.audio.filters.echoreverb.ReverberationPcmAudioFilter
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.sign

class SmoothedParam(private val filterFactory: PCMFilterFactory, private val getCallback: () -> Double, private val setCallback: (Double) -> Unit) {
    private class Ramp(val target: Double, val seconds: Float)

    private val pending = AtomicReference<Ramp?>(null)
    private var target: Double = 0.0
    private var perSecond = 0f

    fun rampTo(target: Double, seconds: Float) = pending.set(Ramp(target, seconds))
    fun set(target: Double)
    {
        rampTo(target, 0f)
        step(0.01f)
    }
    fun get() = getCallback()

    init {
        filterFactory.smoothedParams += this
    }

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

class BypassFilter<T : FloatPcmAudioFilter>(
    val effect: T,
    private val downstream: FloatPcmAudioFilter,
    private val shouldBypass: (T) -> Boolean
) : FloatPcmAudioFilter {
    val isBypassed: Boolean get() = shouldBypass(effect)

    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
        if (shouldBypass(effect)) downstream.process(input, offset, length)
        else effect.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) = effect.seekPerformed(requestedTime, providedTime)
    override fun flush() = effect.flush()
    override fun close() = effect.close()
}

class ParamStepper(
    private val downstream: FloatPcmAudioFilter,
    private val sampleRate: Int,
    private val filterFactory: PCMFilterFactory
) : FloatPcmAudioFilter {
    override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
        val dt = length.toFloat() / sampleRate
        filterFactory.step(dt)
        downstream.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) = downstream.seekPerformed(requestedTime, providedTime)
    override fun flush() = downstream.flush()
    override fun close() = downstream.close()
}

class PCMFilterFactory : PcmFilterFactory {
    var smoothedParams: List<SmoothedParam> = emptyList()

    private var stepper: ParamStepper? = null
    private var timescale: JewelTimescalePcmAudioFilter? = null
    private var tremolo: BypassFilter<JewelTremoloPcmAudioFilter>? = null
    private var vibrato: BypassFilter<JewelVibratoPcmAudioFilter>? = null
    private var eq: JewelEqualizer? = null
    private var clipper: JewelClipperPcmAudioFilter? = null
    private var echo: BypassFilter<EchoPcmAudioFilter>? = null
    private var reverb: BypassFilter<ReverberationPcmAudioFilter>? = null
    private var rotation: BypassFilter<ScropRotationPcmAudioFilter>? = null

    val timescaleSpeedParam = SmoothedParam(this, { timescale!!.speed }, { timescale!!.setSpeed(it.coerceAtLeast(0.001)) })
    val timescalePitchParam = SmoothedParam(this, { timescale!!.getSemitonesFromPitch(timescale!!.pitch) }, { timescale!!.setPitchSemiTones(it) })
    val bassBoostParam = SmoothedParam(this, { eq!!.getGain(1).toDouble() }, { eq!!.setGain(1, it.toFloat()) })
    val clipperThresholdParam = SmoothedParam(this, { clipper!!.threshold.toDouble() }, { clipper!!.threshold = it.toFloat() })
    val rotationParam = SmoothedParam(this, { rotation!!.effect.rotationHz }, { rotation!!.effect.setRotationSpeed(it) })

    val tremoloFrequencyParam = SmoothedParam(this, { tremolo!!.effect.frequency.toDouble() }, { tremolo!!.effect.setFrequency(it.toFloat()) })
    val tremoloDepthParam = SmoothedParam(this, { tremolo!!.effect.getDepth().toDouble() }, { tremolo!!.effect.setDepth(it.toFloat().coerceAtLeast(0.0f).coerceAtMost(1.0f)) })

    val vibratoFrequencyParam = SmoothedParam(this, { vibrato!!.effect.frequency.toDouble() }, { vibrato!!.effect.frequency = it.toFloat() })
    val vibratoDepthParam = SmoothedParam(this, { vibrato!!.effect.depth.toDouble() }, { vibrato!!.effect.depth = it.toFloat() })

    val echoDecayParam = SmoothedParam(this, { echo!!.effect.getDecay().toDouble() }, { echo!!.effect.setDecay(it.toFloat()) })
    val echoDelayParam = SmoothedParam(this, { echo!!.effect.getDelay() }, { echo!!.effect.setDelay(it) })

    val reverbLinesParam = SmoothedParam(this, { reverb!!.effect.getLines().toDouble() }, { reverb!!.effect.setLines(it.toInt()) })
    val reverbSizeParam = SmoothedParam(this, { reverb!!.effect.getSize().toDouble() }, { reverb!!.effect.setSize(it.toFloat()) })
    val reverbStrengthParam = SmoothedParam(this, { reverb!!.effect.getStrength().toDouble() }, { reverb!!.effect.setStrength(it.toFloat()) })

    var isFirstRun = true

    override fun buildChain(
        track: AudioTrack?,
        format: AudioDataFormat,
        output: UniversalPcmAudioFilter
    ): MutableList<AudioFilter> {
        if (isFirstRun)
        {
            stepper = ParamStepper(output, format.sampleRate, this)
            clipper = JewelClipperPcmAudioFilter(stepper as FloatPcmAudioFilter, 1.0f)
            echo = BypassFilter(EchoPcmAudioFilter(clipper, format.channelCount, format.sampleRate), clipper as FloatPcmAudioFilter, { it.getDecay() <= 0f || it.getDelay() <= 0f })
            reverb = BypassFilter(ReverberationPcmAudioFilter(echo, format.channelCount, format.sampleRate), echo as FloatPcmAudioFilter, { it.getSize() <= 0f || it.getStrength() <= 0f|| it.getLines() <= 0f })
            tremolo = BypassFilter(JewelTremoloPcmAudioFilter(reverb as FloatPcmAudioFilter, format.channelCount, format.sampleRate), reverb as FloatPcmAudioFilter, { it.getDepth() <= 0f || it.frequency <= 0f })
            vibrato = BypassFilter(JewelVibratoPcmAudioFilter(tremolo as FloatPcmAudioFilter, format.channelCount, format.sampleRate), tremolo as FloatPcmAudioFilter, { it.depth <= 0f || it.frequency <= 0f })
            timescale = JewelTimescalePcmAudioFilter(vibrato as FloatPcmAudioFilter, format.channelCount, format.sampleRate)
            rotation = BypassFilter(ScropRotationPcmAudioFilter(timescale as FloatPcmAudioFilter, format.sampleRate), timescale as FloatPcmAudioFilter, { it.rotationHz == 0.0 })
            eq = JewelEqualizer(format.channelCount, rotation as FloatPcmAudioFilter)

            reset()
            isFirstRun = false
        }

        @Suppress("UNCHECKED_CAST")
        return mutableListOf(eq, rotation, timescale, vibrato, tremolo, reverb, echo, clipper, stepper) as MutableList<AudioFilter>
    }

    fun reset()
    {
        timescaleSpeedParam.set(1.0);
        timescalePitchParam.set(0.0);

        bassBoostParam.set(0.0);
        clipperThresholdParam.set(1.0);
        rotationParam.set(0.0);

        tremoloFrequencyParam.set(2.0);
        tremoloDepthParam.set(0.0);

        vibratoFrequencyParam.set(2.0);
        vibratoDepthParam.set(0.0);

        echoDecayParam.set(0.0)
        echoDelayParam.set(0.0)

        reverbLinesParam.set(4.0)
        reverbSizeParam.set(0.0)
        reverbStrengthParam.set(0.0)
    }

    fun step(dt: Float)
    {
        for (p in smoothedParams)
        {
            p.step(dt)
        }
    }
}
