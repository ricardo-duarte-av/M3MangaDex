package pt.aguiarvieira.m3mangadex.core.network

/**
 * At most [permits] acquisitions in any [periodNanos] window (a sliding log). MangaDex allows about
 * five requests a second per IP and bans clients that keep exceeding it, so every API call waits
 * here first. Blocking by design: it runs on OkHttp's dispatcher threads.
 */
class RateLimiter(
    private val permits: Int,
    private val periodNanos: Long,
    private val clock: () -> Long = System::nanoTime,
    private val sleep: (nanos: Long) -> Unit = { Thread.sleep(it / NANOS_PER_MILLI, (it % NANOS_PER_MILLI).toInt()) },
) {
    private val stamps = ArrayDeque<Long>()

    fun acquire() {
        while (true) {
            val wait: Long
            synchronized(stamps) {
                val now = clock()
                while (stamps.isNotEmpty() && now - stamps.first() >= periodNanos) stamps.removeFirst()
                if (stamps.size < permits) {
                    stamps.addLast(now)
                    return
                }
                wait = periodNanos - (now - stamps.first())
            }
            sleep(wait)
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
