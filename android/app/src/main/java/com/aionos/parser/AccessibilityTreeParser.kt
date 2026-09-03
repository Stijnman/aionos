package com.aionos.parser

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.UUID

/**
 * Parses Android AccessibilityNodeInfo tree into LLM-friendly structured text.
 * Adapted from mobile-use + Droidrun concepts.
 *
 * Uses a stack-based approach to avoid deep recursion and properly recycle nodes.
 */
class AccessibilityTreeParser {

    fun parse(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "<tree/>"
        val sb = StringBuilder()
        sb.append("<tree package=\"${rootNode.packageName}\">\n")
        parseNodeIterative(rootNode, sb)
        sb.append("</tree>")
        return sb.toString()
    }

    private fun parseNodeIterative(root: AccessibilityNodeInfo, sb: StringBuilder) {
        val stack = mutableListOf<NodeContext>()
        stack.add(NodeContext(root, 1, false))
        
        while (stack.isNotEmpty()) {
            val ctx = stack.removeAt(stack.size - 1)
            val node = ctx.node
            
            if (!node.isVisibleToUser) {
                node.recycle()
                continue
            }

            val bounds = Rect().also { node.getBoundsInScreen(it) }
            val text = (node.text ?: node.contentDescription ?: "").toString().trim()
            val isInteractive = node.isClickable || node.isLongClickable || 
                    node.isFocusable || node.isEditable
            val hasText = text.isNotBlank()

            if (!isInteractive && !hasText && node.childCount > 0) {
                // Push children first, then the node for closing
                for (i in node.childCount - 1 downTo 0) {
                    node.getChild(i)?.let { child ->
                        stack.add(NodeContext(child, ctx.depth + 1, false))
                    }
                }
                stack.add(ctx.copy(isClosing = true))
                continue
            }

            val indent = "  ".repeat(ctx.depth)
            val nodeId = UUID.randomUUID().toString().take(8)

            if (!ctx.isClosing) {
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
                    node.recycle()
                } else {
                    sb.append(">\n")
                    // Push closing tag
                    stack.add(ctx.copy(isClosing = true))
                    // Push children in reverse order
                    for (i in node.childCount - 1 downTo 0) {
                        node.getChild(i)?.let { child ->
                            stack.add(NodeContext(child, ctx.depth + 1, false))
                        }
                    }
                }
            } else {
                sb.append("$indent</node>\n")
                node.recycle()
            }
        }
    }

    private data class NodeContext(
        val node: AccessibilityNodeInfo,
        val depth: Int,
        val isClosing: Boolean
    )

    fun findNodeByText(rootNode: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        val lowerQuery = query.lowercase()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        var result: AccessibilityNodeInfo? = null
        try {
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                try {
                    val text = (node.text ?: node.contentDescription ?: "").toString().lowercase()
                    if (text.contains(lowerQuery) && node.isVisibleToUser) {
                        result = node
                        return result
                    }
                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let { queue.add(it) }
                    }
                } finally {
                    if (node !== rootNode) node.recycle()
                }
            }
        } finally {
            // Recycle all remaining nodes in queue except root
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node !== rootNode) node.recycle()
            }
        }
        return result
    }

    fun findNodeAtPoint(rootNode: AccessibilityNodeInfo?, x: Int, y: Int): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        val bounds = Rect()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        var bestMatch: AccessibilityNodeInfo? = null
        var bestArea = Int.MAX_VALUE
        try {
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                try {
                    node.getBoundsInScreen(bounds)
                    if (bounds.contains(x, y) && node.isVisibleToUser) {
                        val area = bounds.width() * bounds.height()
                        if (area < bestArea) {
                            bestArea = area
                            // Recycle previous best match if it exists and isn't root
                            bestMatch?.let { if (it !== rootNode) it.recycle() }
                            bestMatch = node
                        } else {
                            // Not the best match, recycle this node
                            if (node !== rootNode) node.recycle()
                        }
                    } else if (node !== rootNode) {
                        node.recycle()
                    }
                    for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
                } catch (e: Exception) {
                    if (node !== rootNode) node.recycle()
                }
            }
        } finally {
            // Recycle all remaining nodes in queue except root and bestMatch
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node !== rootNode && node !== bestMatch) node.recycle()
            }
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
