package atf.tests.learning;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;

import org.jsoup.nodes.*;

/** Consumer audit of canonical scenario owners, ordered rows, typed context and actual execution timing. */
public final class LearningReportHtmlAudit {
    private static final ObjectMapper JSON=new ObjectMapper();
    private LearningReportHtmlAudit(){};
    public static long verify(Path html,List<LearningRunReport.Input> inputs)throws Exception{
        long comparisons=0;try(var reader=Files.newBufferedReader(html);var parser=new org.jsoup.parser.StreamParser(org.jsoup.parser.Parser.htmlParser()).parse(reader,"")){
            for(var input:inputs){var scenario=parser.selectNext("#test-view .test-list-item > li.test-item");require(scenario!=null,"Missing canonical invocation");assets(scenario);comparisons+=scenario(scenario,input);scenario.remove();}
            require(parser.selectNext("#test-view .test-list-item > li.test-item")==null,"Unexpected canonical invocation");assets(parser.document());
        }return comparisons;
    }
    public static long verify(Document doc,List<LearningRunReport.Input> inputs)throws Exception{
        var scenarios=doc.select("#test-view .test-list-item > li.test-item");require(scenarios.size()==inputs.size(),"Canonical invocation count");assets(doc);long comparisons=0;for(int i=0;i<inputs.size();
            i++)comparisons+=scenario(scenarios.get(i),inputs.get(i));return comparisons;
    }
    public static void verifyScope(Path html,EvidenceSelection selection)throws Exception {
        try(var reader=Files.newBufferedReader(html);var parser=new org.jsoup.parser.StreamParser(org.jsoup.parser.Parser.htmlParser()).parse(reader,"")){
            var first=parser.selectNext("#test-view .test-list-item > li.test-item");require(first!=null,"Missing scope owner");
            var body=first.select("pre").stream().filter(e->e.wholeText().contains("\"effectiveScope\"")).toList();require(body.size()==1,"Missing or repeated selection scope");require(JSON.readTree(body.get(0).wholeText()).equals(
                JSON.readTree(JSON.writeValueAsString(selection.scope()))),"Selection policy or omissions inventory changed");
            Path binary=Path.of(atf.reporting.extent.session.ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toURI());require(field(first,"Reporting binary").equals(binary.getFileName()
                +" / SHA-256 "+LearningRunReport.hash(Files.readAllBytes(binary))),"Publication and audit Reporting binaries differ");
            require(first.text().toLowerCase(Locale.ROOT).contains("reader guide")&&first.text().contains("Seats 0..3")&&hasField(first,"Independent expected values"),"Missing reader glossary or oracle explanation");
        }
    }
    private static void assets(Element element){require(element.select("script[src],link[rel=stylesheet][href]").isEmpty(),"External asset dependency");
        require(element.select("a[href^=file:],a[href^=http:],a[href^=https:]").isEmpty(),"External evidence dependency");}
    private static long scenario(Element scenario,LearningRunReport.Input input)throws Exception{
        long assertions=0;
        var r=input.receipt();require(field(scenario,"Invocation ID").equals(r.path("invocationId").asText()),"Invocation owner/order changed");
            require(field(scenario,"Source receipt SHA-256").equals(input.sha256()),"Captured source fingerprint changed");
            require(field(scenario,"Original outcome").equals(r.path("status").asText()),"Captured outcome changed");
            String outcome=r.path("status").asText().equals("ERROR")?"fail":r.path("status").asText().toLowerCase(Locale.ROOT);require(scenario.attr("status").equals(outcome),"Native scenario outcome changed");
            var timing=scenario.select("[data-atf-execution-duration-nanos]");require(timing.size()==1&&timing.get(0).attr("data-atf-execution-duration-nanos").equals(r.path("durationNanos").asText()),"Captured timing changed");
            var observations=scenario.select("div[data-atf-node-role=observation]").stream().filter(e->hasField(e,"Checkpoint owner")).toList();require(observations.size()==r.path("checkpoints").size(),"Checkpoint context count");
            var steps=scenario.select(".node").stream().filter(e->e.text().contains(" — ")&&r.path("checkpoints").findValuesAsText("owner").stream().anyMatch(owner->e.text().startsWith(owner+" — "))).toList();
                require(steps.size()>=r.path("checkpoints").size(),"Assertion step inventory");
            for(int c=0;c<r.path("checkpoints").size();c++){
                var expected=r.path("checkpoints").get(c);Element observed=observations.get(c);String owner=expected.path("owner").asText();require(field(observed,"Checkpoint owner").equals(owner),"Checkpoint owner/order changed");
                var expectedLong=JSON.createArrayNode();
                for(var comparison:expected.path("comparisons"))if(InlineComparisonValues.bounded(comparison))expectedLong.add(comparison);
                require(readBody(observed,"Complete checkpoint input and observed state").equals(expected.path("context")),"Typed context changed under "+owner);
                if(!expectedLong.isEmpty())require(InlineComparisonValues.decode(
                        readBody(observed,"Complete values for bounded assertion cells — local references"),expected.path("context")).equals(expectedLong),
                        "Captured long comparison values changed under "+owner);
                String groupTitle=owner+" — "+expected.path("title").asText();
                var groups=steps.stream().filter(e->e.text().equals(groupTitle)||e.text().startsWith(groupTitle+" · rows ")).toList();
                require(!groups.isEmpty()&&groups.get(0).text().equals(groupTitle),"Assertion owner/order changed");
                var rows=new org.jsoup.select.Elements();
                for(int part=0;part<groups.size();part++){
                    Element card=groups.get(part).closest(".card");require(card!=null,"No native assertion group");
                    var partRows=card.select("table.helm-validation-table > tbody > tr");
                    if(part>0)require(groups.get(part).text().equals(groupTitle+" · rows "+(rows.size()+1)+"–"+(rows.size()+partRows.size())
                            +" of "+expected.path("comparisons").size()),"Assertion segment order/range changed");
                    rows.addAll(partRows);
                }
                require(rows.size()==expected.path("comparisons").size(),"Last row/count changed under "+owner);
                for(int row=0;row<rows.size();row++){var a=expected.path("comparisons").get(row);var cells=rows.get(row).select("> td");require(cells.size()==4,"Standard four-column table");
                    require(cells.get(0).wholeText().equals(a.path("name").asText()),"Comparison name/order changed");cellValue(a.path("expected"),cells.get(1));cellValue(a.path("actual"),cells.get(2));
                    require(cells.get(3).text().contains(a.path("passed").asBoolean()?"✅":"❌"),"Comparison verdict changed");assertions++;}
            }

        return assertions;
    }
    private static JsonNode readBody(Element observation,String label)throws Exception{
        var bodies=observation.select("pre").stream().filter(pre->{
            Element details=pre.closest("details");String title=details==null?"":details.select("> summary").text();
            return title.equals(label)||title.startsWith(label+" · part ");
        }).toList();
        require(!bodies.isEmpty(),"Missing complete body "+label);JsonNode combined=null;
        for(int i=0;i<bodies.size();i++){
            String title=bodies.get(i).closest("details").select("> summary").text();
            require(title.equals(bodies.size()==1?label:label+" · part "+(i+1)+" of "+bodies.size()),"Missing/reordered evidence part "+label);
            JsonNode value=JSON.readTree(bodies.get(i).wholeText());
            combined=combined==null?value:atf.tests.reporting.GameEvidenceBodies.merge(combined,value);
        }
        return combined;
    }
    private static void cellValue(JsonNode value,Element cell)throws Exception{
        String expected=LearningRunReport.text(value),actual=cell.wholeText();
        if(expected.codePointCount(0,expected.length())<=240){if(value.isContainerNode())require(JSON.readTree(actual).equals(value),"Native structured cell changed");else require(expected.equals(actual),"Native assertion scalar changed");}
        else {String visible=actual.endsWith("…")?actual.substring(0,actual.length()-1):actual;String original=value.isContainerNode()?jsonTokens(expected):expected;String prefix=value.isContainerNode()?jsonTokens(visible):visible;
            require(prefix.codePointCount(0,prefix.length())>=Math.min(32,original.codePointCount(0,original.length()))&&original.startsWith(prefix),"Native bounded cell prefix changed");require(actual.codePointCount(0,
            actual.length())<=240,"Native assertion preview exceeded240");}
    }
    // Ignore formatting whitespace only outside JSON strings; preserve digits, signs and escaped/string content.
    private static String jsonTokens(String value){var out=new StringBuilder();boolean quoted=false,escaped=false;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(quoted){out.append(c);if(escaped)escaped=false;else
        if(c=='\\')escaped=true;else if(c=='"')quoted=false;}else if(c=='"'){quoted=true;out.append(c);}else if(!Character.isWhitespace(c))out.append(c);}return out.toString();}
    private static boolean hasField(Element e,String label){return e.select("tr").stream().anyMatch(row->row.childrenSize()==2&&row.child(0).text().equalsIgnoreCase(label));}
    private static String field(Element e,String label){var rows=e.select("tr").stream().filter(row->row.childrenSize()==2&&row.child(0).text().equalsIgnoreCase(label)).toList();require(!rows.isEmpty(),"Missing field "+label);
        String value=rows.get(0).child(1).wholeText();for(var row:rows)require(row.child(1).wholeText().equals(value),"Conflicting field "+label);return value;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
