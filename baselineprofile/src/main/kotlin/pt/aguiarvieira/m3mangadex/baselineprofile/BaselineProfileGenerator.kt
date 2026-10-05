package pt.aguiarvieira.m3mangadex.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The paths worth compiling ahead of time: starting up to Browse and scrolling it, opening a
 * manga's details and scrolling its chapters, and paging through the reader. Works logged out; the
 * screens are found by test tag, so the device language doesn't matter.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            // Open first and scroll on the way back: a fling down and up doesn't always return to
            // the top, which would leave the next thing to tap off screen.
            device.wait(Until.hasObject(By.res("hero-0")), TIMEOUT)
            device.findObject(By.res("hero-0"))?.click() ?: error("No hero carousel on Browse")
            device.wait(Until.hasObject(By.res("read").enabled(true)), TIMEOUT)
            device.findObject(By.res("read"))?.click() ?: error("No read button on the details screen")
            device.waitForIdle()
            Thread.sleep(PAGE_WAIT_MILLIS)
            // Forward is the right edge left-to-right and the left edge right-to-left: tap both ways,
            // so pages turn whichever direction this manga reads in.
            listOf(EDGE_PX, device.displayWidth - EDGE_PX).forEach { x ->
                repeat(PAGES) {
                    device.click(x, device.displayHeight / 2)
                    Thread.sleep(PAGE_WAIT_MILLIS)
                }
            }
            device.pressBack()
            device.wait(Until.hasObject(By.res("manga-details")), TIMEOUT)
            fling(By.res("manga-details"))
            device.pressBack()
            device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT)
            fling(By.scrollable(true))
        }

    /** Down then up; the node is looked up again for each gesture (Compose replaces it as it scrolls). */
    private fun MacrobenchmarkScope.fling(selector: BySelector) {
        listOf(Direction.DOWN, Direction.UP).forEach { direction ->
            runCatching { device.findObject(selector)?.fling(direction) }
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE = "pt.aguiarvieira.m3mangadex"
        const val TIMEOUT = 20_000L
        const val PAGE_WAIT_MILLIS = 1_500L
        const val PAGES = 4
        const val EDGE_PX = 60
    }
}
