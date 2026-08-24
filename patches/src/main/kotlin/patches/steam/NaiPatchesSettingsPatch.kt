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
val naiPatchesSettingsPatch = bytecodePatch(
    name = "Nai Patches Settings",
    description = "Steam: adds unified Settings dialog (Options Menu → Nai Patches) to toggle all Steam patches at runtime without re-patching.",
    default = false,
) {
    compatibleWith("com.valvesoftware.android.steam.community")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = "Lcom/valvesoftware/android/steam/community/MainActivity;"
        val mutable = mutableClassDefByOrNull(target) ?: run {
            logger.warning("MainActivity not found")
            return@execute
        }

        // Add field to hold settings layout
        if (mutable.fields.none { it.name == "naiSettingsLayout" }) {
            mutable.fields.add(
                ImmutableField(target, "naiSettingsLayout", "Landroid/widget/LinearLayout;", AccessFlags.PRIVATE.value, null, emptySet(), emptySet()).toMutable()
            )
        }

        val checkIface = "Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;"
        if (checkIface !in mutable.interfaces) mutable.interfaces.add(checkIface)

        // onCheckedChanged(CompoundButton,bool) -> save to SharedPreferences
        if (mutable.methods.none { it.name == "onCheckedChanged" && it.parameterTypes == listOf("Landroid/widget/CompoundButton;", "Z") }) {
            val emptyImpl = ImmutableMethodImplementation(5, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onCheckedChanged",
                listOf(
                    ImmutableMethodParameter("Landroid/widget/CompoundButton;", emptySet(), null),
                    ImmutableMethodParameter("Z", emptySet(), null)
                ),
                "V", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                const-string v0, "nai64_patches"
                const/4 v1, 0x0
                invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v0
                invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0
                invoke-virtual {p1}, Landroid/widget/CompoundButton;->getTag()Ljava/lang/Object;
                move-result-object v1
                check-cast v1, Ljava/lang/String;
                invoke-interface {v0, v1, p2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0
                invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
                return-void
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        }

        // onCreateOptionsMenu -> add Nai's Patches item
        if (mutable.methods.none { it.name == "onCreateOptionsMenu" && it.parameterTypes == listOf("Landroid/view/Menu;") }) {
            val emptyImpl = ImmutableMethodImplementation(3, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onCreateOptionsMenu",
                listOf(ImmutableMethodParameter("Landroid/view/Menu;", emptySet(), null)),
                "Z", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                const v0, 0xCAFF
                const/4 v1, 0x0
                const/4 v2, 0x0
                const-string v3, "Nai's Patches"
                invoke-interface {p1, v0, v1, v2, v3}, Landroid/view/Menu;->add(IIILjava/lang/CharSequence;)Landroid/view/MenuItem;
                const/4 v0, 0x1
                return v0
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        } else {
            val existing = mutable.methods.first { it.name == "onCreateOptionsMenu" && it.parameterTypes == listOf("Landroid/view/Menu;") }
            val cloned = existing.cloneMutable(additionalRegisters = 2)
            val p0 = cloned.p0Register
            // p1 is Menu, need to find its register - cloneMutable shifts, use p0+1
            // For onCreateOptionsMenu(Menu), p1 is Menu. Use register v(b) where b is original registerCount
            val b = existing.implementation!!.registerCount
            val smali = """
                const v${b}, 0xCAFF
                const/4 v${b+1}, 0x0
                const/4 v${b+2}, 0x0
                const-string v${b+3}, "Nai's Patches"
                move-object v${b+4}, p1
                invoke-interface {v${b+4}, v${b}, v${b+1}, v${b+2}, v${b+3}}, Landroid/view/Menu;->add(IIILjava/lang/CharSequence;)Landroid/view/MenuItem;
            """.trimIndent()
            // Find return instruction and inject before it
            // For simplicity, prepend at 0 and keep original logic (original will also add Custom RP)
            cloned.addInstructionsWithLabels(0, smali)
            mutable.methods.remove(existing)
            mutable.methods.add(cloned)
            logger.info("Patched existing onCreateOptionsMenu to add Nai's Patches")
        }

        // onOptionsItemSelected -> handle Nai's Patches (and Custom RP fallback)
        val existingOpt = mutable.methods.firstOrNull { it.name == "onOptionsItemSelected" && it.parameterTypes == listOf("Landroid/view/MenuItem;") }
        if (existingOpt == null) {
            val emptyImpl = ImmutableMethodImplementation(10, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onOptionsItemSelected",
                listOf(ImmutableMethodParameter("Landroid/view/MenuItem;", emptySet(), null)),
                "Z", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                invoke-interface {p1}, Landroid/view/MenuItem;->getItemId()I
                move-result v0
                const v1, 0xCAFF
                if-eq v0, v1, :show_nai
                const v1, 0xCAFE
                if-eq v0, v1, :show_custom_rp_fallback
                const/4 v0, 0x0
                return v0
                :show_custom_rp_fallback
                # Custom RP fallback - show its dialog (duplicate from CustomPresencePatch but inlined here for standalone)
                new-instance v0, Landroid/widget/LinearLayout;
                invoke-direct {v0, p0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                const/4 v1, 0x1
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setOrientation(I)V
                const/16 v1, 0x20
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setPadding(IIII)V
                new-instance v1, Landroid/widget/EditText;
                invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V
                const-string v2, "App ID"
                invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v1, Landroid/app/AlertDialog${'$'}Builder;
                invoke-direct {v1, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
                const-string v2, "Custom RP"
                invoke-virtual {v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                const-string v2, "Close"
                const/4 v3, 0x0
                invoke-virtual {v1, v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
                const/4 v0, 0x1
                return v0
                :show_nai
                new-instance v0, Landroid/widget/LinearLayout;
                invoke-direct {v0, p0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                const/4 v1, 0x1
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setOrientation(I)V
                const/16 v1, 0x20
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setPadding(IIII)V
                iput-object v0, p0, Lcom/valvesoftware/android/steam/community/MainActivity;->naiSettingsLayout:Landroid/widget/LinearLayout;
                const-string v1, "nai64_patches"
                const/4 v2, 0x0
                invoke-virtual {p0, v1, v2}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v1
                new-instance v2, Landroid/widget/Switch;
                invoke-direct {v2, p0}, Landroid/widget/Switch;-><init>(Landroid/content/Context;)V
                const-string v3, "Dev Support"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setText(Ljava/lang/CharSequence;)V
                const-string v3, "steam_dev_support"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setTag(Ljava/lang/Object;)V
                const-string v4, "steam_dev_support"
                const/4 v5, 0x0
                invoke-interface {v1, v4, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
                move-result v4
                invoke-virtual {v2, v4}, Landroid/widget/Switch;->setChecked(Z)V
                invoke-virtual {v2, p0}, Landroid/widget/Switch;->setOnCheckedChangeListener(Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;)V
                invoke-virtual {v0, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v2, Landroid/widget/Switch;
                invoke-direct {v2, p0}, Landroid/widget/Switch;-><init>(Landroid/content/Context;)V
                const-string v3, "Debug Build"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setText(Ljava/lang/CharSequence;)V
                const-string v3, "steam_debug_build"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setTag(Ljava/lang/Object;)V
                const-string v4, "steam_debug_build"
                const/4 v5, 0x0
                invoke-interface {v1, v4, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
                move-result v4
                invoke-virtual {v2, v4}, Landroid/widget/Switch;->setChecked(Z)V
                invoke-virtual {v2, p0}, Landroid/widget/Switch;->setOnCheckedChangeListener(Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;)V
                invoke-virtual {v0, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v2, Landroid/widget/Switch;
                invoke-direct {v2, p0}, Landroid/widget/Switch;-><init>(Landroid/content/Context;)V
                const-string v3, "Beta Features"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setText(Ljava/lang/CharSequence;)V
                const-string v3, "steam_beta"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setTag(Ljava/lang/Object;)V
                const-string v4, "steam_beta"
                const/4 v5, 0x0
                invoke-interface {v1, v4, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
                move-result v4
                invoke-virtual {v2, v4}, Landroid/widget/Switch;->setChecked(Z)V
                invoke-virtual {v2, p0}, Landroid/widget/Switch;->setOnCheckedChangeListener(Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;)V
                invoke-virtual {v0, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v2, Landroid/widget/Switch;
                invoke-direct {v2, p0}, Landroid/widget/Switch;-><init>(Landroid/content/Context;)V
                const-string v3, "Increase Limits"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setText(Ljava/lang/CharSequence;)V
                const-string v3, "steam_limits"
                invoke-virtual {v2, v3}, Landroid/widget/Switch;->setTag(Ljava/lang/Object;)V
                const-string v4, "steam_limits"
                const/4 v5, 0x0
                invoke-interface {v1, v4, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
                move-result v4
                invoke-virtual {v2, v4}, Landroid/widget/Switch;->setChecked(Z)V
                invoke-virtual {v2, p0}, Landroid/widget/Switch;->setOnCheckedChangeListener(Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;)V
                invoke-virtual {v0, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v1, Landroid/app/AlertDialog${'$'}Builder;
                invoke-direct {v1, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
                const-string v2, "Nai's Patches Settings"
                invoke-virtual {v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                const-string v2, "Close"
                const/4 v3, 0x0
                invoke-virtual {v1, v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
                const/4 v0, 0x1
                return v0
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        } else {
            val existing = mutable.methods.first { it.name == "onOptionsItemSelected" && it.parameterTypes == listOf("Landroid/view/MenuItem;") }
            val cloned = existing.cloneMutable(additionalRegisters = 6)
            val smali = """
                invoke-interface {p1}, Landroid/view/MenuItem;->getItemId()I
                move-result v0
                const v1, 0xCAFF
                if-eq v0, v1, :show_nai_patched
                goto :orig_continue
                :show_nai_patched
                new-instance v0, Landroid/widget/LinearLayout;
                invoke-direct {v0, p0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                const/4 v1, 0x1
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setOrientation(I)V
                new-instance v1, Landroid/app/AlertDialog${'$'}Builder;
                invoke-direct {v1, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
                const-string v2, "Nai's Patches Settings"
                invoke-virtual {v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
                const/4 v0, 0x1
                return v0
                :orig_continue
                nop
            """.trimIndent()
            cloned.addInstructionsWithLabels(0, smali)
            mutable.methods.remove(existing)
            mutable.methods.add(cloned)
            logger.info("Patched existing onOptionsItemSelected to add Nai's Patches")
        }

        logger.info("Nai's Patches Settings injected into MainActivity")
    }
}
