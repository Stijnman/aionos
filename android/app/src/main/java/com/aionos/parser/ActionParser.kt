package com.aionos.parser

import com.aionos.action.AgentAction
import kotlinx.serialization.json.*

class ActionParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): List<AgentAction> {
        return try {
            val array = json.parseToJsonElement(jsonString.trim()).jsonArray
            array.mapNotNull { parseSingleAction(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseSingleAction(obj: JsonObject): AgentAction? {
        val actionType = obj["action"]?.jsonPrimitive?.content ?: return null
        return when (actionType.lowercase()) {
            "tap" -> AgentAction.Tap(
                x = obj["x"]?.jsonPrimitive?.int ?: 0,
                y = obj["y"]?.jsonPrimitive?.int ?: 0,
                nodeText = obj["nodeText"]?.jsonPrimitive?.content
            )
            "long_press" -> AgentAction.LongPress(
                x = obj["x"]?.jsonPrimitive?.int ?: 0,
                y = obj["y"]?.jsonPrimitive?.int ?: 0,
                nodeText = obj["nodeText"]?.jsonPrimitive?.content
            )
            "type" -> AgentAction.Type(
                text = obj["text"]?.jsonPrimitive?.content ?: "",
                isPasswordField = obj["isPasswordField"]?.jsonPrimitive?.boolean ?: false,
                nodeText = obj["nodeText"]?.jsonPrimitive?.content
            )
            "scroll" -> AgentAction.Scroll(
                direction = parseDirection(obj["direction"]?.jsonPrimitive?.content),
                amount = obj["amount"]?.jsonPrimitive?.int ?: 500
            )
            "swipe" -> AgentAction.Swipe(
                startX = obj["startX"]?.jsonPrimitive?.int ?: 0,
                startY = obj["startY"]?.jsonPrimitive?.int ?: 0,
                endX = obj["endX"]?.jsonPrimitive?.int ?: 0,
                endY = obj["endY"]?.jsonPrimitive?.int ?: 0
            )
            "open_app" -> AgentAction.OpenApp(
                packageName = obj["packageName"]?.jsonPrimitive?.content ?: "",
                activityName = obj["activityName"]?.jsonPrimitive?.content
            )
            "press_key" -> {
                val keyName = obj["key"]?.jsonPrimitive?.content ?: "BACK"
                val globalAction = try {
                    AgentAction.GlobalAction.valueOf(keyName.uppercase())
                } catch (_: IllegalArgumentException) {
                    AgentAction.GlobalAction.BACK
                }
                AgentAction.PressKey(globalAction)
            }
            "read_text" -> AgentAction.ReadText()
            "wait" -> AgentAction.Wait(millis = obj["millis"]?.jsonPrimitive?.long ?: 1000)
            else -> null
        }
    }

    private fun parseDirection(dir: String?): AgentAction.Direction {
        return when (dir?.lowercase()) {
            "up" -> AgentAction.Direction.UP
            "down" -> AgentAction.Direction.DOWN
            "left" -> AgentAction.Direction.LEFT
            "right" -> AgentAction.Direction.RIGHT
            else -> AgentAction.Direction.DOWN
        }
    }

    fun validate(actions: List<AgentAction>): ValidationResult {
        val errors = mutableListOf<String>()
        for (action in actions) {
            when {
                action.safetyTier == AgentAction.SafetyTier.TIER_4 ->
                    errors.add("Blocked TIER_4 action: ${action.javaClass.simpleName}")
                action is AgentAction.Tap && (action.x < 0 || action.y < 0) ->
                    errors.add("Invalid tap coordinates: (${action.x}, ${action.y})")
                action is AgentAction.Type && action.text.length > 1000 ->
                    errors.add("Text input too long: ${action.text.length} chars")
                action is AgentAction.OpenApp && action.packageName.isBlank() ->
                    errors.add("OpenApp action missing package name")
            }
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult()
    }
}
