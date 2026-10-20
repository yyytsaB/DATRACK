package com.loadpredictor.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Integer centavos avoid floating-point rounding in manually entered load prices. */
fun parseLoadPrice(text: String): Long? {
    if (text.isBlank()) return null
    return try {
        BigDecimal(text.trim()).setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2).longValueExact().takeIf { it >= 0 }
    } catch (_: ArithmeticException) { null }
      catch (_: NumberFormatException) { null }
}
