import atf.android.whist.evidence.HandIndexAudit;
import atf.android.whist.evidence.HandIndexAudit.Box;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

public class WhistOfflineHandIndices {
    static Box box(Element node) {
        var m=java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").matcher(node.getAttribute("bounds"));
        if(!m.matches())throw new IllegalArgumentException("Missing captured bounds");
        return new Box(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4)));
    }
    public static void main(String[] args) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var doc=factory.newDocumentBuilder().parse(Path.of(args[0]).toFile());
        var image=ImageIO.read(Path.of(args[1]).toFile());
        var labels=new ArrayList<Element>();var nodes=doc.getElementsByTagName("node");
        for(int i=0;i<nodes.getLength();i++) {
            var n=(Element)nodes.item(i);
            if(n.getAttribute("text").matches("(?:10|[2-9AJQK])\\s*[♠♥♣♦]"))labels.add(n);
        }
        int bottom=labels.stream().mapToInt(n->box(n).top()).max().orElseThrow();
        labels.removeIf(n->box(n).top()<bottom-15);labels.sort(Comparator.comparingInt(n->box(n).left()));
        if(labels.size()!=13)throw new AssertionError("Expected all thirteen captured indices");
        var json=new StringBuilder("{\"status\":\"PASS\",\"transport\":\"offline\",\"indices\":[");
        for(int i=0;i<labels.size();i++) {
            var node=labels.get(i);var exposed=box((Element)node.getParentNode());
            var result=HandIndexAudit.inspect(image,720,1280,box(node),exposed);
            if(!result.readable())throw new AssertionError("Index "+i+": "+result);
            if(i>0)json.append(',');
            json.append("{\"index\":").append(i).append(",\"rankPixels\":").append(result.rankPixels())
                .append(",\"suitPixels\":").append(result.suitPixels()).append(",\"edgePixels\":").append(result.edgePixels()).append('}');
        }
        System.out.println(json.append("]}"));
    }
}
