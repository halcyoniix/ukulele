/**
 * Copyright 2022 Rohan Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.arbjerg.ukulele.audio.filters.echoreverb

import com.github.natanbc.lavadsp.Converter
import kotlin.math.max

class ReverberationConverter(private val sampleRate: Int) : Converter {
    private var delays = floatArrayOf(0.037f, 0.042f, 0.048f, 0.053f)
    private var gains = floatArrayOf(0.84f, 0.83f, 0.82f, 0.81f)
    private var buffers: FloatArray

    private var position = 0

    init {
        this.buffers = FloatArray(calculateBufferSize())
    }

    fun setDelays(delays: FloatArray) {
        this.delays = delays
        val newSize = calculateBufferSize()
        if (this.buffers.size != newSize) {
            this.buffers = this.buffers.copyOf(newSize)
        }
    }

    fun setGains(gains: FloatArray) {
        this.gains = gains
        val newSize = calculateBufferSize()
        if (this.buffers.size != newSize) {
            this.buffers = this.buffers.copyOf(newSize)
        }
    }

    private fun calculateBufferSize(): Int {
        var maxDelay = (sampleRate * delays[0]).toInt()
        for (i in 1..<delays.size) {
            maxDelay = max(maxDelay, (sampleRate * delays[i]).toInt())
        }
        return maxDelay
    }

    override fun process(input: FloatArray, inputOffset: Int, output: FloatArray, outputOffset: Int, samples: Int) {
        for (i in 0..<samples) {
            val current = input[i + inputOffset]
            var outputSample = 0f

            // Process each delay line
            for (j in delays.indices) {
                val delaySamples = (sampleRate * delays[j]).toInt()
                val gain = gains[j]

                // Read from delay buffer
                var readPosition = position - delaySamples
                if (readPosition < 0) {
                    readPosition += buffers.size
                }
                val delayedSample = buffers[readPosition]

                // Update delay buffer
                buffers[position] = current + (delayedSample * gain)

                // Update output sample
                outputSample += delayedSample

                // Update position
                position++
                if (position >= buffers.size) {
                    position = 0
                }
            }

            // Add the output sample to the current sample
            output[i + outputOffset] = current + (outputSample / delays.size)
        }
    }
}
