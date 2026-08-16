package com.loadpredictor.domain.model

/**
 * Thrown when the app attempts to query device network usage statistics without
 * the required android.permission.PACKAGE_USAGE_STATS (Usage Access) permission.
 *
 * Distinguishes missing/revoked permission from genuine zero network usage,
 * ensuring the UI routes to an explicit permission request state rather than
 * displaying misleading zero data.
 */
class UsageAccessDeniedException(
    message: String = "Usage Access permission (PACKAGE_USAGE_STATS) is required to query mobile data usage",
    cause: Throwable? = null
) : SecurityException(message, cause)
