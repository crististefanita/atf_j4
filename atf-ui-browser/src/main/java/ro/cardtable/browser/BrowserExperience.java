package ro.cardtable.browser;

/** Optional player-facing features owned by an individual game. */
public interface BrowserExperience {
    record Details(String name, Object catalogue, Object notice) { }

    Details details(BrowserGame.Match match);
    void rename(BrowserGame.Match match, String name);
    void completedAction(BrowserGame.Match match);
    void dismissNotice();
}
