package com.amk.app

import com.amk.app.hid.HidConstants
import com.amk.app.hid.KeycodeMapper
import com.amk.app.model.AppSettings
import com.amk.app.model.ClickMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUnitTest {

    @Test
    fun testKeycodeMapper() {
        val mappingA = KeycodeMapper.charToHid('a')
        assertNotNull(mappingA)
        assertEquals(HidConstants.MOD_NONE, mappingA?.first)
        assertEquals(0x04.toByte(), mappingA?.second)

        val mappingCapitalA = KeycodeMapper.charToHid('A')
        assertNotNull(mappingCapitalA)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, mappingCapitalA?.first)
        assertEquals(0x04.toByte(), mappingCapitalA?.second)

        val mappingEnter = KeycodeMapper.charToHid('\n')
        assertNotNull(mappingEnter)
        assertEquals(HidConstants.KEY_ENTER, mappingEnter?.second)

        val mappingSpace = KeycodeMapper.charToHid(' ')
        assertNotNull(mappingSpace)
        assertEquals(HidConstants.KEY_SPACE, mappingSpace?.second)
    }

    @Test
    fun testAppSettingsJsonSerialization() {
        val settings = AppSettings(
            clickMode = ClickMode.BUTTONS_ONLY,
            pointerSpeed = 2.0f,
            accelerationEnabled = false,
            invertScroll = true,
            hapticEnabled = false,
            includeNightly = true,
            lastConnectedDeviceAddress = "6C:0D:C4:05:FA:93",
            lastConnectedDeviceName = "MIBOX4"
        )

        val json = settings.toJsonString()
        val restored = AppSettings.fromJsonString(json)

        assertEquals(ClickMode.BUTTONS_ONLY, restored.clickMode)
        assertEquals(2.0f, restored.pointerSpeed, 0.01f)
        assertEquals(false, restored.accelerationEnabled)
        assertEquals(true, restored.invertScroll)
        assertEquals(false, restored.hapticEnabled)
        assertEquals(true, restored.includeNightly)
        assertEquals("6C:0D:C4:05:FA:93", restored.lastConnectedDeviceAddress)
        assertEquals("MIBOX4", restored.lastConnectedDeviceName)
    }

    @Test
    fun testHidDescriptorIntegrity() {
        val descriptor = HidConstants.HID_REPORT_DESCRIPTOR
        assertTrue("Descriptor should not be empty", descriptor.isNotEmpty())
        assertTrue("Descriptor should contain report ID 1 (mouse)", descriptor.contains(HidConstants.REPORT_ID_MOUSE))
        assertTrue("Descriptor should contain report ID 2 (keyboard)", descriptor.contains(HidConstants.REPORT_ID_KEYBOARD))
        assertTrue("Descriptor should contain report ID 3 (consumer)", descriptor.contains(HidConstants.REPORT_ID_CONSUMER))
    }
}
