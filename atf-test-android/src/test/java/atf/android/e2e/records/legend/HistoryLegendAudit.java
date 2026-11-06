package atf.android.e2e.records.legend;

import atf.reporting.contract.AtfAssertion;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** Observes the caption's viewport and ownership, not merely its accessibility text. */
public final class HistoryLegendAudit {
    private HistoryLegendAudit() { }
    public record Box(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
    public record Snapshot(Box legend, Box columns, Box rows, Box close, boolean outsideColumns, String firstHeading) {
        public List<AtfAssertion> checks() {
            return List.of(check("Legend belongs to the fixed dialog region", true, outsideColumns),
                    new AtfAssertion("Table and close control retain nonempty viewports", "Positive width and height",
                            "columns=" + columns.width() + "x" + columns.height() + "; rows=" + rows.width() + "x" + rows.height()
                                    + "; close=" + close.width() + "x" + close.height(),
                            columns.width() > 0 && columns.height() > 0 && rows.width() > 0 && rows.height() > 0
                                    && close.width() > 0 && close.height() > 0),
                    new AtfAssertion("Legend has a nonempty rendered region", "Positive width and height", legend.toString(),
                            legend.width() > 0 && legend.height() > 0),
                    new AtfAssertion("Legend fits the horizontal viewport", "left >= " + columns.left() + ", right <= " + columns.right(), legend.toString(),
                            legend.left() >= columns.left() && legend.right() <= columns.right()),
                    new AtfAssertion("Legend remains below the row viewport", "top >= " + rows.bottom(), "top=" + legend.top(), legend.top() >= rows.bottom()),
                    new AtfAssertion("Legend does not intersect the close control", "bottom <= " + close.top(), "bottom=" + legend.bottom(), legend.bottom() <= close.top()));
        }
    }

    public static Snapshot inspect(String xml, String legend, String close, String firstPlayer) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        Element caption = unique(document.getDocumentElement(), "text", legend, false);
        Element button = unique(document.getDocumentElement(), "text", close, true);
        boolean outside = true;
        for (Node node = caption.getParentNode(); node instanceof Element element; node = node.getParentNode())
            if (element.getAttribute("class").equals("android.widget.HorizontalScrollView")) outside = false;
        Element columns = unique(document.getDocumentElement(), "class", "android.widget.HorizontalScrollView", false);
        Element rows = unique(columns, "class", "android.widget.ScrollView", false);
        var headings = matching(columns, "text", firstPlayer, false);
        String first = headings.isEmpty() ? "not visible" : headings.get(0).getAttribute("bounds");
        return new Snapshot(box(caption), box(columns), box(rows), box(button), outside, first);
    }

    public static AtfAssertion check(String name, Object expected, Object actual) {
        return new AtfAssertion(name, String.valueOf(expected), String.valueOf(actual), java.util.Objects.equals(expected, actual));
    }

    private static Element unique(Element scope, String attribute, String value, boolean ignoreCase) {
        var nodes = matching(scope, attribute, value, ignoreCase);
        if (nodes.size() != 1) throw new IllegalStateException("Expected one " + attribute + "=" + value + ", found " + nodes.size());
        return nodes.get(0);
    }

    private static List<Element> matching(Element scope, String attribute, String value, boolean ignoreCase) {
        var result = new ArrayList<Element>();
        var nodes = scope.getElementsByTagName("node");
        for (int i = 0; i < nodes.getLength(); i++) {
            var element = (Element) nodes.item(i);String actual = element.getAttribute(attribute);
            if (ignoreCase ? actual.equalsIgnoreCase(value) : actual.equals(value)) result.add(element);
        }
        return result;
    }

    private static Box box(Element element) {
        var m = java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").matcher(element.getAttribute("bounds"));
        if (!m.matches()) throw new IllegalArgumentException("Missing captured rectangle: " + element.getAttribute("bounds"));
        return new Box(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
    }
}
