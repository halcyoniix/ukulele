package dev.arbjerg.ukulele.audio.filters

import com.github.natanbc.lavadsp.util.DoubleToDoubleFunction
import com.github.natanbc.lavadsp.util.VectorSupport
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import kotlin.math.PI
import kotlin.math.sin

class ScropRotationPcmAudioFilter(private val downstream: FloatPcmAudioFilter, val sampleRate: Int) : FloatPcmAudioFilter {



	var rotationHz: Double = 0.0
	var x: Double = 0.0
	var dI: Double = 0.0
    var depth: Double = 1.0
        set (value) {
            field = value.coerceAtMost(1.0)
        }

	init {
		setRotationSpeed(0.0)
	}

	fun setRotationSpeed(rotHz: Double) : ScropRotationPcmAudioFilter {
		rotationHz = rotHz
		val samplesPerCycle: Double = sampleRate / (rotHz * 2.0 * PI)
		if (rotHz == 0.0) {
			x = 0.0
			dI = 0.0
		} else {
			dI = 1.0 / samplesPerCycle
		}
		return this
	}

	fun updateRotationSpeed(function: DoubleToDoubleFunction) : ScropRotationPcmAudioFilter = setRotationSpeed(function.apply(rotationHz))

    internal fun rotation(left: FloatArray, right: FloatArray, offset: Int, length: Int, x: Double, dI: Double, depth: Double): Double {
        var x = x
        for (i in 0..<length) {
            val sin = sin(x) * depth
            left[offset + i] = left[offset + i] * (sin + 1.0).toFloat() / 2.0f
            right[offset + i] = right[offset + i] * (-sin + 1.0).toFloat() / 2.0f
            x += dI
        }

        return x
    }

	@Throws(InterruptedException::class)
	override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
		if (input.size != 2) {
			downstream.process(input, offset, length)
			return
		}
		val (left, right) = input
		x = rotation(left!!, right!!, offset, length, x, dI, depth)
		downstream.process(input, offset, length)
	}

	override fun seekPerformed(requestedTime: Long, providedTime: Long) {

	}
	override fun flush() {

	}
	override fun close() {

	}

}
