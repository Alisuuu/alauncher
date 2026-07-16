package com.alisu.alauncher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collectBaselineProfile(
            packageName = "com.alisu.alauncher"
        ) {
            pressHome()
            startActivityAndWait(
                intent = createIntent("com.alisu.alauncher/.MainActivity")
            )
            device.waitForIdle()
        }
    }
}
