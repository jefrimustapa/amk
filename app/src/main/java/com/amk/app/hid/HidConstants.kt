package com.amk.app.hid

object HidConstants {
    const val REPORT_ID_MOUSE: Byte = 1
    const val REPORT_ID_KEYBOARD: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

    // Composite USB HID Report Descriptor
    // Combines standard 3-button mouse with wheel, 6-key rollover keyboard, and consumer remote
    val HID_REPORT_DESCRIPTOR = byteArrayOf(
        // -------------------------------------------------------------
        // Report ID 1: Mouse (3 Buttons + Relative X/Y + Wheel)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_MOUSE, //   REPORT_ID (1)
        0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
        // Buttons (3 buttons: Left, Right, Middle)
        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
        0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3)
        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        // 5-bit padding
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x75.toByte(), 0x05.toByte(), //     REPORT_SIZE (5)
        0x81.toByte(), 0x03.toByte(), //     INPUT (Cnst,Var,Abs)
        // Movement (X, Y: -127 to 127)
        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
        // Vertical Wheel (-127 to 127)
        0x09.toByte(), 0x38.toByte(), //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
        0xC0.toByte(),                 //   END_COLLECTION
        0xC0.toByte(),                 // END_COLLECTION

        // -------------------------------------------------------------
        // Report ID 2: Keyboard (Modifiers + 6-Key Rollover)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD, // REPORT_ID (2)
        // Modifiers (Left/Right Ctrl, Shift, Alt, GUI)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)
        // Reserved byte
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x81.toByte(), 0x03.toByte(), //   INPUT (Cnst,Var,Abs)
        // Keycodes (6 bytes)
        0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x65.toByte(), //   LOGICAL_MAXIMUM (101)
        0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0x65.toByte(), //   USAGE_MAXIMUM (Keyboard Application)
        0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs)
        0xC0.toByte(),                 // END_COLLECTION

        // -------------------------------------------------------------
        // Report ID 3: Consumer Control (TV Remote, D-Pad, Media, Volume)
        // -------------------------------------------------------------
        0x05.toByte(), 0x0C.toByte(), // USAGE_PAGE (Consumer Devices)
        0x09.toByte(), 0x01.toByte(), // USAGE (Consumer Control)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_CONSUMER, // REPORT_ID (3)
        0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (0x03FF)
        0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (0x03FF)
        0x75.toByte(), 0x10.toByte(), //   REPORT_SIZE (16)
        0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
        0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs)
        0xC0.toByte()                  // END_COLLECTION
    )

    // Consumer Usage Codes (16-bit little-endian)
    const val CONSUMER_POWER: Short = 0x0030
    const val CONSUMER_HOME: Short = 0x0223
    const val CONSUMER_BACK: Short = 0x0224
    const val CONSUMER_MENU: Short = 0x0040

    // Volume & Media
    const val CONSUMER_VOLUME_UP: Short = 0x00E9
    const val CONSUMER_VOLUME_DOWN: Short = 0x00EA
    const val CONSUMER_MUTE: Short = 0x00E2
    const val CONSUMER_PLAY_PAUSE: Short = 0x00CD
    const val CONSUMER_SCAN_NEXT: Short = 0x00B5
    const val CONSUMER_SCAN_PREV: Short = 0x00B6

    // D-Pad navigation for Android TV
    const val CONSUMER_DPAD_UP: Short = 0x0042
    const val CONSUMER_DPAD_DOWN: Short = 0x0043
    const val CONSUMER_DPAD_LEFT: Short = 0x0044
    const val CONSUMER_DPAD_RIGHT: Short = 0x0045
    const val CONSUMER_DPAD_SELECT: Short = 0x0041 // OK / Select

    // Keyboard Modifier Bits
    const val MOD_NONE: Byte = 0
    const val MOD_LEFT_CTRL: Byte = 1
    const val MOD_LEFT_SHIFT: Byte = 2
    const val MOD_LEFT_ALT: Byte = 4
    const val MOD_LEFT_GUI: Byte = 8 // Windows / Home key

    // Standard USB HID Keyboard Usage Codes
    const val KEY_NONE: Byte = 0x00
    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESCAPE: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C
    const val KEY_RIGHT: Byte = 0x4F
    const val KEY_LEFT: Byte = 0x50
    const val KEY_DOWN: Byte = 0x51
    const val KEY_UP: Byte = 0x52
    const val KEY_DELETE: Byte = 0x4C
}
