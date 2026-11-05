package dev.arbjerg.ukulele.audio.filters

import com.github.natanbc.lavadsp.util.DoubleToDoubleFunction
import com.github.natanbc.lavadsp.util.VectorSupport
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import kotlin.math.PI

class ScropRotationPcmAudioFilter(private val downstream: FloatPcmAudioFilter, val sampleRate: Int) : FloatPcmAudioFilter {



	var rotationHz: Double = 0.0
	var x: Double = 0.0
	var dI: Double = 0.0

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

	@Throws(InterruptedException::class)
	override fun process(input: Array<FloatArray?>, offset: Int, length: Int) {
		if (input.size != 2) {
			downstream.process(input, offset, length)
			return
		}
		val (left, right) = input
		x = VectorSupport.rotation(left, right, offset, length, x, dI)
		downstream.process(input, offset, length)
	}

	override fun seekPerformed(requestedTime: Long, providedTime: Long) {

	}
	override fun flush() {

	}
	override fun close() {

	}

}
