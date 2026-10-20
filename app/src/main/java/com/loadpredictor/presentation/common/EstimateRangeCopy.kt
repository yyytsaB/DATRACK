package com.loadpredictor.presentation.common

/** Bounds may be asymmetric after expiry clipping; never imply a calibrated probability. */
object EstimateRangeCopy {
    fun label(early:Long?,late:Long?,now:Long):String? {
        if(early==null || late==null || late<=early || early<now)return null
        val day=86400000L
        val lo=(early-now)/day;val hi=(late-now+day-1)/day
        return "Estimated range: $lo–$hi days"
    }
}
