package atf.android.e2e.release;

import atf.android.e2e.driver.hierarchy.HierarchyDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Reads one captured hierarchy; never polls, dispatches actions or infers an AI decision. */
final class BidChoiceObservation {
    record State(List<Integer> choices, OptionalInt selected, boolean humanTurn, boolean submitEnabled) {
        State { choices = List.copyOf(choices); }
    }

    static State read(String xml, String submitText) throws Exception {
        var nodes = HierarchyDocument.nodes(xml);
        var submit = nodes.stream().filter(node -> node.className().equals("android.widget.Button")
                && node.text().equals(submitText)).toList();
        if (submit.size() != 1) throw new IllegalStateException("Expected exactly one bid confirmation control");
        var buttons = nodes.stream().filter(node -> node.className().equals("android.widget.Button")
                && node.text().matches("-?[0-9]+")).toList();
        if (buttons.isEmpty()) throw new IllegalStateException("No bid choices in the current hierarchy");
        var choices = new ArrayList<Integer>(); var selected = new ArrayList<Integer>();
        boolean human = buttons.getFirst().enabled();
        for (var button : buttons) {
            int bid = Integer.parseInt(button.text());
            if (bid < 0 || bid > 8 || choices.contains(bid) || button.enabled() != human)
                throw new IllegalStateException("Ambiguous or inconsistent Romanian bid choices");
            choices.add(bid);
            if (button.selected()) selected.add(bid);
        }
        if (selected.size() > 1) throw new IllegalStateException("More than one bid is selected");
        boolean ready = human && selected.size() == 1;
        if (submit.getFirst().enabled() != ready)
            throw new IllegalStateException("Bid confirmation availability contradicts the observed selection");
        return new State(choices, selected.isEmpty() ? OptionalInt.empty() : OptionalInt.of(selected.getFirst()),
                human, submit.getFirst().enabled());
    }

    private BidChoiceObservation() { }
}
