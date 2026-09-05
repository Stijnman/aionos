package com.aionos.parser

import com.aionos.action.AgentAction
import com.aionos.security.ActionPolicy
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
                x = obj["x"]?.jsonPrimitive?.int ?: return null,
                y = obj["y"]?.jsonPrimitive?.int ?: return null,
                nodeText = obj["nodeText"]?.jsonPrimitive?.content
            )
            "long_press" -> AgentAction.LongPress(
                x = obj["x"]?.jsonPrimitive?.int ?: return null,
                y = obj["y"]?.jsonPrimitive?.int ?: return null,
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
                startX = obj["startX"]?.jsonPrimitive?.int ?: return null,
                startY = obj["startY"]?.jsonPrimitive?.int ?: return null,
                endX = obj["endX"]?.jsonPrimitive?.int ?: return null,
                endY = obj["endY"]?.jsonPrimitive?.int ?: return null
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
        val errors = actions.flatMap(ActionPolicy::validate)
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult()
    }
}
