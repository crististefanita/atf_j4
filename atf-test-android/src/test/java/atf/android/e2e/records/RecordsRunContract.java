package atf.android.e2e.records;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

/** Preconditions for reversible device work; the guard is evaluated before every transport dispatch. */
final class RecordsRunContract {
    private RecordsRunContract() { }

    static String installedApk(JsonNode setup) {
        String path = setup.path("installedPath").asText(), observed = setup.path("installedApkSha256").asText();
        String requested = setup.path("requestedApkSha256").asText();
        if (!path.startsWith("/") || !path.endsWith(".apk") || path.contains("\n") || path.contains("\r")
                || !observed.matches("(?i)[0-9a-f]{64}") || !requested.matches("(?i)[0-9a-f]{64}")
                || !observed.equalsIgnoreCase(requested))
            throw new IllegalArgumentException("Missing or inconsistent installed APK identity");
        return path + "; SHA-256=" + observed.toLowerCase(java.util.Locale.ROOT);
    }

    @FunctionalInterface interface TextObservation { java.util.List<String> read() throws Exception; }
    @FunctionalInterface interface Reveal { void show(String label) throws Exception; }

    static String visibleLabel(TextObservation observation, Reveal reveal, String requested) throws Exception {
        String actual = matchingLabel(observation.read(), requested);
        if (actual == null) {
            reveal.show(requested);
            actual = matchingLabel(observation.read(), requested);
        }
        if (actual == null) throw new IllegalStateException("No visible control: " + requested);
        return actual;
    }

    private static String matchingLabel(java.util.List<String> texts, String requested) {
        return texts.stream().filter(text -> text.equalsIgnoreCase(requested)).findFirst().orElse(null);
    }

    static void lease(Path path, String id, String serial) throws IOException {
        if(id==null||id.isBlank()||serial==null||serial.isBlank())
            throw new IllegalArgumentException("Expected device reservation identity is missing");
        JsonNode current=RecordsFixtures.JSON.readTree(path.toFile());
        if(!current.path("active").asBoolean()||!id.equals(current.path("leaseId").asText())
                ||!serial.equals(current.path("serial").asText()))
            throw new IllegalStateException("Exclusive Android device lease was lost or targets another device");
    }

    static AndroidUiDriver.Device guarded(AndroidUiDriver.Device delegate,Path path,String id,String serial) {
        return new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... arguments) throws IOException,InterruptedException {
                lease(path,id,serial);return delegate.run(arguments);
            }
            public AdbClient.BinaryResult runBinary(String... arguments) throws IOException,InterruptedException {
                lease(path,id,serial);return delegate.runBinary(arguments);
            }
        };
    }

    static void manifest(JsonNode files) {
        if(!files.isArray())throw new IllegalArgumentException("Backup manifest must be an array");
        var seen=new HashSet<String>();
        for(var file:files) {
            String path=file.path("path").asText();
            if(!path.matches("(?:files|shared_prefs)/[A-Za-z0-9_./-]+")||path.contains("..")||!seen.add(path)
                    ||!file.path("sha256").asText().matches("[a-fA-F0-9]{64}"))
                throw new IllegalArgumentException("Invalid or duplicate backup manifest entry: "+path);
        }
    }

    static void privateRoot(String root) {
        if(!root.equals("/data/data/"+RecordsContext.PACKAGE)&&!root.equals("/data/user/0/"+RecordsContext.PACKAGE))
            throw new IllegalStateException("Unexpected app-private root: "+root);
    }

    static String describe(Object value) {
        if(!(value instanceof JsonNode)&&String.valueOf(value).length()<=200)return String.valueOf(value);
        JsonNode json=value instanceof JsonNode node?node:RecordsFixtures.JSON.valueToTree(value);
        try {
            byte[] canonical=RecordsFixtures.JSON.writer()
                    .with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsBytes(RecordsFixtures.JSON.convertValue(json,Object.class));
            String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonical));
            return "Complete JSON "+json.getNodeType()+" ("+json.size()+" entries), SHA-256="+digest;
        } catch(java.io.IOException|java.security.NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot fingerprint assertion input",failure);
        }
    }

    static int[] scrollBounds(String hierarchy,String className) throws Exception {
        var nodes=RecordsContext.xml(hierarchy).getElementsByTagName("node");
        int[] selected=null;long largest=0;
        var pattern=java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");
        for(int index=0;index<nodes.getLength();index++) {
            var node=(org.w3c.dom.Element)nodes.item(index);
            if(!node.getAttribute("class").equals(className))continue;
            var match=pattern.matcher(node.getAttribute("bounds"));if(!match.matches())continue;
            int[] bounds={Integer.parseInt(match.group(1)),Integer.parseInt(match.group(2)),Integer.parseInt(match.group(3)),Integer.parseInt(match.group(4))};
            long area=(long)(bounds[2]-bounds[0])*(bounds[3]-bounds[1]);
            if(bounds[2]>bounds[0]&&bounds[3]>bounds[1]&&area>largest){selected=bounds;largest=area;}
        }
        if(selected==null)throw new IllegalStateException("No nonempty visible scroll region: "+className);
        return selected;
    }

    static boolean visibleDescription(String hierarchy,String description,boolean horizontal,int minimumExtent) throws Exception {
        var nodes=RecordsContext.xml(hierarchy).getElementsByTagName("node");
        var pattern=java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");
        for(int index=0;index<nodes.getLength();index++) {
            var node=(org.w3c.dom.Element)nodes.item(index);
            if(!description.equals(node.getAttribute("content-desc")))continue;
            var match=pattern.matcher(node.getAttribute("bounds"));if(!match.matches())return false;
            int width=Integer.parseInt(match.group(3))-Integer.parseInt(match.group(1));
            int height=Integer.parseInt(match.group(4))-Integer.parseInt(match.group(2));
            return width>0&&height>0&&(horizontal?width:height)>=minimumExtent;
        }
        return false;
    }

    static boolean historyCardCounts(String hierarchy,String heading,int maximumCards) throws Exception {
        var nodes=RecordsContext.xml(hierarchy).getElementsByTagName("node");
        var bounds=java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");
        int left=-1,right=-1;
        for(int index=0;index<nodes.getLength();index++) {
            var node=(org.w3c.dom.Element)nodes.item(index);
            if(!heading.equals(node.getAttribute("text")))continue;
            var match=bounds.matcher(node.getAttribute("bounds"));
            if(match.matches()){left=Integer.parseInt(match.group(1));right=Integer.parseInt(match.group(3));break;}
        }
        if(left<0||right<=left)return false;
        int[] body=scrollBounds(hierarchy,"android.widget.ScrollView");int found=0;
        for(int index=0;index<nodes.getLength();index++) {
            var node=(org.w3c.dom.Element)nodes.item(index);
            if(!node.getAttribute("class").equals("android.widget.TextView"))continue;
            var match=bounds.matcher(node.getAttribute("bounds"));if(!match.matches())continue;
            int x=Integer.parseInt(match.group(1)),y=Integer.parseInt(match.group(2));
            int end=Integer.parseInt(match.group(3)),bottom=Integer.parseInt(match.group(4));
            if(x!=left||end!=right||y<body[1]||bottom>body[3]||bottom<=y)continue;
            String text=node.getAttribute("text");
            if(!text.matches("[1-9][0-9]*")||Integer.parseInt(text)>maximumCards)return false;
            found++;
        }
        return found>0;
    }
}
