package dev.arbjerg.ukulele.command

import dev.arbjerg.ukulele.audio.SmoothedParam
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class FilterCommand : Command ("filter", "f") {

    override suspend fun CommandContext.invoke() {
        if (argumentText.isBlank())
            return replyHelp()

        val track = player.tracks.firstOrNull() ?: return reply("Not playing anything.")

        val matcher = matchPattern.matcher(argumentText)

        var replyText = "";

        var lerpTime: Float = 0.0f

        fun setSingleParameter(param: SmoothedParam, paramName: String, value: Double) {
            if (lerpTime == 0.0f) {
                param.set(value)
                replyText += "Set $paramName to $value\n"
            } else {
                replyText += "Started a lerp of $paramName from ${param.get()} to $value over $lerpTime seconds\n"
                param.rampTo(value, lerpTime)
            }
        }

        while (matcher.find()) {
            when {
                matcher.group(1) != null -> {
                    val name = matcher.group(1)
                    val value = matcher.group(2).toDouble()
                    when (name) {
                        "s", "speed" -> {
                            setSingleParameter(player.filterFactory.timescaleSpeedParam, "Speed", value)
                        }
                        "p", "pitch" -> {
                            setSingleParameter(player.filterFactory.timescalePitchParam, "Pitch", value)
                        }
                        "bb", "bassboost" -> {
                            setSingleParameter(player.filterFactory.bassBoostParam, "bass boost", value)
                        }
                        "c", "clip" -> {
                            setSingleParameter(player.filterFactory.clipperThresholdParam, "clipper threshold", value)
                        }
                        "rot", "rotation" -> {
                        	setSingleParameter(player.filterFactory.rotationParam, "rotation (Hz)", value)
                        }
                        "reverbLines" -> {
                            setSingleParameter(player.filterFactory.reverbLinesParam, "reverb lines", value)
                        }
                        "l", "lerp" -> {
                            lerpTime = value.toFloat()
                        }
                    }
                }
                matcher.group(3) != null -> {
                    val param1 = matcher.group(4).toDouble()
                    val param2 = matcher.group(5).toDouble()

					val name = matcher.group(3)
					when (name) {
						"t", "tremolo" -> {
                            if (lerpTime == 0.0f) {
                                player.filterFactory.tremoloFrequencyParam.set(param1)
                                player.filterFactory.tremoloDepthParam.set(param2)

                                val setFreq = player.filterFactory.tremoloFrequencyParam.get();
                                val setDepth = player.filterFactory.tremoloDepthParam.get();

                                if (setFreq == 0.0 || setDepth == 0.0)
                                {
                                    replyText += "Disabled tremolo\n"
                                }
                                else
                                {
                                    replyText += "Set tremolo freq/depth to ${setFreq}/${setDepth}\n"
                                }
                            } else {
                                replyText += "Started a lerp of tremolo freq/depth from ${player.filterFactory.tremoloFrequencyParam.get()}/${player.filterFactory.tremoloDepthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                player.filterFactory.tremoloFrequencyParam.rampTo(param1, lerpTime)
                                player.filterFactory.tremoloDepthParam.rampTo(param2, lerpTime)
                            }
						}
						"v", "vibrato" -> {
                            if (lerpTime == 0.0f) {
                                player.filterFactory.vibratoFrequencyParam.set(param1)
                                player.filterFactory.vibratoDepthParam.set(param2)

                                val setFreq = player.filterFactory.vibratoFrequencyParam.get();
                                val setDepth = player.filterFactory.vibratoDepthParam.get();

                                if (setFreq == 0.0 || setDepth == 0.0)
                                {
                                    replyText += "Disabled vibrato\n"
                                }
                                else
                                {
                                    replyText += "Set vibrato freq/depth to ${setFreq}/${setDepth}\n"
                                }
                            } else {
                                replyText += "Started a lerp of vibrato freq/depth from ${player.filterFactory.vibratoFrequencyParam.get()}/${player.filterFactory.vibratoDepthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                player.filterFactory.vibratoFrequencyParam.rampTo(param1, lerpTime)
                                player.filterFactory.vibratoDepthParam.rampTo(param2, lerpTime)
                            }
						}
                        "reverb", "rev" -> {
                            if (lerpTime == 0.0f) {
                                player.filterFactory.reverbSizeParam.set(param1)
                                player.filterFactory.reverbStrengthParam.set(param2)

                                val setSize = player.filterFactory.reverbSizeParam.get();
                                val setStrength = player.filterFactory.reverbStrengthParam.get();

                                if (setSize == 0.0 || setStrength == 0.0)
                                {
                                    replyText += "Disabled reverb\n"
                                }
                                else
                                {
                                    replyText += "Set reverb size/strength to ${setSize}/${setStrength}\n"
                                }
                            } else {
                                replyText += "Started a lerp of reverb size/strength from ${player.filterFactory.reverbSizeParam.get()}/${player.filterFactory.reverbStrengthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                player.filterFactory.reverbSizeParam.rampTo(param1, lerpTime)
                                player.filterFactory.reverbStrengthParam.rampTo(param2, lerpTime)
                            }
                        }
                        "echo" -> {
                            if (lerpTime == 0.0f) {
                                player.filterFactory.echoDelayParam.set(param1)
                                player.filterFactory.echoDecayParam.set(param2)

                                val setDelay = player.filterFactory.echoDelayParam.get();
                                val setDecay = player.filterFactory.echoDecayParam.get();

                                if (setDelay == 0.0 || setDecay == 0.0)
                                {
                                    replyText += "Disabled echo\n"
                                }
                                else
                                {
                                    replyText += "Set echo delay/decay to ${setDelay}/${setDecay}\n"
                                }
                            } else {
                                replyText += "Started a lerp of echo delay/decay from ${player.filterFactory.echoDelayParam.get()}/${player.filterFactory.echoDecayParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                player.filterFactory.echoDelayParam.rampTo(param1, lerpTime)
                                player.filterFactory.echoDecayParam.rampTo(param2, lerpTime)
                            }
                        }
					}

                }
                matcher.group(6) != null -> {
                    val name = matcher.group(6)

                    when (name) {
                        "r", "reset" -> {
                            player.filterFactory.reset()

                            replyText += "Reset filters\n"
                        }
                        "list" ->
                        {
                            replyText += "Current filters:\n"
                            replyText += "\tspeed ${player.filterFactory.timescaleSpeedParam.get()}\n"
                            replyText += "\tpitch ${player.filterFactory.timescalePitchParam.get()}\n"
                            replyText += "\ttremolo ${player.filterFactory.tremoloFrequencyParam.get()} ${player.filterFactory.tremoloDepthParam.get()}\n"
                            replyText += "\tvibrato ${player.filterFactory.vibratoFrequencyParam.get()} ${player.filterFactory.vibratoDepthParam.get()}\n"
                            replyText += "\treverb ${player.filterFactory.reverbSizeParam.get()} ${player.filterFactory.reverbStrengthParam.get()} (${player.filterFactory.reverbLinesParam.get().toInt()} lines)\n"
                            replyText += "\techo ${player.filterFactory.echoDelayParam.get()} ${player.filterFactory.echoDecayParam.get()}\n"
                            replyText += "\tbassboost ${player.filterFactory.bassBoostParam.get()}\n"
                            replyText += "\tclip ${player.filterFactory.clipperThresholdParam.get()}\n"
                            replyText += "\trotation ${player.filterFactory.rotationParam.get()}\n"
                        }
                    }
                }
            }
        }

        if (replyText.isBlank())
        {
            replyHelp()
        }
        else
        {
            reply(replyText)
        }
    }

    // private val matchPattern: Pattern = Pattern.compile("(s|p|speed|pitch)\\s+([\\d.]+)|trem\\s+([\\d.]+)\\s+([\\d.]+)|(r|reset)")
    private val matchPattern: Pattern = Pattern.compile("(s|p|speed|pitch|bb|bassboost|c|clip|l|lerp|rot|rotation|reverbLines)\\s+([\\d.-]+)|(t|tremolo|v|vibrato|rev|reverb|echo)\\s+([\\d.-]+)\\s+([\\d.-]+)|(r|reset|list)")
    //\ns/speed X.XX\np/pitch X.XX\nt/tremolo X.XX (freq) X.XX (depth 0-1)\nbb/bassboost X.XX\nr/reset"
	override fun HelpContext.provideHelp() {
		addUsage("[filter]")
		addDescription("Adjusts the filters on the current track")
		addUsages("speed", "s <s>")
		addUsages("pitch", "p <p>")
		addUsages("tremolo", "t <freq> <depth>")
		addUsages("vibrato", "v <freq> <depth>")
        addUsages("echo <delay> <decay>")
        addUsages("reverb", "rev <size> <strength>")
        addUsages("reverbLines <numLines>")
		addUsages("bassboost", "bb <amount>")
		addUsages("rotation", "rot <hz>")
		addUsages("clip", "c <amount>")
        addUsages("lerp", "l")
		addUsages("list")
		addUsages("reset", "r")
		addDescription("Filters can be chained in any order separated by whitespace")
		addUsage("speed 0.5 pitch 3 vibrato 0.25 20")
        addUsage("speed 0.5 lerp 10 pitch 3 vibrato 0.25 20 // lerps only the values after the lerp")

    	// var asdf = ""
     // 	asdf += "\ns/speed X.XX"
     // 	asdf += "\np/pitch X.XX"
     // 	asdf += "\nt/tremolo X.XX (freq) X.XX (depth 0-1)"
     // 	asdf += "\nv/vibrato X.XX (freq) X.XX (depth 0-1)"
     // 	asdf += "\nbb/bassboost X.XX"
     //    asdf += "\nc/clip X.XX"
     // 	asdf += "\nr/reset"
        //addUsage("A whitespace-separated list of these in any order: " + asdf)
        //addDescription("Adjusts the filters on the current track")
    }
}
