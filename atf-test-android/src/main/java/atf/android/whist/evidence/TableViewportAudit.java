package atf.android.whist.evidence;

import atf.reporting.contract.AtfAssertion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import atf.android.whist.evidence.HandIndexAudit.Box;

/** Audits one observed viewport; observations from different scroll positions cannot be merged. */
public final class TableViewportAudit {
    public record PlayedRegion(String label, Box slot, Box face) {
        public PlayedRegion { Objects.requireNonNull(label); Objects.requireNonNull(slot); Objects.requireNonNull(face); }
    }

    public record Snapshot(Box viewport, Box board, Box hand, List<Box> seats, List<Box> handFaces,
            List<PlayedRegion> playedCards, boolean verticalScroll, boolean headerVisible) {
        public Snapshot {
            Objects.requireNonNull(viewport); Objects.requireNonNull(board); Objects.requireNonNull(hand);
            seats = List.copyOf(seats); handFaces = List.copyOf(handFaces); playedCards = List.copyOf(playedCards);
        }
    }

    public record Requirements(int players, int handCards, int trickCards,
            int handFaceHeight, int playedFaceHeight, int seatHeight) {
        public Requirements {
            if (players < 3 || players > 6 || handCards < 1 || handCards > 13 || trickCards < 0
                    || trickCards > players || handFaceHeight < 1 || playedFaceHeight < 1 || seatHeight < 1) {
                throw new IllegalArgumentException("Invalid table viewport requirements");
            }
        }
    }

    private TableViewportAudit() {}

    public static List<AtfAssertion> inspect(Snapshot state, Requirements expected) {
        var checks = new ArrayList<AtfAssertion>();
        checks.add(check("Game table has no vertical scroll container", "absent", !state.verticalScroll(), !state.verticalScroll()));
        checks.add(check("Game title and trump remain visible with the hand", "visible", state.headerVisible(), state.headerVisible()));
        boolean frames = state.seats().size() == expected.players()
                && state.seats().stream().allMatch(seat -> positive(seat) && height(seat) >= expected.seatHeight()
                        && state.board().contains(seat) && state.viewport().contains(seat))
                && maximumHeight(state.seats()) - minimumHeight(state.seats()) <= 1;
        checks.add(check("Complete seat frames in the same viewport", expected.players() + " complete frames",
                "frames=" + state.seats().size() + "; heights=" + heights(state.seats()), frames));
        long rows = state.handFaces().stream().map(Box::top).distinct().count();
        boolean hand = state.handFaces().size() == expected.handCards() && rows == 1
                && state.handFaces().stream().allMatch(face -> positive(face) && height(face) >= expected.handFaceHeight()
                        && state.hand().contains(face) && state.viewport().contains(face));
        checks.add(check("Whole hand retains complete faces on one row", expected.handCards() + " complete faces",
                "faces=" + state.handFaces().size() + "; heights=" + heights(state.handFaces()) + "; rows=" + rows, hand));
        int handLeft = state.handFaces().stream().mapToInt(Box::left).min().orElse(0);
        int handRight = state.handFaces().stream().mapToInt(Box::right).max().orElse(0);
        long centerError = Math.abs((long) handLeft + handRight - state.hand().left() - state.hand().right());
        checks.add(check("Hand row remains horizontally centered", "center delta <=1 px", centerError / 2.0 + " px",
                !state.handFaces().isEmpty() && centerError <= 2));
        boolean separate = positive(state.board()) && positive(state.hand())
                && state.viewport().contains(state.board()) && state.viewport().contains(state.hand())
                && state.board().bottom() <= state.hand().top();
        checks.add(check("Board and hand remain separate inside the display", "board above hand",
                "board bottom=" + state.board().bottom() + "; hand top=" + state.hand().top(), separate));
        checks.add(check("Completed trick contains every seat's card", expected.trickCards(),
                state.playedCards().size(), state.playedCards().size() == expected.trickCards()));
        var occupied = new ArrayList<>(state.seats());
        for (var card : state.playedCards()) {
            occupied.add(card.slot());
            boolean complete = positive(card.face()) && height(card.face()) >= expected.playedFaceHeight()
                    && card.slot().contains(card.face()) && state.viewport().contains(card.face());
            checks.add(check("Complete played-card face: " + card.label(), expected.playedFaceHeight() + " px minimum",
                    height(card.face()) + " px", complete));
        }
        int intersections = 0;
        for (int first = 0; first < occupied.size(); first++) for (int second = first + 1; second < occupied.size(); second++) {
            if (overlaps(occupied.get(first), occupied.get(second))) intersections++;
        }
        checks.add(check("Seats and all trick cards stay distinct", "no intersections",
                "regions=" + occupied.size() + "; intersections=" + intersections, intersections == 0));
        boolean contained = occupied.stream().allMatch(box -> positive(box) && state.board().contains(box));
        checks.add(check("All occupied regions remain inside the board", "contained", contained, contained));
        return List.copyOf(checks);
    }

    private static AtfAssertion check(String name, Object expected, Object actual, boolean passed) {
        return new AtfAssertion(name, String.valueOf(expected), String.valueOf(actual), passed);
    }
    private static boolean positive(Box box) { return box.right() > box.left() && box.bottom() > box.top(); }
    private static int height(Box box) { return box.bottom() - box.top(); }
    private static int minimumHeight(List<Box> boxes) { return boxes.stream().mapToInt(TableViewportAudit::height).min().orElse(0); }
    private static int maximumHeight(List<Box> boxes) { return boxes.stream().mapToInt(TableViewportAudit::height).max().orElse(0); }
    private static String heights(List<Box> boxes) { return minimumHeight(boxes) + ".." + maximumHeight(boxes) + " px"; }
    private static boolean overlaps(Box first, Box second) {
        return first.left() < second.right() && first.right() > second.left()
                && first.top() < second.bottom() && first.bottom() > second.top();
    }
}
