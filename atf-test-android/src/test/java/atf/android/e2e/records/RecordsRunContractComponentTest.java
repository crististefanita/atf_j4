package atf.android.e2e.records;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** No emulator: proves that ownership loss cannot forward a command to the transport. */
@AtfComponentTest
class RecordsRunContractComponentTest {
    @TempDir Path temp;

    @Test void historyLabelsAreCardCountsRatherThanHandOrdinalsOrCombinedLabels() throws Exception {
        String prefix="<hierarchy><node class=\"android.widget.TextView\" text=\"Hand\" bounds=\"[10,10][76,40]\"/>"
                +"<node class=\"android.widget.ScrollView\" bounds=\"[10,70][500,300]\">";
        String suffix="</node></hierarchy>";
        String row="<node class=\"android.widget.TextView\" text=\"1\" bounds=\"[10,70][76,110]\"/>";
        assertTrue(RecordsRunContract.historyCardCounts(prefix+row+suffix,"Hand",8));
        for(String label:List.of("1 · 1","1.1","24","0","—")) {
            assertFalse(RecordsRunContract.historyCardCounts(prefix+row.replace("text=\"1\"","text=\""+label+"\"")+suffix,"Hand",8));
        }
        assertFalse(RecordsRunContract.historyCardCounts(prefix+suffix,"Hand",8));
        assertFalse(RecordsRunContract.historyCardCounts(prefix+row+suffix,"Mână",8));
        assertTrue(RecordsRunContract.historyCardCounts((prefix+row+suffix).replace("Hand","Mână"),"Mână",8));
    }

    @Test void changingOwnerAfterOneCommandStopsTextAndBinaryDispatch() throws Exception {
        Path path=reservation(true,"ours","device-a");var fake=new FakeDevice();
        var guarded=RecordsRunContract.guarded(fake,path,"ours","device-a");
        guarded.run("shell","get-state");
        reservation(true,"another-owner","device-a");
        assertThrows(IllegalStateException.class,()->guarded.run("shell","input","tap","1","1"));
        assertThrows(IllegalStateException.class,()->guarded.runBinary("exec-out","screencap","-p"));
        assertEquals(1,fake.commands);
    }

    @Test void inactiveOrDifferentDeviceReservationsNeverReachTheTransport() throws Exception {
        for(var value:List.of(List.of(false,"ours","device-a"),List.of(true,"ours","device-b"))) {
            Path path=reservation((Boolean)value.get(0),(String)value.get(1),(String)value.get(2));
            var fake=new FakeDevice();var guarded=RecordsRunContract.guarded(fake,path,"ours","device-a");
            assertThrows(IllegalStateException.class,()->guarded.run("shell","am","force-stop",RecordsContext.PACKAGE));
            assertEquals(0,fake.commands);
        }
    }

    @Test void corruptReservationFailsBeforeTransferAndBlankIdentityIsRejected() throws Exception {
        Path path=temp.resolve("lease.json");Files.writeString(path,"not JSON");var fake=new FakeDevice();
        var guarded=RecordsRunContract.guarded(fake,path,"ours","device-a");
        assertThrows(java.io.IOException.class,()->guarded.run("push","fixture","remote"));
        assertThrows(IllegalArgumentException.class,()->RecordsRunContract.lease(path,"","device-a"));
        assertEquals(0,fake.commands);
    }

    @Test void manifestRejectsTraversalDuplicatesAndMissingDigestsBeforeRestoration() throws Exception {
        String hash="a".repeat(64);
        var good=RecordsFixtures.JSON.createArrayNode();good.addObject().put("path","files/highestScores.json").put("sha256",hash);
        RecordsRunContract.manifest(good);
        for(String path:List.of("/data/data/another/files/x","files/../shared_prefs/x","files/x;rm","shared_prefs/")) {
            var bad=RecordsFixtures.JSON.createArrayNode();bad.addObject().put("path",path).put("sha256",hash);
            assertThrows(IllegalArgumentException.class,()->RecordsRunContract.manifest(bad));
        }
        var duplicate=good.deepCopy();duplicate.add(good.get(0));
        assertThrows(IllegalArgumentException.class,()->RecordsRunContract.manifest(duplicate));
        var missing=good.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)missing.get(0)).remove("sha256");
        assertThrows(IllegalArgumentException.class,()->RecordsRunContract.manifest(missing));
    }

    @Test void privateDirectoryMustBelongToTheSelectedPackage() {
        RecordsRunContract.privateRoot("/data/data/ro.whistlab.app");
        RecordsRunContract.privateRoot("/data/user/0/ro.whistlab.app");
        for(String path:List.of("/data/data","/data/data/another.app","/data/data/ro.whistlab.app/.."))
            assertThrows(IllegalStateException.class,()->RecordsRunContract.privateRoot(path));
    }

    @Test void largeJsonComparisonsStayCompactAndCanonicalWithoutLosingChanges() throws Exception {
        var first=RecordsFixtures.JSON.readTree("{\"rows\":[1,2],\"profile\":{\"a\":1,\"b\":2}}");
        var reordered=RecordsFixtures.JSON.readTree("{\"profile\":{\"b\":2,\"a\":1},\"rows\":[1,2]}");
        assertEquals(RecordsRunContract.describe(first),RecordsRunContract.describe(reordered));
        var changed=reordered.deepCopy();((com.fasterxml.jackson.databind.node.ArrayNode)changed.path("rows")).add(3);
        assertNotEquals(RecordsRunContract.describe(first),RecordsRunContract.describe(changed));
        var large=RecordsFixtures.JSON.createObjectNode().put("payload","x".repeat(100000));
        assertEquals(true,RecordsRunContract.describe(large).length()<150);
        assertEquals("[5, -5]",RecordsRunContract.describe(List.of(5,-5)));
        assertEquals(true,RecordsRunContract.describe(List.of("x".repeat(1000),"row2")).length()<150);
        assertEquals(true,RecordsRunContract.describe("x".repeat(1000)).length()<150);
    }

    @Test void scoreTableScrollWinsOverTheLaterDialogButtonContainer() throws Exception {
        String table="<node class=\"android.widget.ScrollView\" bounds=\"[54,375][684,962]\"/>";
        String buttons="<node class=\"android.widget.ScrollView\" bounds=\"[36,971][684,1097]\"/>";
        for(String body:List.of(table+buttons,buttons+table))assertArrayEquals(new int[]{54,375,684,962},
                RecordsRunContract.scrollBounds("<hierarchy>"+body+"</hierarchy>","android.widget.ScrollView"));
        assertThrows(IllegalStateException.class,()->RecordsRunContract.scrollBounds("<hierarchy>"+table+"</hierarchy>","android.widget.HorizontalScrollView"));
    }

    @Test void aClippedAccessibleCellDoesNotSatisfyCompleteRowOrColumnVisibility() throws Exception {
        String first="<hierarchy><node content-desc=\"cell\" bounds=\"[203,375][527,440]\"/></hierarchy>";
        String clippedRow="<hierarchy><node content-desc=\"cell\" bounds=\"[203,960][527,962]\"/></hierarchy>";
        String clippedColumn="<hierarchy><node content-desc=\"cell\" bounds=\"[527,375][684,440]\"/></hierarchy>";
        assertTrue(RecordsRunContract.visibleDescription(first,"cell",false,65));
        assertTrue(RecordsRunContract.visibleDescription(first,"cell",true,324));
        assertFalse(RecordsRunContract.visibleDescription(clippedRow,"cell",false,65));
        assertFalse(RecordsRunContract.visibleDescription(clippedColumn,"cell",true,324));
        assertFalse(RecordsRunContract.visibleDescription(first,"absent",true,0));
    }

    private Path reservation(boolean active,String id,String serial) throws Exception {
        Path path=temp.resolve("lease.json");Files.writeString(path,RecordsFixtures.JSON.createObjectNode()
                .put("active",active).put("leaseId",id).put("serial",serial).toString());return path;
    }
    private static final class FakeDevice implements AndroidUiDriver.Device {
        int commands;
        public AdbClient.Result run(String... args) {commands++;return new AdbClient.Result(0,"ok",List.of(args));}
        public AdbClient.BinaryResult runBinary(String... args) {commands++;return new AdbClient.BinaryResult(0,new byte[]{1},"",List.of(args));}
    }
}
