package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object PerfTraceStartFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/perf/metrics/Trace;",
    name = "start",
    returnType = "V",
)

internal object PerfTraceStopFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/perf/metrics/Trace;",
    name = "stop",
    returnType = "V",
)

internal object PerfTraceIncrementFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/perf/metrics/Trace;",
    name = "incrementMetric",
    returnType = "V",
)

internal object PerfHttpStartFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/perf/metrics/HttpMetric;",
    name = "start",
    returnType = "V",
)

internal object PerfHttpStopFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/perf/metrics/HttpMetric;",
    name = "stop",
    returnType = "V",
)

@Suppress("unused")
val disableFirebasePerformancePatch = bytecodePatch(
    name = "Disable Firebase Performance",
    description = "Noops Firebase Performance traces and HTTP metrics (start, stop, counters). Stops performance spying with zero behavior change.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var total = 0
        total += gmsNoopAll(logger, PerfTraceStartFingerprint, "Disable Firebase Performance: Trace.start")
        total += gmsNoopAll(logger, PerfTraceStopFingerprint, "Disable Firebase Performance: Trace.stop")
        total += gmsNoopAll(logger, PerfTraceIncrementFingerprint, "Disable Firebase Performance: Trace.incrementMetric")
        total += gmsNoopAll(logger, PerfHttpStartFingerprint, "Disable Firebase Performance: HttpMetric.start")
        total += gmsNoopAll(logger, PerfHttpStopFingerprint, "Disable Firebase Performance: HttpMetric.stop")
        if (total == 0) logger.warning("Disable Firebase Performance: no Performance methods found. No changes applied.")
        else logger.info("Disable Firebase Performance: disabled $total method(s) in total")
    }
}
