package dev.arbjerg.ukulele.command

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

        while (matcher.find()) {
            when {
                matcher.group(1) != null -> {
                    val name = matcher.group(1)
                    val value = matcher.group(2).toDouble()
                    when (name) {
                        "s", "speed" -> {
                            player.filterFactory.speed = value
                            replyText += "Set speed to $value\n"
                        }
                        "p", "pitch" -> {
                            player.filterFactory.pitch = value
                            replyText += "Set pitch to $value\n"
                        }
                        "bb", "bassboost" -> {
                        	player.filterFactory.bassBoost = value.toFloat()
                         	replyText += "Set bass boost to $value\n"
                        }
                        "c", "clip" -> {
                            player.filterFactory.clipperThreshold = value.toFloat()
                            replyText += "Set clipper to $value\n"
                        }
                    }
                }
                matcher.group(3) != null -> {
                    val freq = matcher.group(4).toFloat()
                    val depth = matcher.group(5).toFloat()

					val name = matcher.group(3)
					when (name) {
						"t", "tremolo" -> {
		                    player.filterFactory.tremoloFrequency = freq
		                    player.filterFactory.tremoloDepth = depth.coerceAtLeast(0.0f)

		                    if (depth == 0.0f || freq == 0.0f) {
		                        replyText += "Disabled tremolo\n"
		                    }
		                    else {
		                        replyText += "Set tremolo freq/depth to $freq/$depth\n"
		                    }
						}
						"v", "vibrato" -> {
						   player.filterFactory.vibratoFrequency = freq
					       player.filterFactory.vibratoDepth = depth.coerceAtLeast(0.0f)

					       if (depth == 0.0f || freq == 0.0f) {
					           replyText += "Disabled vibrato\n"
					       }
					       else {
					           replyText += "Set vibrato freq/depth to $freq/$depth\n"
					       }
						}
					}

                }
                matcher.group(6) != null -> {
                    val name = matcher.group(6)

                    when (name) {
                        "r", "reset" -> {
                            player.filterFactory.speed = 1.0
                            player.filterFactory.pitch = 0.0
                            player.filterFactory.tremoloFrequency = 0.0f
                            player.filterFactory.tremoloDepth = 0.0f
                            player.filterFactory.vibratoFrequency = 0.0f
                            player.filterFactory.vibratoDepth = 0.0f
                            player.filterFactory.bassBoost = 0.0f
                            player.filterFactory.clipperThreshold = 1.0f

                            replyText += "Reset filters\n"
                        }
                        "l", "list" ->
                        {
                            replyText += "Current filters:\n"
                            replyText += "\ts/speed ${player.filterFactory.speed}\n"
                            replyText += "\tp/pitch ${player.filterFactory.pitch}\n"
                            replyText += "\tt/tremolo ${player.filterFactory.tremoloFrequency} ${player.filterFactory.tremoloDepth}\n"
                            replyText += "\tv/vibrato ${player.filterFactory.vibratoFrequency} ${player.filterFactory.vibratoDepth}\n"
                            replyText += "\tbb/bassboost ${player.filterFactory.bassBoost}\n"
                            replyText += "\tc/clip ${player.filterFactory.clipperThreshold}\n"
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
    private val matchPattern: Pattern = Pattern.compile("(s|p|speed|pitch|bb|bassboost|c|clip)\\s+([\\d.-]+)|(t|tremolo|v|vibrato)\\s+([\\d.-]+)\\s+([\\d.-]+)|(r|reset|l|list)")
    //\ns/speed X.XX\np/pitch X.XX\nt/tremolo X.XX (freq) X.XX (depth 0-1)\nbb/bassboost X.XX\nr/reset"
	override fun HelpContext.provideHelp() {
		addUsage("[filter]")
		addDescription("Adjusts the filters on the current track")
		addUsages("speed", "s <s>")
		addUsages("pitch", "p <p>")
		addUsages("tremolo", "t <freq> <depth>")
		addUsages("vibrato", "v <freq> <depth>")
		addUsages("bassboost", "bb <amount>")
		addUsages("clip", "c <amount>")
		addUsages("list", "l")
		addUsages("reset", "r")
		addDescription("Filters can be chained in any order separated by whitespace")
		addUsage("speed 0.5 pitch 3 vibrato 0.25 20")

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
