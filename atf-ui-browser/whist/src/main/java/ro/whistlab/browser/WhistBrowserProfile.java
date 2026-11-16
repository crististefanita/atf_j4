package ro.whistlab.browser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import ro.cardtable.browser.BrowserExperience;
import ro.cardtable.browser.BrowserGame;
import ro.whistlab.app.achievements.WhistAchievementRepository;
import ro.whistlab.app.achievements.presentation.WhistAchievementPresentation;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.player.PlayerNames;
import ro.whistlab.core.rules.HowToPlay;

final class WhistBrowserProfile implements BrowserExperience {
    private final WhistBrowserStorage name;
    private final WhistAchievementRepository achievements;
    private final WhistUiText copy = new WhistUiText(HowToPlay.Language.RO);

    WhistBrowserProfile(Path storage, String client) {
        name = new WhistBrowserStorage(storage == null ? null : storage.resolve(client + "-name.txt"));
        achievements = new WhistAchievementRepository(
                new WhistBrowserStorage(storage == null ? null : storage.resolve(client + "-achievements.json")));
    }

    @Override public Details details(BrowserGame.Match match) {
        try {
            var progress = achievements.state();
            var catalogue = WhistAchievementPresentation.catalogue(progress, copy);
            var goals = progress.pendingGoals();
            String saved = name.read();
            String display = saved == null || saved.isBlank() ? PlayerNames.DEFAULT_HUMAN : saved;
            var table = (WhistMatch) match;
            var notice = table == null || goals.isEmpty() ? null : WhistAchievementPresentation.notice(
                    goals, progress, table.handNumber(), table.finished(), copy);
            return new Details(display, catalogue, notice);
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist profile could not be read", failure);
        }
    }

    @Override public void rename(BrowserGame.Match match, String value) {
        String normalized = value.isBlank() ? "" : PlayerNames.normalize(value);
        String display = normalized.isEmpty() ? PlayerNames.DEFAULT_HUMAN : normalized;
        var table = (WhistMatch) match;
        if (table != null) table.validateName(display);
        try {
            name.write(normalized);
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist profile could not be saved", failure);
        }
        if (table != null) table.rename(display);
    }

    @Override public void completedAction(BrowserGame.Match match) {
        try {
            ((WhistMatch) match).recordAchievements(achievements);
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist achievements could not be saved", failure);
        }
    }

    @Override public void dismissNotice() {
        try {
            achievements.acknowledgeNotifications(achievements.pendingNotifications().stream()
                    .map(goal -> goal.id()).toList());
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist notice could not be dismissed", failure);
        }
    }
}
