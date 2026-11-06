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

	fun	formatDouble(num: Double) : String {
		return "%.3f".format(num)
	}

	override suspend fun CommandContext.invoke() {
		if (argumentText.isBlank())
			return replyHelp()

		val filterFactory = globalFilterFactory

		val matcher = matchPattern.matcher(argumentText)

		var replyText = ""

		var lerpTime: Float = 0.0f

		fun setSingleParameter(param: SmoothedParam, paramName: String, value: Double) {
			if (lerpTime == 0.0f) {
				param.set(value)
				replyText += "Set ${paramName.lowercase()} to $value\n"
			} else {
				// replyText += "Started a lerp of $paramName from ${formatDouble(param.get())} to $value over $lerpTime seconds\n"
				// param.rampTo(value, lerpTime)
				// replyText += "Started a $lerpTime second lerp!\n"
				replyText += "$paramName: ${formatDouble(param.get())} -> $value\n"
				param.rampTo(value, lerpTime)
			}
		}

		while (matcher.find()) {
			if (lerpTime > 0.0f && replyText == "") {
				replyText += "Started a $lerpTime second lerp!\n"
			}
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
							setSingleParameter(filterFactory.bassBoostParam, "Bass Boost", value)
						}
						"reverbLines" -> {
							setSingleParameter(filterFactory.reverbLinesParam, "Reverb Lines", value)
						}
						"l", "lerp" -> {
							lerpTime = value.toFloat()
						}
					}
				}
				matcher.group(3) != null -> {
					var param1: Double? = null
					var param2: Double? = null

					param1 = matcher.group(4).toDouble()
					if (matcher.group(5) != null) {
						param2 = matcher.group(5).toDouble()
					}


					val name = matcher.group(3)
					when (name) {
						"t", "tremolo" -> {
							var targetParam1 = filterFactory.tremoloFrequencyParam
							var targetParam2 = filterFactory.tremoloDepthParam
							param2 = if (param2 == null) targetParam2.get() else param2
							if (lerpTime > 0.0f) {
								var lp1 = targetParam1.get()
								var lp2 = targetParam2.get()
								replyText += "--- Tremolo ---\n"
								replyText += "\tFrequency: ${formatDouble(lp1)} -> ${formatDouble(param1)}\n"
								replyText += "\tDepth: ${formatDouble(lp2)} -> ${formatDouble(param2)}\n"
								targetParam1.rampTo(param1, lerpTime)
								targetParam2.rampTo(param2, lerpTime)
							} else {
								targetParam1.set(param1)
								targetParam2.set(param2)
								replyText += if (param1 == 0.0 || param2 == 0.0) "Disabled tremolo\n" else "Set tremolo freq/depth to ${param1}/${param2}\n"
							}
						}
						"v", "vibrato" -> {
							var targetParam1 = filterFactory.vibratoFrequencyParam
							var targetParam2 = filterFactory.vibratoDepthParam
							param2 = if (param2 == null) targetParam2.get() else param2
							if (lerpTime > 0.0f) {
								var lp1 = targetParam1.get()
								var lp2 = targetParam2.get()
								replyText += "--- Vibrato ---\n"
								replyText += "\tFrequency: ${formatDouble(lp1)} -> ${formatDouble(param1)}\n"
								replyText += "\tDepth: ${formatDouble(lp2)} -> ${formatDouble(param2)}\n"
								targetParam1.rampTo(param1, lerpTime)
								targetParam2.rampTo(param2, lerpTime)
							} else {
								targetParam1.set(param1)
								targetParam2.set(param2)
								replyText += if (param1 == 0.0 || param2 == 0.0) "Disabled vibrato\n" else "Set vibrato freq/depth to ${param1}/${param2}\n"
							}
						}
						"reverb", "rev" -> {
							var targetParam1 = filterFactory.reverbSizeParam
							var targetParam2 = filterFactory.reverbStrengthParam
							param2 = if (param2 == null) targetParam2.get() else param2
							if (lerpTime > 0.0f) {
								var lp1 = targetParam1.get()
								var lp2 = targetParam2.get()
								replyText += "--- Reverb ---\n"
								replyText += "\tSize: ${formatDouble(lp1)} -> ${formatDouble(param1)}\n"
								replyText += "\tStrength: ${formatDouble(lp2)} -> ${formatDouble(param2)}\n"
								targetParam1.rampTo(param1, lerpTime)
								targetParam2.rampTo(param2, lerpTime)
							} else {
								targetParam1.set(param1)
								targetParam2.set(param2)
								replyText += if (param1 == 0.0 || param2 == 0.0) "Disabled reverb\n" else "Set reverb size/strength to ${param1}/${param2}\n"
							}
						}
						"echo" -> {
							var targetParam1 = filterFactory.echoDelayParam
							var targetParam2 = filterFactory.echoDecayParam
							param2 = if (param2 == null) targetParam2.get() else param2
							if (lerpTime > 0.0f) {
								var lp1 = targetParam1.get()
								var lp2 = targetParam2.get()
								replyText += "--- Echo ---\n"
								replyText += "\tDelay: ${formatDouble(lp1)} -> ${formatDouble(param1)}\n"
								replyText += "\tDecay: ${formatDouble(lp2)} -> ${formatDouble(param2)}\n"
								targetParam1.rampTo(param1, lerpTime)
								targetParam2.rampTo(param2, lerpTime)
							} else {
								targetParam1.set(param1)
								targetParam2.set(param2)
								replyText += if (param1 == 0.0 || param2 == 0.0) "Disabled echo\n" else "Set echo delay/decay to ${param1}/${param2}\n"
							}
						}
						"rot", "rotation" -> {
							var targetParam1 = filterFactory.rotationParam
							var targetParam2 = filterFactory.rotationDepthParam
							param2 = if (param2 == null) targetParam2.get() else param2
							if (lerpTime > 0.0f) {
								var lp1 = targetParam1.get()
								var lp2 = targetParam2.get()
								replyText += "--- Rotation ---\n"
								replyText += "\tFrequency: ${formatDouble(lp1)} -> ${formatDouble(param1)}\n"
								replyText += "\tDepth: ${formatDouble(lp2)} -> ${formatDouble(param2)}\n"
								targetParam1.rampTo(param1, lerpTime)
								targetParam2.rampTo(param2, lerpTime)
							} else {
								targetParam1.set(param1)
								targetParam2.set(param2)
								replyText += if (param1 == 0.0 || param2 == 0.0) "Disabled rotation\n" else "Set rotation/depth to ${param1}/${param2}\n"
							}
						}
						"c", "clip" -> {
							var targetParam1 = filterFactory.clipperThresholdParam
							var targetParam2 = filterFactory.clipperGainParam
							param2 = if (param2 == null) targetParam2.get() else param2
							if (lerpTime > 0.0f) {
								var lp1 = targetParam1.get()
								var lp2 = targetParam2.get()
								replyText += "--- Clip ---\n"
								replyText += "\tThreshold: ${formatDouble(lp1)} -> ${formatDouble(param1)}\n"
								replyText += "\tGain: ${formatDouble(lp2)} -> ${formatDouble(param2)}\n"
								targetParam1.rampTo(param1, lerpTime)
								targetParam2.rampTo(param2, lerpTime)
							} else {
								targetParam1.set(param1)
								targetParam2.set(param2)
								replyText += if (param1 == 0.0 || param2 == 0.0) "Disabled clipper\n" else "Set clipper threshold/gain to ${param1}/${param2}\n"
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
							replyText += "--- Current Filters ---\n"
							replyText += "\tSpeed: ${formatDouble(filterFactory.timescaleSpeedParam.get())}\n"
							replyText += "\tPitch: ${formatDouble(filterFactory.timescalePitchParam.get())}\n"
							replyText += "\tTremolo: ${formatDouble(filterFactory.tremoloFrequencyParam.get())} ${formatDouble(filterFactory.tremoloDepthParam.get())}\n"
							replyText += "\tVibrato: ${formatDouble(filterFactory.vibratoFrequencyParam.get())} ${formatDouble(filterFactory.vibratoDepthParam.get())}\n"
							replyText += "\tReverb: ${formatDouble(filterFactory.reverbSizeParam.get())} ${formatDouble(filterFactory.reverbStrengthParam.get())} (${filterFactory.reverbLinesParam.get().toInt()} lines)\n"
							replyText += "\tEcho: ${formatDouble(filterFactory.echoDelayParam.get())} ${formatDouble(filterFactory.echoDecayParam.get())}\n"
							replyText += "\tBass Boost: ${formatDouble(filterFactory.bassBoostParam.get())}\n"
							replyText += "\tClip: ${formatDouble(filterFactory.clipperThresholdParam.get())}\n"
							replyText += "\tRotation: ${formatDouble(filterFactory.rotationParam.get())}/${formatDouble(filterFactory.rotationDepthParam.get())}\n"
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
	private val matchPattern: Pattern = Pattern.compile("""(s|p|speed|pitch|bb|bassboost|l|lerp|reverbLines)\s+([\d.-]+)|(t|tremolo|v|vibrato|rev|reverb|echo|rot|rotation|c|clip)\s+([\d.-]+)(?:\s+([\d.-]+))?|^(r|reset|list)$""")
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
		addUsages("clip", "c <amount> <gain>")
		addUsages("lerp", "l")
		addUsages("list")
		addUsages("reset", "r")
		addDescription("Filters can be chained in any order separated by whitespace")
		addUsage("speed 0.5 pitch 3 vibrato 0.25 20")
		addUsage("speed 0.5 lerp 10 pitch 3 vibrato 0.25 20 // lerps only the values after the lerp")
		addDescription("If any additional arguments are omitted after the first one, they will use the previously stored value")
		addUsage("clip 0.7 // will only change the <amount> variable and keep the previous <gain> value")

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
