package ro.whistlab.app.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import java.util.function.Consumer;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.player.PlayerNames;

public final class PlayerSettingsDialog {
    private PlayerSettingsDialog() { }
    public static AlertDialog show(Context context, WhistUiText copy, String current, Consumer<String> save) {
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PlayerNames.MAX_LENGTH)});
        input.setText(current);
        input.setHint(copy.nameHint());
        input.setContentDescription(copy.playerSettings());
        input.setTag("player-name");
        int pad = Math.round(20 * context.getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(copy.playerSettings()).setView(input)
                .setPositiveButton(copy.save(), null).setNeutralButton(copy.resetName(), null)
                .setNegativeButton(copy.close(), null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                try { save.accept(PlayerNames.normalize(input.getText().toString())); dialog.dismiss(); }
                catch (IllegalArgumentException invalid) { input.setError(copy.nameError()); }
                catch (IllegalStateException failure) { input.setError(copy.storageError()); }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                try { save.accept(""); dialog.dismiss(); }
                catch (IllegalStateException failure) { input.setError(copy.storageError()); }
            });
        });
        dialog.show();
        return dialog;
    }
}
