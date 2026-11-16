package ro.cardtable.browser;

import java.util.List;

/** Optional player-facing features owned by an individual game. */
public interface BrowserExperience {
    record Details(String name, boolean savedName, Object catalogue, Object notice) { }

    record RankEntry(String label, int score, boolean ai, boolean sample) { }
    record RankBoard(String key, String label, List<RankEntry> entries) { }
    record Rankings(String preferred, List<RankBoard> boards) { }

    default Rankings rankings(BrowserGame.Match match, String language) { return null; }
    Details details(BrowserGame.Match match);
    default Details details(BrowserGame.Match match, String language) { return details(match); }
    void rename(BrowserGame.Match match, String name);
    void completedAction(BrowserGame.Match match);
    void dismissNotice();
}
