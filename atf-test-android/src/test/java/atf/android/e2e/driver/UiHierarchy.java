package atf.android.e2e.driver;

import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.AndroidUiDriver.CardLabel;
import atf.android.e2e.driver.AndroidUiDriver.ElementSize;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.regex.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/** Pure hierarchy parsing and selectors; never issues ADB commands or retries a previous hierarchy. */
final class UiHierarchy {
    private UiHierarchy() {}

    private static final Pattern BOUNDS = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");

    private static final Pattern PLAYING_CARD = Pattern.compile("(?:[2-9]|10|J|Q|K|A)[♠♥♣♦]");

    static Matcher matchBounds(String value) {
        return BOUNDS.matcher(value);
    }

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

    static String firstLegalCardDescription(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(Node::clickable)
                .filter(node -> node.description().contains("Legal card"))
                .map(Node::description)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No clickable legal card is exposed"));
    }

    static List<String> legalCardDescriptions(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(Node::clickable)
                .filter(node -> node.description().contains("Legal card"))
                .map(Node::description)
                .distinct()
                .toList();
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

    static boolean legalCardIsClickable(String xml, String cardLabel) throws Exception {
        String accessibleName = accessibleCardName(cardLabel);
        List<Node> cards =
                nodes(xml).stream()
                        .filter(node -> node.className().equals("android.widget.FrameLayout"))
                        .filter(node -> node.description().startsWith(accessibleName + ". "))
                        .filter(
                                node ->
                                        node.description().endsWith("Legal card")
                                                || node.description().endsWith("Not legal now"))
                        .toList();
        if (cards.size() != 1) {
            throw new IllegalStateException(
                    "Expected one accessible card slot for '"
                            + cardLabel
                            + "', got "
                            + cards.size());
        }
        Node card = cards.getFirst();
        boolean markedLegal = card.description().endsWith("Legal card");
        if (card.clickable() != markedLegal) {
            throw new IllegalStateException(
                    "Card accessibility state disagrees with clickability for "
                            + cardLabel
                            + ": "
                            + card.description()
                            + ", clickable="
                            + card.clickable());
        }
        return card.clickable();
    }

    static String accessibleCardName(String cardLabel) {
        if (cardLabel.length() < 2)
            throw new IllegalArgumentException("Invalid card label: " + cardLabel);
        String rank = cardLabel.substring(0, cardLabel.length() - 1);
        String rankName =
                switch (rank) {
                    case "J" -> "Jack";
                    case "Q" -> "Queen";
                    case "K" -> "King";
                    case "A" -> "Ace";
                    default -> rank;
                };
        String suit =
                switch (cardLabel.substring(cardLabel.length() - 1)) {
                    case "♠" -> "spades";
                    case "♥" -> "hearts";
                    case "♦" -> "diamonds";
                    case "♣" -> "clubs";
                    default ->
                            throw new IllegalArgumentException("Invalid card label: " + cardLabel);
                };
        return rankName + " of " + suit;
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

    static List<CardLabel> bottomRowCardLabels(String xml) throws Exception {
        List<CardLabel> cards =
                nodes(xml).stream()
                        .filter(node -> PLAYING_CARD.matcher(node.text()).matches())
                        .map(node -> new CardLabel(node.text(), bounds(node.bounds())[1]))
                        .toList();
        if (cards.isEmpty()) return List.of();
        int bottomRowTop = cards.stream().mapToInt(CardLabel::top).max().orElseThrow();
        return cards.stream().filter(card -> card.top() >= bottomRowTop - 15).toList();
    }

    static List<Bounds> playingCardLabelBounds(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> PLAYING_CARD.matcher(node.text()).matches())
                .map(node -> toBounds(node.bounds()))
                .toList();
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

    static int[] bounds(String value) {
        Matcher matcher = matchBounds(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid UI bounds: " + value);
        return new int[] {
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            Integer.parseInt(matcher.group(4))
        };
    }

    static Bounds toBounds(String value) {
        int[] parsed = bounds(value);
        return new Bounds(parsed[0], parsed[1], parsed[2], parsed[3]);
    }

    static boolean matchesSpinnerLabel(String selector, String label) {
        return switch (selector) {
            case "interface-language" -> List.of("Language", "Limba").contains(label);
            case "player-count" -> List.of("Players", "Jucători").contains(label);
            case "game-type" -> List.of("Game type", "Varianta de joc").contains(label);
            case "ai-level" -> List.of("AI level", "Nivelul adversarilor").contains(label);
            case "romanian-streak-value" ->
                    List.of(
                                    "Five-hand exact-contract bonus",
                                    "Bonus pentru cinci contracte exacte eligibile")
                            .contains(label);
            case "bid-options" -> List.of("Bid value", "Valoarea licitației").contains(label);
            default -> selector.equals(label);
        };
    }

    static List<Node> nodes(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document =
                factory.newDocumentBuilder()
                        .parse(
                                new java.io.ByteArrayInputStream(
                                        xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        NodeList elements = document.getElementsByTagName("node");
        List<Node> result = new ArrayList<>(elements.getLength());
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            result.add(
                    new Node(
                            cardText(element.getAttribute("text")),
                            element.getAttribute("class"),
                            element.getAttribute("bounds"),
                            Boolean.parseBoolean(element.getAttribute("clickable")),
                            Boolean.parseBoolean(element.getAttribute("enabled")),
                            Boolean.parseBoolean(element.getAttribute("selected")),
                            element.getAttribute("content-desc"),
                            element.getParentNode() instanceof Element parent
                                    ? parent.getAttribute("bounds")
                                    : ""));
        }
        return result;
    }

    static String cardText(String text) {
        String compact = text.replace("\n", "").replace(" ", "");
        return PLAYING_CARD.matcher(compact).matches() ? compact : text;
    }

    static boolean turnedTrumpIdentityMatchesVisibleCard(String xml) throws Exception {
        var entries = nodes(xml);
        var tiles =
                entries.stream()
                        .filter(n -> n.description().startsWith("Trump set by turned card: "))
                        .toList();
        if (tiles.size() != 1) return false;
        var tile = tiles.getFirst();
        return entries.stream()
                .filter(n -> PLAYING_CARD.matcher(n.text()).matches())
                .anyMatch(
                        n ->
                                n.parentBounds().equals(tile.bounds())
                                        && tile.description()
                                                .endsWith(accessibleCardName(n.text())));
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

    static List<Bounds> handCardFaceBounds(String xml) throws Exception {
        var labels =
                nodes(xml).stream()
                        .filter(node -> PLAYING_CARD.matcher(node.text()).matches())
                        .toList();
        int bottom =
                labels.stream().mapToInt(node -> toBounds(node.bounds()).top()).max().orElse(0);
        return labels.stream()
                .filter(node -> toBounds(node.bounds()).top() >= bottom - 15)
                .map(node -> toBounds(node.parentBounds()))
                .toList();
    }

    record Node(
            String text,
            String className,
            String bounds,
            boolean clickable,
            boolean enabled,
            boolean selected,
            String description,
            String parentBounds) {}

    private static List<Node> numericButtons(String xml, boolean enabledOnly) throws Exception {
        return nodes(xml).stream()
                .filter(node -> node.className().equals("android.widget.Button"))
                .filter(node -> !enabledOnly || node.enabled())
                .filter(node -> node.text().matches("\\d+"))
                .toList();
    }
}
