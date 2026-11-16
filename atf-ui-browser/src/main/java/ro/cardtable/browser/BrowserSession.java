package ro.cardtable.browser;

import java.util.HashMap;
import java.util.Map;

/** One in-memory match per browser tab. Refreshes can read the latest accepted revision. */
final class BrowserSession {
    record State(long revision, BrowserGame.View view, BrowserExperience.Details profile,
                 BrowserGame.CollectionCue collection) { }

    private final BrowserExperience profile;
    private long revision;
    private BrowserGame.Match match;
    private BrowserGame.View view;

    BrowserSession(BrowserExperience profile) { this.profile = profile; }

    synchronized State state() {
        return response(null);
    }

    private State response(BrowserGame.CollectionCue collection) {
        return new State(revision, view, profile == null ? null : profile.details(match), collection);
    }

    synchronized State start(BrowserGame game, long expected, Map<String, String> supplied) {
        checkRevision(expected);
        var configuration = new HashMap<String, String>();
        for (var field : game.fields()) {
            String initial = profile != null && field.key().equals("name") ? profile.details(null).name() : field.initial();
            String value = supplied.getOrDefault(field.key(), initial);
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
        if (profile != null) profile.completedAction(match);
        return response(match.collection());
    }

    synchronized State rename(long expected, String name) {
        checkRevision(expected);
        if (profile == null) throw new IllegalArgumentException("Profile unavailable");
        profile.rename(match, name);
        if (match != null) view = match.view();
        revision++;
        return state();
    }

    synchronized State dismissNotice(long expected) {
        checkRevision(expected);
        if (profile == null) throw new IllegalArgumentException("Notices unavailable");
        profile.dismissNotice();
        revision++;
        return state();
    }

    private void checkRevision(long expected) {
        if (expected != revision) throw new StaleRevision();
    }

    static final class StaleRevision extends RuntimeException { }
}
