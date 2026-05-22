package com.example.accomlink.utils

/** Validates payment card numbers with the Luhn checksum. */
object LuhnCheck {
    /** Returns true when the supplied card number passes Luhn validation. */
    fun isValid(number: String): Boolean {
        val digits = number.filter(Char::isDigit)
        if (digits.length != 16) return false
        var sum = 0
        var doubleDigit = false
        for (index in digits.length - 1 downTo 0) {
            var value = digits[index].digitToInt()
            if (doubleDigit) {
                value *= 2
                if (value > 9) value -= 9
            }
            sum += value
            doubleDigit = !doubleDigit
        }
        return sum % 10 == 0
    }
}
