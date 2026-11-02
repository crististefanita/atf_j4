package atf.tests;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Matches complete payload values while respecting Reporting's native compact JSON formatting. */
final class ReportTextValueMatcher {
    private static final ObjectMapper JSON=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private ReportTextValueMatcher(){ }
    static boolean containsCompleteValue(String html,String value) {
        if(containsLiteral(html,value))return true;
        String trimmed=value.strip();
        if(!trimmed.startsWith("[")&&!trimmed.startsWith("{"))return false;
        try{return containsLiteral(html,JSON.readTree(trimmed).toString());}
        catch(Exception notJson){return false;}
    }
    private static boolean containsLiteral(String html,String value) {
        String escaped=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
        return html.contains(value)||html.contains(escaped);
    }
}
