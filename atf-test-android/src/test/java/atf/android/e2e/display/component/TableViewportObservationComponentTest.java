package atf.android.e2e.display.component;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.display.support.TableViewportObservation;
import atf.testkit.AtfComponentTest;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static atf.android.e2e.display.component.SyntheticTableHierarchy.Fault;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class TableViewportObservationComponentTest {
    @TempDir static Path output;

    static Stream<Arguments> validWindows() {
        return profiles().stream().flatMap(profile->Stream.of(1f,1.3f,1.5f).flatMap(scale->Stream.of(false,true)
                .map(trick->Arguments.of(profile,scale,trick))));
    }
    static Stream<Arguments> faults() {
        return Stream.of(Fault.MISSING_HUMAN,Fault.SHORT_SEAT,Fault.SHORT_FACE,Fault.SCROLL,
                Fault.ZERO_TITLE,Fault.ZERO_TRUMP,Fault.OVERLAP,Fault.OBSTRUCTED_WINDOW,
                Fault.MISSING_ACTION,Fault.ACTION_BELOW_NAME,Fault.CUT_ACTION,Fault.TALL_PLAYED_FACE)
                .map(fault->Arguments.of(fault, switch(fault) {
                    case MISSING_HUMAN,SHORT_SEAT->"Complete seat frames in the same viewport";
                    case SHORT_FACE->"Whole hand retains complete faces on one row";
                    case SCROLL->"Game table has no vertical scroll container";
                    case ZERO_TITLE,ZERO_TRUMP->"Game title and trump remain visible with the hand";
                    case OVERLAP->"Seats and all trick cards stay distinct";
                    case OBSTRUCTED_WINDOW->"Board and hand remain separate inside the display";
                    case MISSING_ACTION,ACTION_BELOW_NAME,CUT_ACTION->"Complete turn action stays above the player name";
                    case TALL_PLAYED_FACE->"Requested played-card height: King of hearts";
                    default->throw new IllegalArgumentException();
                }));
    }

    @ParameterizedTest(name="{0}, measured font scale {1}, completed trick {2}")
    @MethodSource("validWindows")
    void theSameDecoderAuditsEveryDeclaredDisplayWithoutATransport(ResponsiveDisplayProfile profile,float fontScale,boolean trick) throws Exception {
        int cards=trick?12:13;
        String xml=new SyntheticTableHierarchy(profile).xml(cards,trick,fontScale,Fault.NONE);
        var checks=TableViewportObservation.inspect(driver(),xml,profile,cards,trick);
        assertTrue(checks.stream().allMatch(check->check.passed()),checks.toString());
    }

    @ParameterizedTest(name="reject {0}") @MethodSource("faults")
    void controlledDefectsCannotBeHiddenByAValidProfile(Fault fault,String comparison) throws Exception {
        var profile=profiles().getFirst();var fixture=new SyntheticTableHierarchy(profile);
        boolean trick = fault != Fault.MISSING_ACTION && fault != Fault.ACTION_BELOW_NAME && fault != Fault.CUT_ACTION;
        int cards = trick ? 12 : 13;
        var baseline=TableViewportObservation.inspect(driver(),fixture.xml(cards,trick,1f,Fault.NONE),profile,cards,trick);
        assertTrue(baseline.stream().allMatch(check->check.passed()));
        var checks=TableViewportObservation.inspect(driver(),fixture.xml(cards,trick,1f,fault),profile,cards,trick);
        assertFalse(checks.stream().filter(check->check.name().equals(comparison)).findFirst().orElseThrow().passed());
    }

    @Test void aPresentButZeroHeightHeaderIsNotAVisibleTitle() throws Exception {
        var profile=profiles().getFirst();String xml=new SyntheticTableHierarchy(profile).xml(13,false,1f,Fault.ZERO_TITLE);
        assertTrue(driver().hasText(xml,"Whist · Descending"));
        assertTrue(driver().hasDescriptionContaining(xml,"Trump"));
        assertFalse(TableViewportObservation.decode(driver(),xml,profile).headerVisible());
    }

    @Test void windowBoundsAreObservedAndLegacyCapturesHaveAnExplicitDisplayFallback() throws Exception {
        var profile=profiles().getFirst();String xml=new SyntheticTableHierarchy(profile).xml(13,false,1f,Fault.OBSTRUCTED_WINDOW);
        assertTrue(TableViewportObservation.decode(driver(),xml,profile).viewport().bottom()<profile.height());
        var legacy=TableViewportObservation.decode(driver(),xml.replace("resource-id=\"android:id/content\"",""),profile);
        assertEquals(profile.height(),legacy.viewport().bottom());
    }

    @Test void malformedOrAmbiguousObservationsFailBeforeAnyLiveAction() throws Exception {
        var profile=profiles().getFirst();String xml=new SyntheticTableHierarchy(profile).xml(12,true,1f,Fault.NONE);
        assertThrows(IllegalArgumentException.class,()->TableViewportObservation.decode(driver(),xml.replace("King of hearts","King of stars"),profile));
        String duplicate=xml.replace("</hierarchy>","<node resource-id=\"android:id/content\" bounds=\"[0,0][720,1280]\"/></hierarchy>");
        assertThrows(IllegalArgumentException.class,()->TableViewportObservation.decode(driver(),duplicate,profile));
        assertThrows(Exception.class,()->TableViewportObservation.decode(driver(),"<!DOCTYPE hierarchy><hierarchy/>",profile));
    }

    @Test void biddingAndPausedStatesKeepAnExplicitActionContract() throws Exception {
        var profile=profiles().getFirst();
        String play=new SyntheticTableHierarchy(profile).xml(13,false,1f,Fault.NONE);
        String bid=play.replace("PLAY NOW","BID NOW");
        assertTrue(TableViewportObservation.inspectActions(driver(),bid,profile).getFirst().passed());
        String paused=new SyntheticTableHierarchy(profile).xml(13,false,1f,Fault.MISSING_ACTION)
                .replace(" Current turn. PLAY NOW","");
        assertTrue(TableViewportObservation.inspectActions(driver(),paused,profile,0).getFirst().passed());
        assertFalse(TableViewportObservation.inspectActions(driver(),paused,profile,1).getFirst().passed(),
                "A playing table cannot silently lose both the active flag and action label");
        assertFalse(TableViewportObservation.inspectActions(driver(),play.replace("PLAY NOW\" content-desc", "BID NOW\" content-desc"),
                profile).getFirst().passed());
    }

    @Test void occupiedSeatRegionIncludesTheActionWhileTheFrameKeepsItsOwnBounds() throws Exception {
        var profile=profiles().getFirst();
        String xml=new SyntheticTableHierarchy(profile).xml(13,false,1f,Fault.NONE);
        var ui=driver();
        String human=ui.descriptions(xml).stream().filter(text->text.startsWith("You. Bid ")).findFirst().orElseThrow();
        var frame=ui.boundsForDescription(xml,human);
        var action=ui.boundsForText(xml,"PLAY NOW");
        var occupied=TableViewportObservation.occupiedSeatBounds(ui,xml,human);
        assertEquals(action.top(),occupied.top());
        assertEquals(frame.bottom(),occupied.bottom());
        assertEquals(frame.left(),occupied.left());
        int controlsBottom=action.top()-6,opponentGap=14;
        assertTrue(occupied.top()-controlsBottom<opponentGap);
        assertTrue(frame.top()-controlsBottom>opponentGap,"Ignoring the action reverses the same distance comparison");
        String paused=xml.replace(" Current turn. PLAY NOW","");
        assertEquals(frame,TableViewportObservation.occupiedSeatBounds(ui,paused,human.replace(" Current turn. PLAY NOW","")));
    }

    @Test void romanianActionsUseTheirOwnCompleteVisibleAndAccessibleLabels() throws Exception {
        var profile=profiles().getFirst();
        String xml=new SyntheticTableHierarchy(profile).xml(13,false,1f,Fault.NONE)
                .replace(". Bid ",". Licitat ").replace(". Score ",". Scor ")
                .replace(". Current turn.",". La rând.").replace("PLAY NOW","JOACĂ ACUM");
        assertTrue(TableViewportObservation.inspectActions(driver(),xml,profile,1,true).getFirst().passed());
        assertFalse(TableViewportObservation.inspectActions(driver(),xml.replace("JOACĂ ACUM","JOACĂ"),profile,1,true).getFirst().passed());
        String bidding=xml.replace("JOACĂ ACUM\" content-desc","LICITEAZĂ\" content-desc")
                .replace("La rând. JOACĂ ACUM","La rând. LICITEAZĂ ACUM");
        assertTrue(TableViewportObservation.inspectActions(driver(),bidding,profile,1,true).getFirst().passed());
    }

    private static List<ResponsiveDisplayProfile> profiles() {
        return List.of(new ResponsiveDisplayProfile(720,1280,360),new ResponsiveDisplayProfile(972,1728,440),
                new ResponsiveDisplayProfile(1080,1920,440),new ResponsiveDisplayProfile(1080,2448,440),
                new ResponsiveDisplayProfile(1200,1920,320),new ResponsiveDisplayProfile(1440,3200,640));
    }
    private static AndroidUiDriver driver() throws Exception {
        return new AndroidUiDriver(new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args){throw new AssertionError("Display component attempted a device command");}
            public AdbClient.BinaryResult runBinary(String... args){throw new AssertionError("Display component attempted a capture");}
        },output);
    }
}
