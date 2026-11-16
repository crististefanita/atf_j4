package ro.whistlab.browser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import ro.cardtable.browser.BrowserExperience;
import ro.cardtable.browser.BrowserGame;
import ro.whistlab.app.achievements.WhistAchievementRepository;
import ro.whistlab.app.achievements.presentation.WhistAchievementPresentation;
import ro.whistlab.core.player.PlayerNames;
import ro.whistlab.app.highscores.HighScores;
import ro.whistlab.app.highscores.HighScoreRepository;
import ro.whistlab.app.highscores.HighScorePresentation;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;

final class WhistBrowserProfile implements BrowserExperience {
    private final WhistAchievementRepository.Storage name;
    private final WhistAchievementRepository achievements;
    private final HighScoreRepository rankings;

    WhistBrowserProfile(Path storage, String client) {
        this(new WhistBrowserStorage(storage == null ? null : storage.resolve(client + "-name.txt")),
                new WhistBrowserStorage(storage == null ? null : storage.resolve(client + "-achievements.json")),
                new WhistBrowserStorage(storage == null ? null : storage.resolve(client + "-rankings.json")));
    }

    WhistBrowserProfile(WhistAchievementRepository.Storage name, WhistAchievementRepository.Storage progress,
                        HighScoreRepository.Storage records) {
        this.name = name;
        achievements = new WhistAchievementRepository(progress);
        rankings = new HighScoreRepository(records, new java.util.Random());
    }

    @Override public Details details(BrowserGame.Match match) { return details(match, "ro"); }

    @Override public synchronized Details details(BrowserGame.Match match, String language) {
        var copy = WhistBrowser.copy(language);
        try {
            var progress = achievements.state();
            var catalogue = WhistAchievementPresentation.catalogue(progress, copy);
            var goals = progress.pendingGoals();
            String saved = name.read();
            String display = saved == null || saved.isBlank() ? PlayerNames.DEFAULT_HUMAN : saved;
            var table = (WhistMatch) match;
            var notice = table == null || goals.isEmpty() ? null : WhistAchievementPresentation.notice(
                    goals, progress, table.handNumber(), table.finished(), copy);
            return new Details(copy.displayName(display, true), saved != null, catalogue, notice);
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist profile could not be read", failure);
        }
    }

    @Override public synchronized Rankings rankings(BrowserGame.Match match, String language) {
        var copy = WhistBrowser.copy(language);
        var config = match == null ? GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL,
                0, true, 10, true) : ((WhistMatch) match).config();
        var preferred = HighScores.Profile.from(config);
        try {
            rankings.board(preferred);
            return new Rankings(preferred.key(), rankings.boards().stream().map(board -> new RankBoard(
                    board.profile().key(), HighScorePresentation.profile(board.profile(), copy),
                    board.entries().stream().map(entry -> new RankEntry(HighScorePresentation.player(entry, copy),
                            entry.score(), entry.ai(), entry.sample())).toList())).toList());
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist rankings could not be read", failure);
        }
    }

    @Override public synchronized void rename(BrowserGame.Match match, String value) {
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

    @Override public synchronized void completedAction(BrowserGame.Match match) {
        try {
            var table = (WhistMatch) match;
            table.recordAchievements(achievements);
            table.recordRanking(rankings);
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist results could not be saved", failure);
        }
    }

    @Override public synchronized void dismissNotice() {
        try {
            achievements.acknowledgeNotifications(achievements.pendingNotifications().stream()
                    .map(goal -> goal.id()).toList());
        } catch (IOException failure) {
            throw new UncheckedIOException("Whist notice could not be dismissed", failure);
        }
    }
}
