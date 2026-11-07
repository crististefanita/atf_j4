package ro.cardtable.ui.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class PlayerNameDialog {
    private PlayerNameDialog() { }
    public record Text(String playerSettings, String nameHint, String save, String resetName, String close,
            String nameError, String storageError) { }
    public static AlertDialog show(Context context, Text copy, String current, int maximumLength,
            UnaryOperator<String> normalize, Consumer<String> save) {
        NameEdit edit=new NameEdit(normalize,save);
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maximumLength)});
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
                try { edit.save(input.getText().toString()); dialog.dismiss(); }
                catch (IllegalArgumentException invalid) { input.setError(copy.nameError()); }
                catch (IllegalStateException failure) { input.setError(copy.storageError()); }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                try { edit.reset(); dialog.dismiss(); }
                catch (IllegalStateException failure) { input.setError(copy.storageError()); }
            });
        });
        dialog.show();
        return dialog;
    }
}
