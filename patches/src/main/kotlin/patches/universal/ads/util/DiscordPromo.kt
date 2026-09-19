package patches.universal.ads.util

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

internal object DiscordPromo {
    private val shown = AtomicBoolean(false)

    fun logOnce(logger: Logger) {
        if (shown.compareAndSet(false, true)) {
            logger.info("Join the discord server https://discord.gg/an9uXxMNyc for updates")
        }
    }
}
