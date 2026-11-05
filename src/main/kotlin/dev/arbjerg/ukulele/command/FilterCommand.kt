package dev.arbjerg.ukulele.command

import dev.arbjerg.ukulele.audio.SmoothedParam
import dev.arbjerg.ukulele.audio.globalFilterFactory
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

        val filterFactory = globalFilterFactory

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
                            setSingleParameter(filterFactory.timescaleSpeedParam, "Speed", value)
                        }
                        "p", "pitch" -> {
                            setSingleParameter(filterFactory.timescalePitchParam, "Pitch", value)
                        }
                        "bb", "bassboost" -> {
                            setSingleParameter(filterFactory.bassBoostParam, "bass boost", value)
                        }
                        "c", "clip" -> {
                            setSingleParameter(filterFactory.clipperThresholdParam, "clipper threshold", value)
                        }
                        "reverbLines" -> {
                            setSingleParameter(filterFactory.reverbLinesParam, "reverb lines", value)
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
                                filterFactory.tremoloFrequencyParam.set(param1)
                                filterFactory.tremoloDepthParam.set(param2)

                                val setFreq = filterFactory.tremoloFrequencyParam.get();
                                val setDepth = filterFactory.tremoloDepthParam.get();

                                if (setFreq == 0.0 || setDepth == 0.0)
                                {
                                    replyText += "Disabled tremolo\n"
                                }
                                else
                                {
                                    replyText += "Set tremolo freq/depth to ${setFreq}/${setDepth}\n"
                                }
                            } else {
                                replyText += "Started a lerp of tremolo freq/depth from ${filterFactory.tremoloFrequencyParam.get()}/${filterFactory.tremoloDepthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                filterFactory.tremoloFrequencyParam.rampTo(param1, lerpTime)
                                filterFactory.tremoloDepthParam.rampTo(param2, lerpTime)
                            }
						}
						"v", "vibrato" -> {
                            if (lerpTime == 0.0f) {
                                filterFactory.vibratoFrequencyParam.set(param1)
                                filterFactory.vibratoDepthParam.set(param2)

                                val setFreq = filterFactory.vibratoFrequencyParam.get();
                                val setDepth = filterFactory.vibratoDepthParam.get();

                                if (setFreq == 0.0 || setDepth == 0.0)
                                {
                                    replyText += "Disabled vibrato\n"
                                }
                                else
                                {
                                    replyText += "Set vibrato freq/depth to ${setFreq}/${setDepth}\n"
                                }
                            } else {
                                replyText += "Started a lerp of vibrato freq/depth from ${filterFactory.vibratoFrequencyParam.get()}/${filterFactory.vibratoDepthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                filterFactory.vibratoFrequencyParam.rampTo(param1, lerpTime)
                                filterFactory.vibratoDepthParam.rampTo(param2, lerpTime)
                            }
						}
                        "reverb", "rev" -> {
                            if (lerpTime == 0.0f) {
                                filterFactory.reverbSizeParam.set(param1)
                                filterFactory.reverbStrengthParam.set(param2)

                                val setSize = filterFactory.reverbSizeParam.get();
                                val setStrength = filterFactory.reverbStrengthParam.get();

                                if (setSize == 0.0 || setStrength == 0.0)
                                {
                                    replyText += "Disabled reverb\n"
                                }
                                else
                                {
                                    replyText += "Set reverb size/strength to ${setSize}/${setStrength}\n"
                                }
                            } else {
                                replyText += "Started a lerp of reverb size/strength from ${filterFactory.reverbSizeParam.get()}/${filterFactory.reverbStrengthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                filterFactory.reverbSizeParam.rampTo(param1, lerpTime)
                                filterFactory.reverbStrengthParam.rampTo(param2, lerpTime)
                            }
                        }
                        "echo" -> {
                            if (lerpTime == 0.0f) {
                                filterFactory.echoDelayParam.set(param1)
                                filterFactory.echoDecayParam.set(param2)

                                val setDelay = filterFactory.echoDelayParam.get();
                                val setDecay = filterFactory.echoDecayParam.get();

                                if (setDelay == 0.0 || setDecay == 0.0)
                                {
                                    replyText += "Disabled echo\n"
                                }
                                else
                                {
                                    replyText += "Set echo delay/decay to ${setDelay}/${setDecay}\n"
                                }
                            } else {
                                replyText += "Started a lerp of echo delay/decay from ${filterFactory.echoDelayParam.get()}/${filterFactory.echoDecayParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                filterFactory.echoDelayParam.rampTo(param1, lerpTime)
                                filterFactory.echoDecayParam.rampTo(param2, lerpTime)
                            }
                        }
                        "rot", "rotation" -> {
                            if (lerpTime == 0.0f) {
                                filterFactory.rotationParam.set(param1)
                                filterFactory.rotationDepthParam.set(param2)

                                val setRotation = filterFactory.rotationParam.get();
                                val setDepth = filterFactory.rotationDepthParam.get();

                                if (setRotation == 0.0 || setDepth == 0.0)
                                {
                                    replyText += "Disabled rotation\n"
                                }
                                else
                                {
                                    replyText += "Set rotation/depth to ${setRotation}/${setDepth}\n"
                                }
                            } else {
                                replyText += "Started a lerp of rotation/depth from ${filterFactory.rotationParam.get()}/${filterFactory.rotationDepthParam.get()} to $param1/$param2 over $lerpTime seconds\n"
                                filterFactory.rotationParam.rampTo(param1, lerpTime)
                                filterFactory.rotationDepthParam.rampTo(param2, lerpTime)
                            }
                        }
					}

                }
                matcher.group(6) != null -> {
                    val name = matcher.group(6)

                    when (name) {
                        "r", "reset" -> {
                            filterFactory.reset()

                            replyText += "Reset filters\n"
                        }
                        "list" ->
                        {
                            replyText += "Current filters:\n"
                            replyText += "\tspeed ${filterFactory.timescaleSpeedParam.get()}\n"
                            replyText += "\tpitch ${filterFactory.timescalePitchParam.get()}\n"
                            replyText += "\ttremolo ${filterFactory.tremoloFrequencyParam.get()} ${filterFactory.tremoloDepthParam.get()}\n"
                            replyText += "\tvibrato ${filterFactory.vibratoFrequencyParam.get()} ${filterFactory.vibratoDepthParam.get()}\n"
                            replyText += "\treverb ${filterFactory.reverbSizeParam.get()} ${filterFactory.reverbStrengthParam.get()} (${filterFactory.reverbLinesParam.get().toInt()} lines)\n"
                            replyText += "\techo ${filterFactory.echoDelayParam.get()} ${filterFactory.echoDecayParam.get()}\n"
                            replyText += "\tbassboost ${filterFactory.bassBoostParam.get()}\n"
                            replyText += "\tclip ${filterFactory.clipperThresholdParam.get()}\n"
                            replyText += "\trotation ${filterFactory.rotationParam.get()}/${filterFactory.rotationDepthParam.get()}\n"
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
    private val matchPattern: Pattern = Pattern.compile("(s|p|speed|pitch|bb|bassboost|c|clip|l|lerp|reverbLines)\\s+([\\d.-]+)|(t|tremolo|v|vibrato|rev|reverb|echo|rot|rotation)\\s+([\\d.-]+)\\s+([\\d.-]+)|^(r|reset|list)$")
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
		addUsages("rotation", "rot <hz> <depth>")
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
