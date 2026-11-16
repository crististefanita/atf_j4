package ro.cardtable.browser;

import java.util.HashMap;
import java.util.Map;

/** One in-memory match per browser tab. Refreshes can read the latest accepted revision. */
final class BrowserSession {
    record State(long revision, BrowserGame.View view) { }

    private long revision;
    private BrowserGame.Match match;
    private BrowserGame.View view;

    synchronized State state() {
        return new State(revision, view);
    }

    synchronized State start(BrowserGame game, long expected, Map<String, String> supplied) {
        checkRevision(expected);
        var configuration = new HashMap<String, String>();
        for (var field : game.fields()) {
            String value = supplied.getOrDefault(field.key(), field.initial());
            if (value.length() > 100 || (!field.options().isEmpty()
                    && field.options().stream().noneMatch(option -> value.equals(option.value())))) {
                throw new IllegalArgumentException("Invalid configuration: " + field.key());
            }
            configuration.put(field.key(), value);
        }
        var next = game.start(Map.copyOf(configuration));
        var nextView = next.view();
        match = next;
        view = nextView;
        revision++;
        return state();
    }

    synchronized State act(long expected, String key) {
        checkRevision(expected);
        if (view == null || !view.accepts(key)) throw new IllegalArgumentException("Action unavailable");
        try {
            match.act(key);
            view = match.view();
        } catch (RuntimeException failure) {
            // An unexpected engine failure may follow a partially committed turn.
            // Refuse to retry it against mutable state with the old revision.
            match = null;
            view = null;
            throw new IllegalStateException("Match stopped after a game error", failure);
        } finally {
            revision++;
        }
        return state();
    }

    private void checkRevision(long expected) {
        if (expected != revision) throw new StaleRevision();
    }

    static final class StaleRevision extends RuntimeException { }
}
