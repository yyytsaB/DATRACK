package com.loadpredictor.testing

import androidx.test.runner.AndroidJUnitRunner

/** Instrumentation is allowed only against the disposable verification installation. */
class SafeTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        check(targetContext.packageName == "com.loadpredictor.releasecheck.audit") {
            "Refusing instrumentation against a personal or Preview installation"
        }
        super.onStart()
    }
}
