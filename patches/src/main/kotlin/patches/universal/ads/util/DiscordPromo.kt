package patches.universal.ads.util

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

internal object DiscordPromo {
    private val registered = AtomicBoolean(false)

    private const val MESSAGE =
        "Join the discord server https://discord.gg/an9uXxMNyc for updates and game-specific patches"

    // Emitted from a shutdown hook so it lands after every patch has finished
    // reporting, instead of on whichever patch happened to run first. The
    // patcher owns log ordering otherwise, so the only reliable "end of log"
    // is process exit.
    fun logOnce(logger: Logger) {
        if (registered.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        System.out.println(MESSAGE)
                        System.out.flush()
                    },
                )
            } catch (_: Throwable) {
                logger.info(MESSAGE)
            }
        }
    }
}
