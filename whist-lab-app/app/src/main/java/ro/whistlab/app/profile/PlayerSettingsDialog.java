package ro.whistlab.app.profile;

import android.app.AlertDialog;
import android.content.Context;
import java.util.function.Consumer;
import ro.cardtable.ui.profile.PlayerNameDialog;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.player.PlayerNames;

/** Whist keeps its validation and persistence; widgets are shared unchanged. */
public final class PlayerSettingsDialog {
    private PlayerSettingsDialog() { }
    public static AlertDialog show(Context context, WhistUiText copy, String current, Consumer<String> save) {
        var text=new PlayerNameDialog.Text(copy.playerSettings(),copy.nameHint(),copy.save(),copy.resetName(),
                copy.close(),copy.nameError(),copy.storageError());
        return PlayerNameDialog.show(context,text,current,PlayerNames.MAX_LENGTH,PlayerNames::normalize,save);
    }
}
