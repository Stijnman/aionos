package com.aionos.parser

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AccessibilityTreeParserTest {

    private lateinit var parser: AccessibilityTreeParser

    @Before
    fun setup() {
        parser = AccessibilityTreeParser()
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "test-uuid-12345678"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `parse returns empty tree for null root`() {
        val result = parser.parse(null)
        assert(result == "<tree/>")
    }

    @Test
    fun `parse handles empty tree`() {
        val root = mockk<AccessibilityNodeInfo>()
        every { root.packageName } returns "com.test"
        every { root.isVisibleToUser } returns false
        every { root.childCount } returns 0
        
        val result = parser.parse(root)
        
        assert(result.contains("<tree package=\"com.test\""))
        assert(result.contains("</tree>"))
    }

    @Test
    fun `parse includes node with text`() {
        val root = mockk<AccessibilityNodeInfo>()
        every { root.packageName } returns "com.test"
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 0
        every { root.text } returns "Test Node"
        every { root.contentDescription } returns null
        every { root.className } returns "android.widget.TextView"
        every { root.isClickable } returns false
        every { root.isLongClickable } returns false
        every { root.isFocusable } returns false
        every { root.isEditable } returns false
        every { root.isPassword } returns false
        every { root.isScrollable } returns false
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 100, 50)
        }
        
        val result = parser.parse(root)
        
        assert(result.contains("text=\"Test Node\""))
        assert(result.contains("class=\"android.widget.TextView\""))
    }

    @Test
    fun `parse includes clickable attribute`() {
        val root = mockk<AccessibilityNodeInfo>()
        every { root.packageName } returns "com.test"
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 0
        every { root.text } returns null
        every { root.contentDescription } returns null
        every { root.className } returns "android.widget.Button"
        every { root.isClickable } returns true
        every { root.isLongClickable } returns false
        every { root.isFocusable } returns false
        every { root.isEditable } returns false
        every { root.isPassword } returns false
        every { root.isScrollable } returns false
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 100, 50)
        }
        
        val result = parser.parse(root)
        
        assert(result.contains("clickable=\"true\""))
    }

    @Test
    fun `parse handles nested nodes`() {
        val child = mockk<AccessibilityNodeInfo>()
        every { child.isVisibleToUser } returns true
        every { child.childCount } returns 0
        every { child.text } returns "Child Text"
        every { child.contentDescription } returns null
        every { child.className } returns "android.widget.TextView"
        every { child.isClickable } returns false
        every { child.isLongClickable } returns false
        every { child.isFocusable } returns false
        every { child.isEditable } returns false
        every { child.isPassword } returns false
        every { child.isScrollable } returns false
        every { child.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(10, 10, 110, 60)
        }
        
        val root = mockk<AccessibilityNodeInfo>()
        every { root.packageName } returns "com.test"
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 1
        every { root.getChild(0) } returns child
        every { root.text } returns null
        every { root.contentDescription } returns null
        every { root.className } returns "android.widget.FrameLayout"
        every { root.isClickable } returns false
        every { root.isLongClickable } returns false
        every { root.isFocusable } returns false
        every { root.isEditable } returns false
        every { root.isPassword } returns false
        every { root.isScrollable } returns false
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 200, 200)
        }
        
        val result = parser.parse(root)
        
        assert(result.contains("<tree package=\"com.test\""))
        assert(result.contains("<node"))
        assert(result.contains("Child Text"))
    }

    @Test
    fun `parse escapes XML special characters`() {
        val root = mockk<AccessibilityNodeInfo>()
        every { root.packageName } returns "com.test"
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 0
        every { root.text } returns "Test & <value>"
        every { root.contentDescription } returns null
        every { root.className } returns "android.widget.TextView"
        every { root.isClickable } returns false
        every { root.isLongClickable } returns false
        every { root.isFocusable } returns false
        every { root.isEditable } returns false
        every { root.isPassword } returns false
        every { root.isScrollable } returns false
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 100, 50)
        }
        
        val result = parser.parse(root)
        
        assert(result.contains("&amp;"))
        assert(result.contains("&lt;"))
    }

    @Test
    fun `findNodeByText finds matching node`() {
        val matchingChild = mockk<AccessibilityNodeInfo>()
        every { matchingChild.isVisibleToUser } returns true
        every { matchingChild.childCount } returns 0
        every { matchingChild.text } returns "Target Text"
        every { matchingChild.contentDescription } returns null
        
        val nonMatchingChild = mockk<AccessibilityNodeInfo>()
        every { nonMatchingChild.isVisibleToUser } returns true
        every { nonMatchingChild.childCount } returns 0
        every { nonMatchingChild.text } returns "Other Text"
        every { nonMatchingChild.contentDescription } returns null
        
        val root = mockk<AccessibilityNodeInfo>()
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 2
        every { root.getChild(0) } returns matchingChild
        every { root.getChild(1) } returns nonMatchingChild
        every { root.text } returns null
        every { root.contentDescription } returns null
        
        val result = parser.findNodeByText(root, "Target")
        
        assert(result == matchingChild)
    }

    @Test
    fun `findNodeByText returns null when not found`() {
        val child = mockk<AccessibilityNodeInfo>()
        every { child.isVisibleToUser } returns true
        every { child.childCount } returns 0
        every { child.text } returns "Other Text"
        every { child.contentDescription } returns null
        
        val root = mockk<AccessibilityNodeInfo>()
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 1
        every { root.getChild(0) } returns child
        every { root.text } returns null
        every { root.contentDescription } returns null
        
        val result = parser.findNodeByText(root, "Not Found")
        
        assert(result == null)
    }

    @Test
    fun `findNodeAtPoint finds node containing point`() {
        val child = mockk<AccessibilityNodeInfo>()
        every { child.isVisibleToUser } returns true
        every { child.childCount } returns 0
        every { child.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(50, 50, 150, 100) // Contains point (100, 75)
        }
        
        val root = mockk<AccessibilityNodeInfo>()
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 1
        every { root.getChild(0) } returns child
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 200, 200)
        }
        
        val result = parser.findNodeAtPoint(root, 100, 75)
        
        assert(result == child)
    }

    @Test
    fun `findNodeAtPoint returns null when no node contains point`() {
        val child = mockk<AccessibilityNodeInfo>()
        every { child.isVisibleToUser } returns true
        every { child.childCount } returns 0
        every { child.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 100, 100) // Does not contain (200, 200)
        }
        
        val root = mockk<AccessibilityNodeInfo>()
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 1
        every { root.getChild(0) } returns child
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 200, 200)
        }
        
        val result = parser.findNodeAtPoint(root, 200, 200)
        
        assert(result == null)
    }

    @Test
    fun `findNodeAtPoint finds smallest node containing point`() {
        val smallChild = mockk<AccessibilityNodeInfo>()
        every { smallChild.isVisibleToUser } returns true
        every { smallChild.childCount } returns 0
        every { smallChild.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(50, 50, 100, 100) // Area = 2500
        }
        
        val largeChild = mockk<AccessibilityNodeInfo>()
        every { largeChild.isVisibleToUser } returns true
        every { largeChild.childCount } returns 0
        every { largeChild.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 200, 200) // Area = 40000
        }
        
        val root = mockk<AccessibilityNodeInfo>()
        every { root.isVisibleToUser } returns true
        every { root.childCount } returns 2
        every { root.getChild(0) } returns smallChild
        every { root.getChild(1) } returns largeChild
        every { root.getBoundsInScreen(any()) } answers {
            val rect = it.arg<Rect>(0)
            rect.set(0, 0, 300, 300)
        }
        
        val result = parser.findNodeAtPoint(root, 75, 75)
        
        // Should return the smaller node that contains the point
        assert(result == smallChild)
    }
}
