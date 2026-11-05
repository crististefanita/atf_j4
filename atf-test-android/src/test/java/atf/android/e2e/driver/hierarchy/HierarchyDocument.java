package atf.android.e2e.driver.hierarchy;

import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Decodează o singură observație XML și coordonatele ei, fără acces la dispozitiv. */
public final class HierarchyDocument {
    private HierarchyDocument() {}

    private static final Pattern BOUNDS = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");

    public static Matcher matchBounds(String value) {
        return BOUNDS.matcher(value);
    }

    public static int[] bounds(String value) {
        Matcher matcher = matchBounds(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid UI bounds: " + value);
        return new int[] {
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            Integer.parseInt(matcher.group(4))
        };
    }

    public static Bounds toBounds(String value) {
        int[] parsed = bounds(value);
        return new Bounds(parsed[0], parsed[1], parsed[2], parsed[3]);
    }

    public static List<Node> nodes(String xml) throws Exception {
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
                            CardLabelText.normalize(element.getAttribute("text")),
                            element.getAttribute("class"),
                            element.getAttribute("bounds"),
                            Boolean.parseBoolean(element.getAttribute("clickable")),
                            Boolean.parseBoolean(element.getAttribute("enabled")),
                            Boolean.parseBoolean(element.getAttribute("selected")),
                            Boolean.parseBoolean(element.getAttribute("scrollable")),
                            element.getAttribute("content-desc"),
                            element.getParentNode() instanceof Element parent
                                    ? parent.getAttribute("bounds")
                                    : ""));
        }
        return result;
    }

    public record Node(
            String text,
            String className,
            String bounds,
            boolean clickable,
            boolean enabled,
            boolean selected,
            boolean scrollable,
            String description,
            String parentBounds) {}
}
