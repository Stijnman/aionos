package com.aionos

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @Test
    fun packageIdentityIsStable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aionos", context.packageName)
    }
}
