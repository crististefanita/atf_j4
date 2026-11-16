package ro.cardtable.browser;

/** Optional player-facing features owned by an individual game. */
public interface BrowserExperience {
    record Details(String name, boolean savedName, Object catalogue, Object notice) { }

    Details details(BrowserGame.Match match);
    default Details details(BrowserGame.Match match, String language) { return details(match); }
    void rename(BrowserGame.Match match, String name);
    void completedAction(BrowserGame.Match match);
    void dismissNotice();
}
