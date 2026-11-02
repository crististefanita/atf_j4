package atf.tests.learning;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Lossless interning within one checkpoint, independent of its captured verdict. No cross-owner references. */
public final class InlineComparisonValues {
    private static final ObjectMapper JSON=new ObjectMapper();
    private InlineComparisonValues(){};
    public static boolean bounded(JsonNode a){return length(a.path("expected"))>240||length(a.path("actual"))>240;}
    private static int length(JsonNode value){String text=LearningRunReport.text(value);return text.codePointCount(0,text.length());}
    public static ObjectNode encode(JsonNode checkpoint){
        var result=JSON.createObjectNode();result.put("format","checkpoint-inline-values-v1");var values=result.putObject("values");var rows=result.putArray("rows");
        for(var a:checkpoint.path("comparisons"))if(bounded(a)){var row=rows.addObject();row.put("name",a.path("name").asText());row.set("expected",intern(a.path("expected"),checkpoint.path("context"),values));row.set("actual",intern(a.path("actual"),checkpoint.path("context"),values));row.put("passed",a.path("passed").asBoolean());}return result;
    }
    private static ObjectNode intern(JsonNode value,JsonNode context,ObjectNode values){
        var token=JSON.createObjectNode();for(var fields=context.fields();fields.hasNext();){var field=fields.next();if(field.getValue().equals(value)){token.put("contextPointer","/"+field.getKey().replace("~","~0").replace("/","~1"));return token;}}
        for(var fields=values.fields();fields.hasNext();){var field=fields.next();if(field.getValue().equals(value)){token.put("valueRef",field.getKey());return token;}}
        String id="v"+values.size();values.set(id,value.deepCopy());token.put("valueRef",id);return token;
    }
    public static JsonNode decode(JsonNode encoded,JsonNode context){
        if(!encoded.path("format").asText().equals("checkpoint-inline-values-v1"))throw new IllegalArgumentException("Unknown inline value format");var rows=JSON.createArrayNode();
        for(var source:encoded.path("rows")){var row=rows.addObject();row.put("name",source.path("name").asText());row.set("expected",resolve(source.path("expected"),encoded.path("values"),context));row.set("actual",resolve(source.path("actual"),encoded.path("values"),context));row.put("passed",source.path("passed").asBoolean());}return rows;
    }
    private static JsonNode resolve(JsonNode token,JsonNode values,JsonNode context){
        JsonNode result;if(token.has("contextPointer")&&token.size()==1)result=context.at(token.path("contextPointer").asText());else if(token.has("valueRef")&&token.size()==1)result=values.path(token.path("valueRef").asText());else throw new IllegalArgumentException("Invalid local inline reference");if(result.isMissingNode())throw new IllegalArgumentException("Missing local inline value");return result.deepCopy();
    }
}
