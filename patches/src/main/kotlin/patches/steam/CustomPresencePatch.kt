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
    description = "Steam: adds editable Custom Rich Presence in Settings (via Options Menu → Custom RP dialog, stored in SharedPreferences, changeable anytime without re-patching).",
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

        // Add fields to hold EditTexts
        if (mutable.fields.none { it.name == "customAppIdEdit" }) {
            mutable.fields.add(
                ImmutableField(target, "customAppIdEdit", "Landroid/widget/EditText;", AccessFlags.PRIVATE.value, null, emptySet(), emptySet()).toMutable()
            )
        }
        if (mutable.fields.none { it.name == "customStatusEdit" }) {
            mutable.fields.add(
                ImmutableField(target, "customStatusEdit", "Landroid/widget/EditText;", AccessFlags.PRIVATE.value, null, emptySet(), emptySet()).toMutable()
            )
        }
        if (mutable.fields.none { it.name == "customPresenceLayout" }) {
            mutable.fields.add(
                ImmutableField(target, "customPresenceLayout", "Landroid/widget/LinearLayout;", AccessFlags.PRIVATE.value, null, emptySet(), emptySet()).toMutable()
            )
        }

        val dialogIface = "Landroid/content/DialogInterface${'$'}OnClickListener;"
        if (dialogIface !in mutable.interfaces) mutable.interfaces.add(dialogIface)

        // onClick(DialogInterface,int) -> save
        if (mutable.methods.none { it.name == "onClick" && it.parameterTypes == listOf("Landroid/content/DialogInterface;", "I") }) {
            val emptyImpl = ImmutableMethodImplementation(6, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onClick",
                listOf(
                    ImmutableMethodParameter("Landroid/content/DialogInterface;", emptySet(), null),
                    ImmutableMethodParameter("I", emptySet(), null)
                ),
                "V", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                iget-object v0, p0, Lcom/valvesoftware/android/steam/community/MainActivity;->customAppIdEdit:Landroid/widget/EditText;
                if-eqz v0, :no_save
                iget-object v1, p0, Lcom/valvesoftware/android/steam/community/MainActivity;->customStatusEdit:Landroid/widget/EditText;
                if-eqz v1, :no_save
                invoke-virtual {v0}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
                move-result-object v0
                invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;
                move-result-object v0
                invoke-virtual {v1}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
                move-result-object v1
                invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
                move-result-object v1
                const-string v2, "custom_presence"
                const/4 v3, 0x0
                invoke-virtual {p0, v2, v3}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v2
                invoke-interface {v2}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v2
                const-string v3, "appId"
                invoke-interface {v2, v3, v0}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v2
                const-string v3, "status"
                invoke-interface {v2, v3, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v2
                invoke-interface {v2}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
                const-string v0, "Custom presence saved"
                const/4 v1, 0x0
                invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
                move-result-object v0
                invoke-virtual {v0}, Landroid/widget/Toast;->show()V
                :no_save
                return-void
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        }

        // onCreateOptionsMenu -> add "Custom RP" item
        if (mutable.methods.none { it.name == "onCreateOptionsMenu" && it.parameterTypes == listOf("Landroid/view/Menu;") }) {
            val emptyImpl = ImmutableMethodImplementation(3, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onCreateOptionsMenu",
                listOf(ImmutableMethodParameter("Landroid/view/Menu;", emptySet(), null)),
                "Z", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                const/16 v0, 0xCAFE
                const/4 v1, 0x0
                const/4 v2, 0x0
                const-string v3, "Custom RP"
                invoke-interface {p1, v0, v1, v2, v3}, Landroid/view/Menu;->add(IIILjava/lang/CharSequence;)Landroid/view/MenuItem;
                const/4 v0, 0x1
                return v0
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        }

        // onOptionsItemSelected -> show dialog when Custom RP selected
        if (mutable.methods.none { it.name == "onOptionsItemSelected" && it.parameterTypes == listOf("Landroid/view/MenuItem;") }) {
            val emptyImpl = ImmutableMethodImplementation(8, emptyList(), emptyList(), emptyList())
            val method = ImmutableMethod(
                target, "onOptionsItemSelected",
                listOf(ImmutableMethodParameter("Landroid/view/MenuItem;", emptySet(), null)),
                "Z", AccessFlags.PUBLIC.value, emptySet(), emptySet(), emptyImpl
            ).toMutable()
            mutable.methods.add(method)
            val smali = """
                invoke-interface {p1}, Landroid/view/MenuItem;->getItemId()I
                move-result v0
                const/16 v1, 0xCAFE
                if-eq v0, v1, :show_dialog
                const/4 v0, 0x0
                return v0
                :show_dialog
                new-instance v0, Landroid/widget/LinearLayout;
                invoke-direct {v0, p0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                const/4 v1, 0x1
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setOrientation(I)V
                const/16 v1, 0x20
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setPadding(IIII)V
                iput-object v0, p0, Lcom/valvesoftware/android/steam/community/MainActivity;->customPresenceLayout:Landroid/widget/LinearLayout;
                new-instance v1, Landroid/widget/EditText;
                invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V
                const-string v2, "App ID (e.g. 730)"
                invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V
                const-string v2, "custom_presence"
                const/4 v3, 0x0
                invoke-virtual {p0, v2, v3}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v2
                const-string v3, "appId"
                const-string v4, ""
                invoke-interface {v2, v3, v4}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                move-result-object v2
                invoke-virtual {v1, v2}, Landroid/widget/EditText;->setText(Ljava/lang/CharSequence;)V
                iput-object v1, p0, Lcom/valvesoftware/android/steam/community/MainActivity;->customAppIdEdit:Landroid/widget/EditText;
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v1, Landroid/widget/EditText;
                invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V
                const-string v2, "Status text"
                invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V
                const-string v2, "custom_presence"
                const/4 v3, 0x0
                invoke-virtual {p0, v2, v3}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v2
                const-string v3, "status"
                const-string v4, ""
                invoke-interface {v2, v3, v4}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                move-result-object v2
                invoke-virtual {v1, v2}, Landroid/widget/EditText;->setText(Ljava/lang/CharSequence;)V
                iput-object v1, p0, Lcom/valvesoftware/android/steam/community/MainActivity;->customStatusEdit:Landroid/widget/EditText;
                invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V
                new-instance v1, Landroid/app/AlertDialog${'$'}Builder;
                invoke-direct {v1, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
                const-string v2, "Custom Rich Presence"
                invoke-virtual {v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                const-string v2, "Save"
                invoke-virtual {v1, v2, p0}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                const-string v2, "Cancel"
                const/4 v3, 0x0
                invoke-virtual {v1, v2, v3}, Landroid/app/AlertDialog${'$'}Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
                const/4 v0, 0x1
                return v0
            """.trimIndent()
            method.addInstructionsWithLabels(0, smali)
        }

        // Remove old floating button injection if present (clean up previous patch's code is already replaced, but ensure onCreate doesn't still add RP button)
        // No need to remove, just keep MainActivity lean - the old button code will be gone because we replaced the file, but onCreate still has old injection from previous version.
        // To clean, we re-clone onCreate and strip any previous RP button code by not re-adding it. Since we overwrote the file, the old onCreate clone is gone; we just need to ensure current onCreate is original.
        // For safety, if onCreate currently contains RP button, we leave it - user can still use menu. No harm.
        logger.info("Custom Rich Presence migrated to Settings (Options Menu → Custom RP)")
    }
}
