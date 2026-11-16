package ro.cardtable.browser;

import ro.cardtable.ui.hand.HandLayoutMetrics;
import ro.cardtable.ui.table.TableGeometry;
import ro.whistlab.app.achievements.presentation.WhistAchievementPresentation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Writes only public DTOs. TeaVM does not need runtime reflection on the game model. */
final class PageJson {
    private PageJson() { }

    static Map<String, Object> fields(Object... pairs) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    static String encode(Object value) {
        var out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof String text) {
            BrowserJson.quote(out, text);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            String separator = "";
            for (var entry : map.entrySet()) {
                out.append(separator);
                BrowserJson.quote(out, (String) entry.getKey());
                out.append(':');
                append(out, entry.getValue());
                separator = ",";
            }
            out.append('}');
        } else if (value instanceof Iterable<?> list) {
            out.append('[');
            String separator = "";
            for (var item : list) {
                out.append(separator);
                append(out, item);
                separator = ",";
            }
            out.append(']');
        } else {
            append(out, projection(value));
        }
    }

    private static Map<String, Object> projection(Object value) {
        if (value instanceof BrowserSession.State state)
            return fields("revision", state.revision(), "view", state.view(), "profile", state.profile(),
                    "collections", state.collections());
        if (value instanceof BrowserGame.View view)
            return fields("heading", view.heading(), "subtitle", view.subtitle(), "prompt", view.prompt(),
                    "seats", view.seats(), "hand", view.hand(), "played", view.played(), "center", view.center(),
                    "actions", view.actions(), "history", view.history());
        if (value instanceof BrowserGame.Field field)
            return fields("key", field.key(), "label", field.label(), "initial", field.initial(), "options", field.options());
        if (value instanceof BrowserGame.Option option)
            return fields("value", option.value(), "label", option.label());
        if (value instanceof BrowserGame.Action action)
            return fields("key", action.key(), "label", action.label(), "emphasized", action.emphasized(),
                    "minimum", action.minimum(), "maximum", action.maximum());
        if (value instanceof BrowserGame.Card card)
            return fields("rank", card.rank(), "suit", card.suit(), "label", card.label(), "action", card.action());
        if (value instanceof BrowserGame.Seat seat)
            return fields("anchor", seat.anchor(), "name", seat.name(), "firstFact", seat.firstFact(),
                    "secondFact", seat.secondFact(), "active", seat.active());
        if (value instanceof BrowserGame.Played played) return fields("seat", played.seat(), "card", played.card());
        if (value instanceof BrowserGame.History history) return fields("headings", history.headings(), "rows", history.rows());
        if (value instanceof BrowserGame.CollectionCue cue)
            return fields("winningSeat", cue.winningSeat(), "displayMillis", cue.displayMillis(),
                    "travelMillis", cue.travelMillis(), "cards", cue.cards());
        if (value instanceof BrowserGeometry.Layout layout) return fields("board", layout.board(), "hand", layout.hand());
        if (value instanceof TableGeometry.Plan plan) return fields("seats", plan.seats(), "center", plan.center());
        if (value instanceof TableGeometry.Rect rect)
            return fields("left", rect.left(), "top", rect.top(), "right", rect.right(), "bottom", rect.bottom());
        if (value instanceof HandLayoutMetrics.Fan fan)
            return fields("width", fan.width(), "first", fan.first(), "advance", fan.advance(), "cardWidth", fan.cardWidth());
        if (value instanceof BrowserExperience.Details details)
            return fields("name", details.name(), "savedName", details.savedName(), "catalogue", details.catalogue(),
                    "notice", details.notice());
        if (value instanceof WhistAchievementPresentation.Catalogue catalogue)
            return fields("summary", catalogue.summary(), "groups", catalogue.groups());
        if (value instanceof WhistAchievementPresentation.GroupSection group)
            return fields("number", group.number(), "heading", group.heading(), "recommended", group.recommended(),
                    "goals", group.goals());
        if (value instanceof WhistAchievementPresentation.GoalRow goal)
            return fields("id", goal.id(), "marker", goal.marker(), "level", goal.level(), "title", goal.title(),
                    "progress", goal.progress(), "description", goal.description(), "complete", goal.complete());
        if (value instanceof WhistAchievementPresentation.Notice notice)
            return fields("title", notice.title(), "summary", notice.summary(), "goals", notice.goals());
        throw new IllegalArgumentException("Unsupported public browser value");
    }
}
