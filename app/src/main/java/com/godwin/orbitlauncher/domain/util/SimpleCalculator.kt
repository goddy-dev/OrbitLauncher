package com.godwin.orbitlauncher.domain.util

/**
 * Tiny recursive-descent evaluator for +, -, *, /, parentheses, and
 * decimals. No eval()/reflection/scripting engine -- just arithmetic.
 * Returns null if the input isn't a valid expression (e.g. plain text),
 * so callers can silently fall back to normal search.
 */
object SimpleCalculator {

    fun evaluateOrNull(input: String): Double? {
        val cleaned = input.replace(" ", "")
        if (cleaned.isEmpty() || cleaned.none { it.isDigit() }) return null
        if (cleaned.any { it !in "0123456789.+-*/()" }) return null

        return try {
            val parser = Parser(cleaned)
            val result = parser.parseExpression()
            if (parser.hasMoreInput()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val text: String) {
        private var pos = 0

        fun hasMoreInput(): Boolean = pos < text.length

        fun parseExpression(): Double {
            var value = parseTerm()
            while (hasMoreInput()) {
                when (text[pos]) {
                    '+' -> { pos++; value += parseTerm() }
                    '-' -> { pos++; value -= parseTerm() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (hasMoreInput()) {
                when (text[pos]) {
                    '*' -> { pos++; value *= parseFactor() }
                    '/' -> { pos++; value /= parseFactor() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            if (hasMoreInput() && text[pos] == '-') {
                pos++
                return -parseFactor()
            }
            if (hasMoreInput() && text[pos] == '(') {
                pos++
                val value = parseExpression()
                if (hasMoreInput() && text[pos] == ')') pos++
                return value
            }
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("Expected number at $pos")
            return text.substring(start, pos).toDouble()
        }
    }
}
