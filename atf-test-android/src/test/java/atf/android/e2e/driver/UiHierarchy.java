package atf.android.e2e.driver;

import static atf.android.e2e.driver.hierarchy.HierarchyDocument.bounds;
import static atf.android.e2e.driver.hierarchy.HierarchyDocument.nodes;
import static atf.android.e2e.driver.hierarchy.HierarchyDocument.toBounds;

import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.AndroidUiDriver.ElementSize;
import atf.android.e2e.driver.hierarchy.HierarchyDocument.Node;
import java.util.List;

/** Selectori generici pentru o observație decodată, fără comenzi sau reluarea capturii. */
final class UiHierarchy {
    private UiHierarchy() {}

    static boolean hasText(String xml, String exactText) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.text().equals(exactText));
    }

    static boolean hasTextContaining(String xml, String fragment) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.text().contains(fragment));
    }

    static boolean hasDescription(String xml, String exactDescription) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.description().equals(exactDescription));
    }

    static boolean hasDescriptionContaining(String xml, String fragment) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.description().contains(fragment));
    }

    static List<String> descriptions(String xml) throws Exception {
        return nodes(xml).stream()
                .map(Node::description)
                .filter(value -> !value.isBlank())
                .toList();
    }

    static Bounds boundsForText(String xml, String exactText) throws Exception {
        List<Node> matches =
                nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1)
            throw new IllegalStateException(
                    "Expected one UI element named '" + exactText + "', got " + matches.size());
        return toBounds(matches.getFirst().bounds());
    }

    static Bounds boundsForDescription(String xml, String exactDescription) throws Exception {
        List<Node> matches =
                nodes(xml).stream()
                        .filter(node -> node.description().equals(exactDescription))
                        .toList();
        List<String> distinctBounds = matches.stream().map(Node::bounds).distinct().toList();
        if (distinctBounds.size() != 1)
            throw new IllegalStateException(
                    "Expected one UI region described as '"
                            + exactDescription
                            + "', got "
                            + distinctBounds.size()
                            + " distinct bounds across "
                            + matches.size()
                            + " matching nodes");
        return toBounds(distinctBounds.getFirst());
    }

    static Bounds boundsForDescription(String xml, String exactDescription, String exactClassName)
            throws Exception {
        List<Node> matches =
                nodes(xml).stream()
                        .filter(node -> node.description().equals(exactDescription))
                        .filter(node -> node.className().equals(exactClassName))
                        .toList();
        if (matches.size() != 1)
            throw new IllegalStateException(
                    "Expected one UI element described as '"
                            + exactDescription
                            + "' with class "
                            + exactClassName
                            + ", got "
                            + matches.size());
        return toBounds(matches.getFirst().bounds());
    }

    static String spinnerValue(String xml, String exactDescription) throws Exception {
        List<Node> controls =
                nodes(xml).stream()
                        .filter(node -> node.className().equals("android.widget.Spinner"))
                        .filter(node -> node.description().equals(exactDescription))
                        .toList();
        if (controls.size() != 1) {
            throw new IllegalStateException(
                    "Expected one spinner described as '"
                            + exactDescription
                            + "', got "
                            + controls.size());
        }
        Bounds control = toBounds(controls.getFirst().bounds());
        List<String> values =
                nodes(xml).stream()
                        .filter(node -> !node.text().isBlank())
                        .filter(node -> !node.className().equals("android.widget.Spinner"))
                        .filter(
                                node -> {
                                    Bounds value = toBounds(node.bounds());
                                    return value.left() >= control.left()
                                            && value.top() >= control.top()
                                            && value.right() <= control.right()
                                            && value.bottom() <= control.bottom();
                                })
                        .map(Node::text)
                        .distinct()
                        .toList();
        if (values.size() != 1) {
            throw new IllegalStateException(
                    "Expected one visible value inside spinner '"
                            + exactDescription
                            + "', got "
                            + values);
        }
        return values.getFirst();
    }

    static Bounds boundsForDescriptionContaining(String xml, String fragment) throws Exception {
        List<Node> matches =
                nodes(xml).stream().filter(node -> node.description().contains(fragment)).toList();
        if (matches.size() != 1)
            throw new IllegalStateException(
                    "Expected one UI element described with '"
                            + fragment
                            + "', got "
                            + matches.size());
        return toBounds(matches.getFirst().bounds());
    }

    static boolean hasClass(String xml, String exactClass) throws Exception {
        return nodes(xml).stream().anyMatch(node -> node.className().equals(exactClass));
    }

    static boolean allContentFitsViewport(String xml) throws Exception {
        List<Node> elements = nodes(xml);
        Node viewport =
                elements.stream()
                        .filter(node -> node.className().equals("android.widget.ScrollView"))
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalStateException("Screen has no scroll viewport"));
        if (viewport.scrollable()) return false;
        int[] viewportBounds = bounds(viewport.bounds());
        return elements.stream()
                .filter(
                        node ->
                                !node.text().isBlank()
                                        || !node.description().isBlank()
                                        || node.clickable())
                .map(node -> bounds(node.bounds()))
                .allMatch(
                        element ->
                                element[0] >= viewportBounds[0]
                                        && element[1] >= viewportBounds[1]
                                        && element[2] <= viewportBounds[2]
                                        && element[3] <= viewportBounds[3]);
    }

    static boolean isEnabled(String xml, String exactText) throws Exception {
        List<Node> matches =
                nodes(xml).stream().filter(node -> node.text().equals(exactText)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected one UI element named '" + exactText + "', got " + matches.size());
        }
        return matches.getFirst().enabled();
    }

    static boolean isSelected(String xml, String exactText) throws Exception {
        List<Node> matches =
                nodes(xml).stream()
                        .filter(
                                node ->
                                        node.className().equals("android.widget.Button")
                                                && node.text().equals(exactText))
                        .toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected one UI element named '" + exactText + "', got " + matches.size());
        }
        return matches.getFirst().selected();
    }

    static List<String> disabledButtonTexts(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> !node.enabled())
                .map(Node::text)
                .filter(text -> !text.isBlank())
                .toList();
    }

    static List<String> texts(String xml) throws Exception {
        return nodes(xml).stream().map(Node::text).filter(text -> !text.isBlank()).toList();
    }

    static List<ElementSize> numericButtonSizes(String xml) throws Exception {
        return numericButtons(xml, false).stream()
                .map(
                        node -> {
                            int[] value = bounds(node.bounds());
                            return new ElementSize(value[2] - value[0], value[3] - value[1]);
                        })
                .toList();
    }

    static ElementSize buttonSize(String xml, String text) throws Exception {
        List<Node> matches =
                nodes(xml).stream()
                        .filter(node -> node.className().equals("android.widget.Button"))
                        .filter(node -> node.text().equals(text))
                        .toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected one button labeled '" + text + "', got " + matches.size());
        }
        int[] value = bounds(matches.getFirst().bounds());
        return new ElementSize(value[2] - value[0], value[3] - value[1]);
    }

    static List<Integer> numericButtonValues(String xml) throws Exception {
        return numericButtons(xml, false).stream()
                .map(node -> Integer.parseInt(node.text()))
                .toList();
    }

    static List<Integer> enabledNumericButtonValues(String xml) throws Exception {
        return numericButtons(xml, true).stream()
                .map(node -> Integer.parseInt(node.text()))
                .toList();
    }

    static boolean matchesSpinnerLabel(String selector, String label) {
        return switch (selector) {
            case "interface-language" -> List.of("Language", "Limba").contains(label);
            case "player-count" -> List.of("Players", "Jucători").contains(label);
            case "game-type" -> List.of("Game type", "Varianta de joc").contains(label);
            case "ai-level" -> List.of("AI level", "Nivelul adversarilor").contains(label);
            case "romanian-streak-value" ->
                    List.of(
                                    "Five-hand reward / penalty",
                                    "Bonus / penalizare după cinci mâini",
                                    "Five-hand exact-contract bonus",
                                    "Bonus pentru cinci contracte exacte eligibile")
                            .contains(label);
            case "bid-options" -> List.of("Bid value", "Valoarea licitației").contains(label);
            default -> selector.equals(label);
        };
    }

    static Bounds buttonBounds(String xml, String text) throws Exception {
        var matches =
                nodes(xml).stream()
                        .filter(
                                n ->
                                        n.className().equals("android.widget.Button")
                                                && n.text().equals(text))
                        .toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one button: " + text);
        return toBounds(matches.getFirst().bounds());
    }

    private static List<Node> numericButtons(String xml, boolean enabledOnly) throws Exception {
        return nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> !enabledOnly || node.enabled())
                .filter(node -> node.text().matches("\\d+"))
                .toList();
    }
}
