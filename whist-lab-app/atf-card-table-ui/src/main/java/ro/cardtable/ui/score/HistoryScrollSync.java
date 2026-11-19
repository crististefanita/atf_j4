package ro.cardtable.ui.score;

/** The same two-way scroll policy is exercised with JVM ports and Android panes. */
public final class HistoryScrollSync {
    public interface Pane { int position(); void scrollTo(int x); }
    private boolean updating;
    public void follow(Pane target, int x) {
        if (updating || target.position() == x) return;
        updating = true;
        try { target.scrollTo(x); } finally { updating = false; }
    }
}
