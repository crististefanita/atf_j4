package atf.android.e2e;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AndroidUiDriverComponentTest {
    @Test
    void enabledNumericBidsAreResolvedAsButtonsDespiteRepeatedSeatLabels() throws Exception {
        String hierarchy = """
                <hierarchy>
                  <node text="0" class="android.widget.TextView" enabled="true" />
                  <node text="0" class="android.widget.Button" enabled="true" />
                  <node text="1" class="android.widget.Button" enabled="false" />
                  <node text="2" class="android.widget.Button" enabled="true" />
                </hierarchy>
                """;

        assertEquals(List.of(0, 2), AndroidUiDriver.enabledNumericButtonValues(hierarchy));
    }
}