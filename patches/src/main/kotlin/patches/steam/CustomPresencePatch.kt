package patches.steam

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import patches.universal.ads.util.cloneMutable
import patches.universal.ads.util.p0Register
import java.util.logging.Logger

@Suppress("unused")
val customPresencePatch = bytecodePatch(
    name = "Custom Rich Presence",
    description = "Steam: adds in-app editable Custom Rich Presence (tap RP button to cycle presets, stored in SharedPreferences, changeable anytime without re-patching).",
    default = false,
) {
    compatibleWith("com.valvesoftware.android.steam.community")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = "Lcom/valvesoftware/android/steam/community/MainActivity;"
        val mutable = mutableClassDefByOrNull(target) ?: run {
            logger.warning("MainActivity not found. No changes applied.")
            return@execute
        }

        val clickIface = "Landroid/view/View${'$'}OnClickListener;"
        if (clickIface !in mutable.interfaces) mutable.interfaces.add(clickIface)

        if (mutable.methods.none { it.name == "onClick" && it.parameterTypes == listOf("Landroid/view/View;") }) {
            val emptyImpl = ImmutableMethodImplementation(6, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onClick",
                listOf(ImmutableMethodParameter("Landroid/view/View;", emptySet(), null)),
                "V", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                const-string v0, "custom_presence"
                const/4 v1, 0x0
                invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v0
                const-string v1, "presence_index"
                const/4 v2, 0x0
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
                move-result v1
                add-int/lit8 v1, v1, 0x1
                rem-int/lit8 v1, v1, 0x4
                invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v2
                const-string v3, "presence_index"
                invoke-interface {v2, v3, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v2
                invoke-interface {v2}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
                if-eqz v1, :cond_0
                const/4 v2, 0x1
                if-eq v1, v2, :cond_1
                const/4 v2, 0x2
                if-eq v1, v2, :cond_2
                const-string v2, "Custom: Playing Dota 2"
                goto :show
                :cond_0
                const-string v2, "Custom: Online"
                goto :show
                :cond_1
                const-string v2, "Custom: Playing CS2"
                goto :show
                :cond_2
                const-string v2, "Custom: Away"
                :show
                const/4 v3, 0x0
                invoke-static {p0, v2, v3}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
                move-result-object v2
                invoke-virtual {v2}, Landroid/widget/Toast;->show()V
                return-void
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        }

        val onCreate = mutable.methods.firstOrNull {
            it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;")
        } ?: run {
            logger.warning("MainActivity onCreate not found")
            return@execute
        }
        val cloned = onCreate.cloneMutable(additionalRegisters = 4)
        val p0 = cloned.p0Register
        val b = cloned.implementation!!.registerCount
        val smali = """
            new-instance v$b, Landroid/widget/Button;
            move-object/from16 v${b+1}, v$p0
            invoke-direct/range {v$b .. v${b+1}}, Landroid/widget/Button;-><init>(Landroid/content/Context;)V
            const-string v${b+1}, "RP"
            invoke-virtual {v$b, v${b+1}}, Landroid/widget/Button;->setText(Ljava/lang/CharSequence;)V
            move-object/from16 v${b+1}, v$p0
            invoke-virtual/range {v$b .. v${b+1}}, Landroid/widget/Button;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
            invoke-virtual {v$p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
            move-result-object v${b+1}
            invoke-virtual {v${b+1}}, Landroid/view/Window;->getDecorView()Landroid/view/View;
            move-result-object v${b+1}
            check-cast v${b+1}, Landroid/view/ViewGroup;
            const/16 v${b+2}, 0x11
            invoke-virtual {v${b+1}, v$b, v${b+2}}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
        """.trimIndent()
        cloned.addInstructionsWithLabels(0, smali)
        mutable.methods.remove(onCreate)
        mutable.methods.add(cloned)

        logger.info("Custom Rich Presence (in-app cyclable) injected into MainActivity")
    }
}
