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

class EchoPcmAudioFilter(
    downstream: FloatPcmAudioFilter?,
    channelCount: Int,
    sampleRate: Int
) : ConverterPcmAudioFilter<EchoConverter>(
    { EchoConverter(sampleRate) },
    downstream,
    channelCount
) {
    private var lastSetDelay = 0.0
    private var lastSetDecay = 1.0f

    fun getDelay() : Double {
        return lastSetDelay
    }

    fun getDecay() : Float {
        return lastSetDecay
    }

    fun setDelay(delay: Double) = apply {
        val newDelay = delay.coerceAtLeast(0.0)
        lastSetDelay = newDelay
        converters().forEach { it.setDelay(newDelay) }
    }

    fun setDecay(decay: Float) = apply {
        val newDecay = decay.coerceAtLeast(0.0f)
        lastSetDecay = newDecay
        converters().forEach { it.setDecay(newDecay) }
    }
}