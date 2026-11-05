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

import com.github.natanbc.lavadsp.ConverterPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import java.util.function.Supplier


class ReverberationPcmAudioFilter(downstream: FloatPcmAudioFilter?, channelCount: Int, sampleRate: Int) :
    ConverterPcmAudioFilter<ReverberationConverter>(
        Supplier { ReverberationConverter(sampleRate) },
        downstream,
        channelCount
    ) {
    private var lastSetLines = 4
    private var lastSetSize = 0.053f
    private var lastSetStrength = 0.0f

    private var lastSetDelays = arrayOf(0.037f, 0.042f, 0.048f, 0.053f).toFloatArray()
    private var lastSetGains = arrayOf(0.84f, 0.83f, 0.82f, 0.81f).toFloatArray()

    fun getSize() : Float {
        return lastSetSize
    }

    fun getStrength() : Float {
        return lastSetStrength
    }

    fun getLines() : Int {
        return lastSetLines
    }

    fun setSize(size: Float): ReverberationPcmAudioFilter {
        lastSetSize = size
        update()
        return this
    }

    fun setStrength(strength: Float): ReverberationPcmAudioFilter {
        lastSetStrength = strength
        update()
        return this
    }

    fun setLines(lines: Int): ReverberationPcmAudioFilter {
        if (lastSetLines != lines)
        {
            lastSetDelays = lastSetDelays.copyOf(lines)
            lastSetGains = lastSetGains.copyOf(lines)
        }
        lastSetLines = lines
        update()
        return this
    }

    private fun update()
    {
        for (i in 0 until lastSetLines) {
            val t = if (lastSetLines == 1) 1f else i / (lastSetLines - 1f)
            lastSetDelays[i] = lastSetSize * (0.7f + 0.3f * t)
            lastSetGains[i] = lastSetStrength * (1f - 0.012f * i)
        }
        setDelays(lastSetDelays)
        setGains(lastSetGains)
    }

    /**
     * An array of delay times in seconds.
     * @param delays Delays
     */
    fun setDelays(delays: FloatArray): ReverberationPcmAudioFilter {
        for (converter in converters()) {
            converter.setDelays(delays)
        }
        return this
    }

    /**
     * An array of gain values between 0 and 1 for each delay line.
     * @param gains Gains (preferably between zero and one).
     */
    fun setGains(gains: FloatArray): ReverberationPcmAudioFilter {
        for (converter in converters()) {
            converter.setGains(gains)
        }
        return this
    }
}
