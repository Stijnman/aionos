package com.aionos.parser

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.UUID

/**
 * Parses Android AccessibilityNodeInfo tree into LLM-friendly structured text.
 * Adapted from mobile-use + Droidrun concepts.
 *
 * CRITICAL: Caller manages node lifecycle. Do not recycle nodes from root tree.
 */
class AccessibilityTreeParser {

    fun parse(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "<tree/>"
        val sb = StringBuilder()
        sb.append("<tree package=\"${rootNode.packageName}\">\n")
        parseNode(rootNode, sb, 1)
        sb.append("</tree>")
        return sb.toString()
    }

    private fun parseNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (!node.isVisibleToUser) return

        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val text = (node.text ?: node.contentDescription ?: "").toString().trim()
        val isInteractive = node.isClickable || node.isLongClickable || node.isFocusable || node.isEditable
        val hasText = text.isNotBlank()

        if (!isInteractive && !hasText && node.childCount > 0) {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { parseNode(it, sb, depth) }
            }
            return
        }

        val indent = "  ".repeat(depth)
        val nodeId = UUID.randomUUID().toString().take(8)

        sb.append("$indent<node")
        sb.append(" id=\"$nodeId\"")
        sb.append(" class=\"${node.className ?: "unknown"}\"")
        if (hasText) sb.append(" text=\"${text.escapeXml()}\"")
        if (node.isClickable) sb.append(" clickable=\"true\"")
        if (node.isLongClickable) sb.append(" longClickable=\"true\"")
        if (node.isEditable) sb.append(" editable=\"true\"")
        if (node.isPassword) sb.append(" password=\"true\"")
        if (node.isScrollable) sb.append(" scrollable=\"true\"")
        sb.append(" bounds=\"[${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}]\"")

        if (node.childCount == 0) {
            sb.append("/>\n")
        } else {
            sb.append(">\n")
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { parseNode(it, sb, depth + 1) }
            }
            sb.append("$indent</node>\n")
        }
    }

    fun findNodeByText(rootNode: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        val lowerQuery = query.lowercase()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text ?: node.contentDescription ?: "").toString().lowercase()
            if (text.contains(lowerQuery) && node.isVisibleToUser) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    fun findNodeAtPoint(rootNode: AccessibilityNodeInfo?, x: Int, y: Int): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        val bounds = Rect()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        var bestMatch: AccessibilityNodeInfo? = null
        var bestArea = Int.MAX_VALUE
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            node.getBoundsInScreen(bounds)
            if (bounds.contains(x, y) && node.isVisibleToUser) {
                val area = bounds.width() * bounds.height()
                if (area < bestArea) {
                    bestArea = area
                    bestMatch = node
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return bestMatch
    }

    private fun String.escapeXml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
