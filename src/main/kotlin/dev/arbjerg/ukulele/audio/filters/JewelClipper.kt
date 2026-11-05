package dev.arbjerg.ukulele.audio.filters

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter

class JewelClipperPcmAudioFilter(
    private val next: FloatPcmAudioFilter,
    threshold: Float = 1.0f,
    gain: Float = 1.0f
) : FloatPcmAudioFilter {

    var threshold = threshold
        set(value) {
            require(value >= 0.0f)
            field = value
        }

    var gain = gain
        set(value) {
            require(value >= 0.0f)
            field = value
        }

    override fun process(
        input: Array<FloatArray>,
        offset: Int,
        length: Int
    ) {
        val limit = threshold
        val vol = gain

        for (channel in input) {
            for (i in offset until offset + length) {
                channel[i] = (channel[i] * vol).coerceIn(-limit, limit)
            }
        }

        next.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        //next.seekPerformed(requestedTime, providedTime)
    }

    override fun flush() {
    	//next.flush()
    }
    override fun close() {
    	//next.close()
    }
}
