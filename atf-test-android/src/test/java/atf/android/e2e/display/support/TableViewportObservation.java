package atf.android.e2e.display.support;

import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.whist.evidence.HandIndexAudit.Box;
import atf.android.whist.evidence.TableViewportAudit;
import atf.reporting.contract.AtfAssertion;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import static atf.android.e2e.display.support.ResponsiveGeometry.box;

/** Decodes the English diagnostic fixture from one immutable hierarchy; never acquires device data. */
public final class TableViewportObservation {
    private TableViewportObservation() { }

    public static List<AtfAssertion> inspect(AndroidUiDriver ui,String xml,ResponsiveDisplayProfile profile,
            int handCards,boolean completeTrick) throws Exception {
        double density=profile.density()/160.0;
        var expected=new TableViewportAudit.Requirements(4,handCards,completeTrick?4:0,
                (int)Math.round(98*density),(int)Math.round(profile.playedCardFaceHeightDp()*density),
                (int)Math.floor(44*density));
        return TableViewportAudit.inspect(decode(ui,xml,profile),expected);
    }

    public static TableViewportAudit.Snapshot decode(AndroidUiDriver ui,String xml,
            ResponsiveDisplayProfile profile) throws Exception {
        Box display=new Box(0,0,profile.width(),profile.height());
        Box viewport=contentBounds(xml,display);
        var seats=new ArrayList<Box>();
        var played=new ArrayList<TableViewportAudit.PlayedRegion>();
        for(String description:ui.descriptions(xml)) {
            if(description.contains(". Bid ")&&description.contains(". Score "))
                seats.add(box(ui.boundsForDescription(xml,description)));
            if(description.contains(" played ")) {
                String label=description.substring(description.indexOf(" played ")+" played ".length());
                played.add(new TableViewportAudit.PlayedRegion(label,box(ui.boundsForDescription(xml,description)),
                        box(ui.boundsForText(xml,shortLabel(label)))));
            }
        }
        boolean header=false;
        if(ui.hasText(xml,"Whist · Descending")) {
            var trumps=ui.descriptions(xml).stream().filter(value->value.startsWith("Trump")).toList();
            header=complete(viewport,box(ui.boundsForText(xml,"Whist · Descending")))&&!trumps.isEmpty();
            for(String description:trumps)header&=complete(viewport,box(ui.boundsForDescription(xml,description)));
        }
        return new TableViewportAudit.Snapshot(viewport,box(ui.boundsForDescription(xml,"Playing surface")),
                box(ui.boundsForDescriptionContaining(xml,"Your hand,")),seats,
                ui.handCardFaceBounds(xml).stream().map(ResponsiveGeometry::box).toList(),played,
                ui.hasClass(xml,"android.widget.ScrollView"),header);
    }

    private static Box contentBounds(String xml,Box display) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var nodes=factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getElementsByTagName("node");
        Box content=null;
        for(int index=0;index<nodes.getLength();index++) {
            var node=(org.w3c.dom.Element)nodes.item(index);
            if(!node.getAttribute("resource-id").equals("android:id/content"))continue;
            if(content!=null)throw new IllegalArgumentException("Ambiguous Android content viewport");
            var bounds=java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").matcher(node.getAttribute("bounds"));
            if(!bounds.matches())throw new IllegalArgumentException("Malformed Android content viewport");
            content=new Box(Integer.parseInt(bounds.group(1)),Integer.parseInt(bounds.group(2)),
                    Integer.parseInt(bounds.group(3)),Integer.parseInt(bounds.group(4)));
            if(!complete(display,content))throw new IllegalArgumentException("Android content is outside the declared display");
        }
        return content==null?display:content;
    }

    private static boolean complete(Box parent,Box child) {
        return child.right()>child.left()&&child.bottom()>child.top()&&parent.contains(child);
    }
    private static String shortLabel(String card) {
        String[] parts=card.split(" of ",2);
        if(parts.length!=2)throw new IllegalArgumentException("Malformed played-card description: "+card);
        String rank=switch(parts[0]){case "Ace"->"A";case "King"->"K";case "Queen"->"Q";case "Jack"->"J";default->parts[0];};
        String suit=switch(parts[1]){case "spades"->"♠";case "hearts"->"♥";case "clubs"->"♣";case "diamonds"->"♦";
            default->throw new IllegalArgumentException("Unknown fixture card: "+card);};
        return rank+suit;
    }
}
