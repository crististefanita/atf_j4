package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.AdbClient;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import atf.testkit.AtfComponentTest;

@AtfComponentTest
class AndroidHierarchyFreshnessTest {
    @TempDir Path temp;
    @Test void zeroExitNullRootMustNeverReturnThePreviousScreensHierarchy()throws Exception {
        FakeDevice device=new FakeDevice(false);
        AndroidUiDriver driver=new AndroidUiDriver(device,temp);
        assertThrows(IllegalStateException.class,driver::dump);
        assertEquals(0,device.pulls);assertEquals(4,device.removals);
    }
    @Test void retryOnlyPullsAConfirmedFreshHierarchy()throws Exception {
        FakeDevice device=new FakeDevice(true);
        String xml=new AndroidUiDriver(device,temp).dump();
        assertTrue(xml.contains("Fresh bidding"));assertFalse(xml.contains("Old menu"));
        assertEquals(1,device.pulls);assertEquals(2,device.removals);
    }
    private static final class FakeDevice implements AndroidUiDriver.Device {
        String remote="<hierarchy><node text=\"Old menu\"/></hierarchy>";
        int dumps,pulls,removals;final boolean recover;
        FakeDevice(boolean recover){this.recover=recover;}
        public AdbClient.Result run(String... args)throws java.io.IOException {
            List<String> command=List.of(args);String output="";int exit=0;
            if(command.contains("rm")){remote=null;removals++;}
            else if(command.contains("uiautomator")) {
                dumps++;
                if(recover&&dumps==2){remote="<hierarchy><node text=\"Fresh bidding\"/></hierarchy>";output="UI hierchary dumped to: /sdcard/atf-j4-window.xml";}
                else output="ERROR: null root node returned by UiTestAutomationBridge.";
            }else if(command.getFirst().equals("pull")) {
                pulls++;if(remote==null)exit=1;else Files.writeString(Path.of(command.get(2)),remote);
            }else throw new AssertionError("Unexpected command "+command);
            return new AdbClient.Result(exit,output,command);
        }
        public AdbClient.BinaryResult runBinary(String... args){throw new AssertionError("Screenshot not expected");}
    }
}
