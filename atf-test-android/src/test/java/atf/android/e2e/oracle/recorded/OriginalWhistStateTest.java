package atf.android.e2e.oracle.recorded;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import atf.testkit.AtfComponentTest;

@AtfComponentTest
class OriginalWhistStateTest {
    @TempDir Path temp;
    @Test void decodesARealVersion145BiddingCheckpoint()throws Exception {
        var state=OriginalWhistState.read(fixture());
        assertEquals(4,state.players);assertEquals(6,state.mode);assertEquals(1,state.deck);
        assertEquals(10,state.handSize);assertEquals(0,state.handIndex);assertEquals(2,state.dealer);
        assertEquals(0,state.trump);assertEquals(0,state.trumpCard);
        assertArrayEquals(new int[]{-1,-1,-1,2},state.bids);
        assertTrue(state.bidding);assertFalse(state.handOver);assertEquals(List.of(),state.plays());
        assertEquals(40,java.util.stream.IntStream.range(0,4).boxed().flatMap(p->state.hand(p).stream()).distinct().count());
        assertArrayEquals(new int[]{0,0,0,0},state.scores[0]);
    }
    @Test void extractsOnlyTheTargetSaveFromCompressedPackageBackup()throws Exception {
        byte[] save=fixture();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        bytes.write("ANDROID BACKUP\n5\n1\nnone\n".getBytes(StandardCharsets.US_ASCII));
        try(var compressed=new DeflaterOutputStream(bytes)) {
            byte[] header=new byte[512];
            byte[] name="apps/com.qsoft.android.whist/f/save2.dat".getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(name,0,header,0,name.length);
            byte[] size=String.format("%011o",save.length).getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(size,0,header,124,size.length);
            compressed.write(header);compressed.write(save);compressed.write(new byte[(512-save.length%512)%512]);
            compressed.write(new byte[1024]);
        }
        Path file=temp.resolve("package.ab");Files.write(file,bytes.toByteArray());
        assertArrayEquals(save,OriginalWhistState.saveFileFromBackup(file));
    }
    @Test void recognizesCompletedScoreTableEvenWhenTheAnimationFlagWasCleared()throws Exception {
        try(var in=getClass().getResourceAsStream("/original-whist/v4.5-complete-hand.save2")) {
            var state=OriginalWhistState.read(Objects.requireNonNull(in).readAllBytes());
            assertFalse(state.savedHandOverFlag);assertTrue(state.handOver);
            assertEquals(40,state.plays().size());assertArrayEquals(new int[]{0,0,0,0},state.remaining);
            assertArrayEquals(new int[]{10,15,2,3},state.scores[state.handIndex]);
        }
    }
    @Test void resumedScoreMaySerializeNextHandSizeWithoutChangingTheCompletedDeal()throws Exception {
        try(var in=getClass().getResourceAsStream("/original-whist/v4.5-resumed-score-next-size.save2")) {
            var state=OriginalWhistState.read(Objects.requireNonNull(in).readAllBytes());
            assertEquals(9,state.serializedHandSize);assertEquals(8,state.handSize);
            assertEquals(17,state.handIndex);assertTrue(state.handOver);assertFalse(state.bidding);
            assertEquals(32,state.plays().size());assertArrayEquals(new int[]{2,2,1,3},state.won);
            assertArrayEquals(new int[]{77,122,138,103},state.scores[state.handIndex]);
        }
    }
    @Test void rejectsTruncationAndEncryptedBackup()throws Exception {
        assertThrows(IOException.class,()->OriginalWhistState.read(Arrays.copyOf(fixture(),180)));
        Path file=temp.resolve("encrypted.ab");Files.writeString(file,"ANDROID BACKUP\n5\n1\nAES-256\n");
        assertThrows(IOException.class,()->OriginalWhistState.saveFileFromBackup(file));
    }
    @Test void readsTheOriginalCompletionFlagWithoutExportingOtherPreferences()throws Exception {
        for(boolean value:List.of(false,true)) {
            byte[] map=("<map><boolean name=\"gameover\" value=\""+value+"\"/></map>").getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            bytes.write("ANDROID BACKUP\n5\n1\nnone\n".getBytes(StandardCharsets.US_ASCII));
            try(var zipped=new DeflaterOutputStream(bytes)) {
                byte[] header=new byte[512];
                byte[] name="apps/com.qsoft.android.whist/sp/Whist.xml".getBytes(StandardCharsets.US_ASCII);
                System.arraycopy(name,0,header,0,name.length);
                byte[] size=String.format("%011o",map.length).getBytes(StandardCharsets.US_ASCII);
                System.arraycopy(size,0,header,124,size.length);zipped.write(header);zipped.write(map);
                zipped.write(new byte[(512-map.length%512)%512]);zipped.write(new byte[1024]);
            }
            Path backup=temp.resolve("completion-"+value+".ab");Files.write(backup,bytes.toByteArray());
            assertEquals(value,OriginalWhistState.gameOverFromBackup(backup));
        }
    }
    private byte[] fixture()throws IOException {
        try(var in=getClass().getResourceAsStream("/original-whist/v4.5-bidding.save2")){return Objects.requireNonNull(in).readAllBytes();}
    }
}
