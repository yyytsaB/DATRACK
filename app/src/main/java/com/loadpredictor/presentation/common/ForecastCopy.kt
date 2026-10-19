package com.loadpredictor.presentation.common

import com.loadpredictor.domain.model.BurnPace
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ForecastCopy {
    fun status(pace: BurnPace, noExpiry: Boolean): String = when (pace) {
        BurnPace.EXPIRED -> "Promo expired"
        BurnPace.DEPLETED -> "Data used up"
        BurnPace.INSUFFICIENT_DATA -> "Learning your usage"
        BurnPace.BURNING_FAST -> "May run out early"
        BurnPace.ON_TRACK -> if (noExpiry) "Steady usage" else "Will last until expiry"
        BurnPace.CONSERVATIVE -> "Will last until expiry"
    }
    fun runway(pace: BurnPace, depletion: Long?, now: Long): String {
        if (pace == BurnPace.EXPIRED) return "Validity ended"
        if (pace == BurnPace.DEPLETED) return "No data left"
        if (pace == BurnPace.INSUFFICIENT_DATA || depletion == null) return "Estimate not ready"
        val days = ((depletion - now).coerceAtLeast(0L) / 86_400_000L).toInt()
        return when {
            days >= 60 -> "About ${kotlin.math.round(days / 30.0).toInt()} months left"
            days >= 2 -> "About $days days left"
            days == 1 -> "About 1 day left"
            else -> "Less than 1 day left"
        }
    }
    fun updated(at: Long, now: Long): String {
        if (at <= 0) return "No reading yet"
        val minutes = ((now - at).coerceAtLeast(0L) / 60_000L)
        return when {
            minutes < 1 -> "Checked just now"
            minutes < 60 -> "Checked $minutes min ago"
            minutes < 1440 -> "Checked ${minutes / 60} hr ago"
            else -> "Reading ${minutes / 1440} days old"
        }
    }
    fun date(at: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(at))
}
