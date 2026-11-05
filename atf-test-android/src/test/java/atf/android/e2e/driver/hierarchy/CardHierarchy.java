package atf.android.e2e.driver.hierarchy;

import static atf.android.e2e.driver.hierarchy.HierarchyDocument.bounds;
import static atf.android.e2e.driver.hierarchy.HierarchyDocument.nodes;
import static atf.android.e2e.driver.hierarchy.HierarchyDocument.toBounds;

import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.AndroidUiDriver.CardLabel;
import atf.android.e2e.driver.hierarchy.HierarchyDocument.Node;
import java.util.List;

/** Interpretează etichetele, fețele și starea accesibilă a cărților din observație. */
public final class CardHierarchy {
    private CardHierarchy() {}

    public static String firstLegalCardDescription(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(Node::clickable)
                .filter(node -> node.description().contains("Legal card"))
                .map(Node::description)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No clickable legal card is exposed"));
    }

    public static List<String> legalCardDescriptions(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(Node::clickable)
                .filter(node -> node.description().contains("Legal card"))
                .map(Node::description)
                .distinct()
                .toList();
    }

    public static boolean legalCardIsClickable(String xml, String cardLabel) throws Exception {
        String accessibleName = CardLabelText.accessibleName(cardLabel);
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

    public static List<CardLabel> bottomRowCardLabels(String xml) throws Exception {
        List<CardLabel> cards =
                nodes(xml).stream()
                        .filter(node -> CardLabelText.isPlayingCard(node.text()))
                        .map(node -> new CardLabel(node.text(), bounds(node.bounds())[1]))
                        .toList();
        if (cards.isEmpty()) return List.of();
        int bottomRowTop = cards.stream().mapToInt(CardLabel::top).max().orElseThrow();
        return cards.stream().filter(card -> card.top() >= bottomRowTop - 15).toList();
    }

    public static List<Bounds> playingCardLabelBounds(String xml) throws Exception {
        return nodes(xml).stream()
                .filter(node -> CardLabelText.isPlayingCard(node.text()))
                .map(node -> toBounds(node.bounds()))
                .toList();
    }

    public static boolean turnedTrumpIdentityMatchesVisibleCard(String xml) throws Exception {
        var entries = nodes(xml);
        var tiles =
                entries.stream()
                        .filter(n -> n.description().startsWith("Trump set by turned card: "))
                        .toList();
        if (tiles.size() != 1) return false;
        var tile = tiles.getFirst();
        return entries.stream()
                .filter(n -> CardLabelText.isPlayingCard(n.text()))
                .anyMatch(
                        n ->
                                n.parentBounds().equals(tile.bounds())
                                        && tile.description()
                                                .endsWith(CardLabelText.accessibleName(n.text())));
    }

    public static List<Bounds> handCardFaceBounds(String xml) throws Exception {
        var labels =
                nodes(xml).stream()
                        .filter(node -> CardLabelText.isPlayingCard(node.text()))
                        .toList();
        int bottom =
                labels.stream().mapToInt(node -> toBounds(node.bounds()).top()).max().orElse(0);
        return labels.stream()
                .filter(node -> toBounds(node.bounds()).top() >= bottom - 15)
                .map(node -> toBounds(node.parentBounds()))
                .toList();
    }
}
