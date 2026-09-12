package patches.universal.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldIntGetterConst
import patches.universal.misc.foldObjectGetterToNull
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideNetworkIdentityPatch = bytecodePatch(
    name = "Hide Network Identity",
    description = "Hides network interfaces, DNS servers and the local IP so apps cannot fingerprint your LAN",
    default = false,
) {
    category("Privacy")
    val hideInterfaces by booleanOption(
        title = "Hide interfaces",
        default = true,
        key = "hideNetworkInterfaces",
        description = "Report no network interfaces and null interface lookups.",
    )
    val hideDns by booleanOption(
        title = "Hide DNS",
        default = true,
        key = "hideDnsServers",
        description = "Null link properties and empty DNS, link address and route lists.",
    )
    val hideLocalIp by booleanOption(
        title = "Hide local IP",
        default = true,
        key = "hideLocalIp",
        description = "Report the WiFi IP address as 0.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        if (hideInterfaces == true) {
            patched += replaceGetterWithStaticCall(
                "Ljava/net/NetworkInterface;",
                setOf("getNetworkInterfaces"),
                "Ljava/util/Enumeration;",
                "invoke-static {}, Ljava/util/Collections;->emptyEnumeration()Ljava/util/Enumeration;",
            )
            patched += foldObjectGetterToNull(
                "Ljava/net/NetworkInterface;",
                setOf("getByName", "getByInetAddress"),
                "Ljava/net/NetworkInterface;",
            )
        }
        if (hideDns == true) {
            patched += foldObjectGetterToNull(
                "Landroid/net/ConnectivityManager;",
                setOf("getLinkProperties"),
                "Landroid/net/LinkProperties;",
            )
            patched += replaceGetterWithStaticCall(
                "Landroid/net/LinkProperties;",
                setOf("getDnsServers", "getLinkAddresses", "getRoutes"),
                "Ljava/util/List;",
                "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
            )
        }
        if (hideLocalIp == true) {
            patched += foldIntGetterConst(
                "Landroid/net/wifi/WifiInfo;",
                setOf("getIpAddress"),
                0,
            )
        }
        if (patched > 0) logger.info("Hid network identity at $patched call site(s)")
        else logger.warning("No network identity calls found. No changes applied.")
    }
}
