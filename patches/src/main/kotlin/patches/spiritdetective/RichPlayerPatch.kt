package patches.spiritdetective

import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.rawResourcePatch
import kotlin.experimental.xor
import java.io.File
import java.security.MessageDigest
import java.util.logging.Logger

private const val PACKAGE = "com.ghost.detective.rpg"
private const val BIG = 1_000_000_000

private val REWARD_IDS = setOf(
    1, 3, 5, 7, 8, 11,
    100001, 100002, 100003, 100004, 100005, 100006, 100007, 100008, 100009, 100010,
    100011, 100012, 100013, 100014, 100015, 100016, 100017, 100018, 100019, 100020,
    100021, 100022, 100023, 100024, 100025, 100026, 100027, 100028, 100030,
)
private val COST_IDS = setOf(
    1, 6, 7, 11, 12, 100001, 100002, 100015, 100019, 100020, 100022, 100023,
    100024, 100026, 100028, 161001, 162001,
)

private val EQUIPMENT_COSTS = setOf(
    100001, 100002, 100019, 100020, 100023, 100024, 100026, 12, 161001, 162001,
)

private object Assets {
    // Hashes of the original files after every value these patches can change
    // is replaced with 0, so patches can be applied in any combination.
    val hashes = mapOf(
        "assets/res/CWq_hTr/ZlHDBrwOECiUh39Xx.json" to "07760acd4c9661f16ade678c5a5e3972c5d6aae07e6d09cf7ce7520e121868a2",
        "assets/res/Olg/1uhkX/jMySqPpOxfva2.json" to "cc9ab38a6b2e2dfa334b1231d8988efa2808beb6849a38cac4ff11f94d616aac",
        "assets/res/Olg/1uhkX/vg1XBJVf/pnbANOZ2Wx5Pfm.json" to "85719aa1bb810d05320992233e2a1561ec88bd9af45abe0ce8aa73dee9e90aaf",
        "assets/res/Olg/JDBYmwS7/xNzflhyJ/7GTmqwkRj8Jxc_AliHD.json" to "5032acbef8d4496e2846c01d15170990ad5b2185ebca341e39c9d33023662615",
        "assets/res/Olg/OCs/5ySgaT/Jz_noxNjsi8EPHtO.json" to "2041fe789885a5b017a63b7218ab921eb582793084bbbbd8b72b7168663912db",
        "assets/res/Olg/c0I/6zj2cy1KrtRU-FM0.json" to "7f4e3371d79e76ef95c606422d78fb6cc0741a714ae8569b5c1bf95e0d43768f",
        "assets/res/Olg/ujW-Fx/0qNlfzoD/XVHaNRBsyQ12.json" to "17432de3ea684faaacae2a059f9d16f081f33bbd8e3c525fce9741709b045a69",
        "assets/res/Olg/ujW-Fx/FhvA1/26JBdPlWXkzEA5jHmI-eqxK37.json" to "0b3d8b040cf6a0b9c54834e847c5832d0cc14df1c6f8ac0722dc10bf64a1e9df",
        "assets/res/Olg/ujW-Fx/FhvA1/ohSw-b8XDdWF79Vjy3.json" to "9b329728d56451897f17322484c6b876787302decc0eaf448785f07eb8d92593",
        "assets/res/Olg/ujW-Fx/p-kbmDKG/fUwsBHGpqZ5M40J2YDkx6.json" to "bc5b922620cd818956d1304cb8bd98efccccf3d66c14562884b1b76a015a35f5",
        "assets/res/Olg/vkVLYCTM/ajvlI8/Y31Qdj_ILyC.json" to "93cdb4addac96505d13a83df68a7d071f9cebd9309f5d82eb1f99327cfa544e4",
        "assets/res/Olg/vkVLYCTM/ajvlI8/ed482iJ9Es5.json" to "04207057be2dcd3824ac3457abbbbc4ac3a6d6e7a45b339050abfe4024825b97",
        "assets/res/Olg/vkVLYCTM/qB7A/nz7xl3YKi80qOU.json" to "f07ad3f143f074e99061497cccfde43ca4f4682cb9b9977f95098452ea747b1b",
        "assets/res/Olg/vkVLYCTM/vSmfU5t/gTMybu_BntX.json" to "8429920552d87d8650cbc305bc2e4fe276daae80e98aeebefc1365231e8b7a7f",
        "assets/res/Olg/vkVLYCTM/vSmfU5t/h3ZOWLqBsixrfvP5UlHtngJ.json" to "2791e78061526f08cf9e25dbee45f8b0598b9eed1bd10b82394131610c510947",
        "assets/res/RcCo/50q/L4punV/AnWf1mv3JN.json" to "f8ebd28849a72973f82921e5a41f84fe70281a827e47e40a18f3ca592abba02e",
        "assets/res/RcCo/50q/hp53/7QSdkbemjVZMWKp.json" to "8da0908d98f1f88df51660ebc90361faa4e0923289a526fe30747f2a5d73134e",
        "assets/res/RcCo/50q/hp53/E5yrYSPR_LAJTm.json" to "55098afc2dd9a8c79786d642e689a32c8625d2f92a49bba81c34891bc1fa7b9e",
        "assets/res/RcCo/50q/hp53/HTYj6QmNeb9.json" to "2abb2cb4e53a1b05faabd4c9e9b07aaa3436ae33d473ec820adb879d5c8daeb8",
        "assets/res/RcCo/50q/hp53/JcRsLjeB8qaxtA.json" to "7f0a3e4e3205de00436cc167207790048669c452d0eb9573ac3aca55e981c27c",
        "assets/res/RcCo/50q/nMb/kD7BUbol1XIKEdhcR-a86vLM.json" to "1233c16eaa1b2ea33651b01d99d7fa7a31c04a4169a4593d46b282bc4dfd0add",
        "assets/res/RcCo/NyW-T4l/TBlD8Ftpsz2e7GN1iZjQ.json" to "dce235ee0bd7756811c9ddd9ec004bac56dc6ee4de0737dde9738afe7619c073",
        "assets/res/RcCo/NyW-T4l/XtZC8q-ugkD6o5KIYpAbm.json" to "f54b2b65b1be7cee2c7c88634f9ff719497d7e4c4933882d80426e15d98531fc",
        "assets/res/RcCo/NyW-T4l/gzViYxAs4lC2TUbjW8t7Im19q.json" to "bdee4da827f54a4f5df82386402e7a7f4319a766ae9cb35875bf9eb6680570a9",
        "assets/res/RcCo/W9sCzmU/5Og/Q4Lqth8VxjScvXa75GDUsn.json" to "78437019b878a295344501ae8d5854d264396abf1d82a2be84b33496ab13b164",
        "assets/res/RcCo/W9sCzmU/Rtc0/xq9PrFaiL1-B3jk.json" to "a5cd118bb20bf74fb5805d6684efa58dea9e2c3cd4bdabceabd0549688617d1b",
        "assets/res/RcCo/W9sCzmU/eIPSC/qv-n_wyXu830dzPWOlDLNAK1.json" to "ec0f35762c5e950fad7efe7412dbb718f256187662c6f25a9f06327babf34988",
        "assets/res/RcCo/W9sCzmU/s3Y/uIKliBd4PjHGnNM9JUZ.json" to "ca73bff45b40ff558e973d340bce86dc3a1f3e2ef1aae6514a9d1e3112ca1e7f",
        "assets/res/tPDIYB/DYCthJzGdwfabecB9r.json" to "6d6be95fc73f96105c2800b5c3eb1bfcc2a7a63cf4b1835230ae52bf3390224f",
        "assets/res/tPDIYB/QO9RAp6wcNSB.json" to "b1451dba70f67e183254c4fe7d871024ed67bf2de09394bb3a7848b08b7387e5",
        "assets/res/tPDIYB/l42sVdSzwQx3XA50ZIPURn.json" to "e9823925568bdab982ad93f5ee203775253744fddde65860d913deea1817b496",
        "assets/res/tPDIYB/rtD6RWISJ5ebm1MQndCK0.json" to "474a74e09ff0de0e67b57f2d1d8f4b741c19fad59e3335e5fd0efe942219d753",
    )
    val ALL: List<String> = hashes.keys.toList()
}

private val COMPAT = Compatibility(packageName = PACKAGE, name = "Spirit Detective")

private fun sha(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

private fun ids(ids: Set<Int>) = ids.joinToString("|")

private val itemValue = Regex("""\{"id":(${ids(REWARD_IDS)}),"(count|num)":\d+\}""")
private val NORMALIZED_COST_IDS = COST_IDS + REWARD_IDS
private val costObject = Regex(""""cost":\{"id":(${ids(NORMALIZED_COST_IDS)}),"count":\d+\}""")
private val costArray = Regex(""""cost":\[\{"id":(${ids(NORMALIZED_COST_IDS)}),"count":\d+\}\]""")

/** Replace every value this patch family can change, so all combinations hash alike. */
private fun normalize(text: String): String {
    var out = itemValue.replace(text) { m ->
        "{\"id\":${m.groupValues[1]},\"${m.groupValues[2]}\":0}"
    }
    out = costObject.replace(out) { m ->
        "\"cost\":{\"id\":${m.groupValues[1]},\"count\":0}"
    }
    out = costArray.replace(out) { m ->
        "\"cost\":[{\"id\":${m.groupValues[1]},\"count\":0}]"
    }
    return out
}

/**
 * Spirit Detective wraps its JSON config as:
 *   "encode" + 1-byte XOR key + XOR-ed text.
 * The same key byte is kept on write so the game's loader can still read the file.
 */
private class EncodedJson(val path: String, private val file: File) {
    private val original: ByteArray = file.readBytes()

    init {
        require(original.size > 7 && original.copyOf(6).decodeToString() == "encode") {
            "Unsupported Spirit Detective asset: $path"
        }
        require(sha(normalize(decode(original))) == Assets.hashes[path]) {
            "Unsupported Spirit Detective version: $path"
        }
    }

    var text: String = decode(original)

    private fun decode(bytes: ByteArray): String =
        ByteArray(bytes.size - 7) { bytes[it + 7] xor bytes[6] }.decodeToString()

    fun write() {
        if (text == decode(original)) return
        val payload = text.encodeToByteArray()
        val bytes = ByteArray(payload.size + 7)
        original.copyInto(bytes, 0, 0, 7)
        for (i in payload.indices) bytes[i + 7] = payload[i] xor bytes[6]
        file.writeBytes(bytes)
    }
}

/** Fields that describe a price or requirement, not a reward payout. */
private val COST_CONTEXT = listOf(
    "\"cost\"", "\"lvRes\"", "\"sweep\"", "\"consume\"", "\"need\"", "\"price\"",
)

private fun boost(assets: List<EncodedJson>, target: Set<Int>, logger: Logger, label: String) {
    val regex = Regex("""\{"id":(${ids(target)}),"(count|num)":(\d+)\}""")
    var total = 0
    for (asset in assets) {
        val current = asset.text
        var n = 0
        asset.text = regex.replace(current) { m ->
            val before = current.substring(maxOf(0, m.range.first - 48), m.range.first)
            if (COST_CONTEXT.any { it in before }) {
                m.value
            } else {
                n++
                "{\"id\":${m.groupValues[1]},\"${m.groupValues[2]}\":$BIG}"
            }
        }
        total += n
    }
    check(total > 0) { "No Spirit Detective rows matched for $label" }
    logger.info("Spirit Detective: $label boosted $total rows")
}

private fun zeroPurchases(assets: List<EncodedJson>, logger: Logger) {
    val regex = Regex("""\{"id":(${ids(REWARD_IDS)}),"(count|num)":(\d+)\}""")
    var total = 0
    for (asset in assets) {
        val current = asset.text
        var n = 0
        asset.text = regex.replace(current) { m ->
            val before = current.substring(maxOf(0, m.range.first - 48), m.range.first)
            if (COST_CONTEXT.any { it in before }) {
                n++
                "{\"id\":${m.groupValues[1]},\"${m.groupValues[2]}\":0}"
            } else m.value
        }
        total += n
    }
    check(total > 0) { "No Spirit Detective purchase costs matched" }
    logger.info("Spirit Detective: free purchases zeroed $total costs")
}

private fun zeroCosts(assets: List<EncodedJson>, target: Set<Int>, logger: Logger, label: String) {
    val obj = Regex(""""cost":\{"id":(${ids(target)}),"count":\d+\}""")
    val arr = Regex(""""cost":\[\{"id":(${ids(target)}),"count":\d+\}\]""")
    var total = 0
    for (asset in assets) {
        var text = obj.replace(asset.text) { m ->
            total++
            "\"cost\":{\"id\":${m.groupValues[1]},\"count\":0}"
        }
        text = arr.replace(text) { m ->
            total++
            "\"cost\":[{\"id\":${m.groupValues[1]},\"count\":0}]"
        }
        asset.text = text
    }
    check(total > 0) { "No Spirit Detective costs matched for $label" }
    logger.info("Spirit Detective: $label zeroed $total costs")
}

private fun load(get: (String) -> File): List<EncodedJson> =
    Assets.ALL.map { EncodedJson(it, get(it)) }

private fun finish(assets: List<EncodedJson>) = assets.forEach { it.write() }

@Suppress("unused")
val spiritDetectiveUnlimitedDiamondsPatch = rawResourcePatch(
    name = "Unlimited Diamonds",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(1), logger, "diamonds")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedGoldPatch = rawResourcePatch(
    name = "Unlimited Gold",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(3), logger, "gold")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedArmorShardsPatch = rawResourcePatch(
    name = "Unlimited Armor Shards",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(100001), logger, "armor shards")
        zeroCosts(assets, setOf(100001), logger, "armor shard upgrade")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedSkillPointsPatch = rawResourcePatch(
    name = "Unlimited Skill Points",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(11), logger, "skill points")
        zeroCosts(assets, setOf(11), logger, "skill point")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedKeysPatch = rawResourcePatch(
    name = "Unlimited Keys",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(100017, 100023, 100030), logger, "keys")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedVipPointsPatch = rawResourcePatch(
    name = "Unlimited VIP Points",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(7), logger, "VIP points")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedAdFreeTicketsPatch = rawResourcePatch(
    name = "Unlimited Ad-Free Tickets",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(5), logger, "ad-free tickets")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedActivePointsPatch = rawResourcePatch(
    name = "Unlimited Active Points",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        boost(assets, setOf(8, 100011, 100012), logger, "active points")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveUnlimitedMaterialsPatch = rawResourcePatch(
    name = "Unlimited Materials",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val materials = setOf(
            100002, 100003, 100004, 100005, 100006, 100007, 100008, 100009, 100010,
            100013, 100014, 100015, 100016, 100018, 100019, 100020, 100021, 100022,
            100024, 100025, 100026, 100027, 100028,
        )
        val assets = load { get(it) }
        boost(assets, materials, logger, "materials")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveFreePurchasesPatch = rawResourcePatch(
    name = "Free Purchases",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        zeroPurchases(assets, logger)
        zeroCosts(assets, REWARD_IDS, logger, "purchase")
        finish(assets)
    }
}

@Suppress("unused")
val spiritDetectiveFreeEquipmentUpgradesPatch = rawResourcePatch(
    name = "Free Equipment Upgrades",
    default = false,
) {
    compatibleWith(COMPAT)
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val assets = load { get(it) }
        zeroCosts(assets, EQUIPMENT_COSTS, logger, "equipment upgrade")
        finish(assets)
    }
}
