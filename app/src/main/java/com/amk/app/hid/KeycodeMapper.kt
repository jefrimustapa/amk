package com.amk.app.hid

object KeycodeMapper {
    /**
     * Translates standard ASCII character to USB HID Keycode and Shift Modifier
     */
    fun charToHid(c: Char): Pair<Byte, Byte>? {
        return when (c) {
            in 'a'..'z' -> Pair(HidConstants.MOD_NONE, (0x04 + (c - 'a')).toByte())
            in 'A'..'Z' -> Pair(HidConstants.MOD_LEFT_SHIFT, (0x04 + (c.lowercaseChar() - 'a')).toByte())
            '1' -> Pair(HidConstants.MOD_NONE, 0x1E.toByte())
            '2' -> Pair(HidConstants.MOD_NONE, 0x1F.toByte())
            '3' -> Pair(HidConstants.MOD_NONE, 0x20.toByte())
            '4' -> Pair(HidConstants.MOD_NONE, 0x21.toByte())
            '5' -> Pair(HidConstants.MOD_NONE, 0x22.toByte())
            '6' -> Pair(HidConstants.MOD_NONE, 0x23.toByte())
            '7' -> Pair(HidConstants.MOD_NONE, 0x24.toByte())
            '8' -> Pair(HidConstants.MOD_NONE, 0x25.toByte())
            '9' -> Pair(HidConstants.MOD_NONE, 0x26.toByte())
            '0' -> Pair(HidConstants.MOD_NONE, 0x27.toByte())

            '!' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x1E.toByte())
            '@' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x1F.toByte())
            '#' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x20.toByte())
            '$' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x21.toByte())
            '%' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x22.toByte())
            '^' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x23.toByte())
            '&' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x24.toByte())
            '*' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x25.toByte())
            '(' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x26.toByte())
            ')' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x27.toByte())

            ' ' -> Pair(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            '\n' -> Pair(HidConstants.MOD_NONE, HidConstants.KEY_ENTER)
            '\t' -> Pair(HidConstants.MOD_NONE, HidConstants.KEY_TAB)
            '\b' -> Pair(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)

            '-' -> Pair(HidConstants.MOD_NONE, 0x2D.toByte())
            '_' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x2D.toByte())
            '=' -> Pair(HidConstants.MOD_NONE, 0x2E.toByte())
            '+' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x2E.toByte())
            '[' -> Pair(HidConstants.MOD_NONE, 0x2F.toByte())
            '{' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x2F.toByte())
            ']' -> Pair(HidConstants.MOD_NONE, 0x30.toByte())
            '}' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x30.toByte())
            '\\' -> Pair(HidConstants.MOD_NONE, 0x31.toByte())
            '|' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x31.toByte())
            ';' -> Pair(HidConstants.MOD_NONE, 0x33.toByte())
            ':' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x33.toByte())
            '\'' -> Pair(HidConstants.MOD_NONE, 0x34.toByte())
            '"' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x34.toByte())
            '`' -> Pair(HidConstants.MOD_NONE, 0x35.toByte())
            '~' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x35.toByte())
            ',' -> Pair(HidConstants.MOD_NONE, 0x36.toByte())
            '<' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x36.toByte())
            '.' -> Pair(HidConstants.MOD_NONE, 0x37.toByte())
            '>' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x37.toByte())
            '/' -> Pair(HidConstants.MOD_NONE, 0x38.toByte())
            '?' -> Pair(HidConstants.MOD_LEFT_SHIFT, 0x38.toByte())

            else -> null
        }
    }
}
