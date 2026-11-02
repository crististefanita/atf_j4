package atf.android.e2e;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.InflaterInputStream;

/** Read-only observation of version 4.5/145 save2.dat, obtained through Android's package backup. */
final class OriginalWhistState {
    int players, mode, deck, dealer, handIndex, handSize, serializedHandSize, trump, trumpCard, turn, firstTurn;
    boolean handOver, savedHandOverFlag, bidding, trickOver;
    int[] bids, won, remaining;
    int[][] cards, scores;
    float[][] x, y, width, height;
    List<Integer> history;
    String[] names;

    static byte[] saveFileFromBackup(Path backup) throws IOException {
        return backupEntry(backup,"apps/com.qsoft.android.whist/f/save2.dat");
    }

    static boolean gameOverFromBackup(Path backup) throws Exception {
        byte[] xml=backupEntry(backup,"apps/com.qsoft.android.whist/sp/Whist.xml");
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var document=factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        var values=document.getElementsByTagName("boolean");
        for(int i=0;i<values.getLength();i++) {
            var value=(org.w3c.dom.Element)values.item(i);
            if(value.getAttribute("name").equals("gameover"))return Boolean.parseBoolean(value.getAttribute("value"));
        }
        throw new IOException("Original backup has no gameover preference");
    }

    private static byte[] backupEntry(Path backup,String expectedName) throws IOException {
        try (InputStream raw = Files.newInputStream(backup)) {
            if (!line(raw).equals("ANDROID BACKUP") || !line(raw).equals("5"))
                throw new IOException("Expected Android backup format 5");
            String compressed = line(raw), encryption = line(raw);
            if (!encryption.equals("none")) throw new IOException("Encrypted backup is unsupported");
            InputStream tar = compressed.equals("1") ? new InflaterInputStream(raw) : raw;
            byte[] header;
            while ((header = tar.readNBytes(512)).length == 512 && header[0] != 0) {
                String name = new String(header, 0, 100, StandardCharsets.US_ASCII).split("\u0000", 2)[0];
                String sizeText = new String(header, 124, 12, StandardCharsets.US_ASCII).replace("\u0000", "").trim();
                int size = Math.toIntExact(Long.parseLong(sizeText, 8));
                byte[] bytes = tar.readNBytes(size);
                if (bytes.length != size) throw new EOFException("Truncated tar entry");
                if (name.equals(expectedName)) return bytes;
                tar.skipNBytes((512 - size % 512) % 512);
            }
        }
        throw new IOException("Package backup did not contain "+expectedName);
    }

    static OriginalWhistState read(byte[] bytes) throws IOException {
        OriginalWhistState s = new OriginalWhistState();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            in.readInt();
            int[] key = new int[30];
            for (int i=0;i<30;i++) key[i]=in.readInt();
            if (Arrays.stream(key).distinct().count()!=30 || Arrays.stream(key).anyMatch(k->k<0 || k>=30))
                throw new IOException("Invalid score bit permutation");
            in.readInt();
            s.players=in.readInt(); s.mode=in.readInt(); s.deck=in.readInt(); s.dealer=in.readInt();
            s.handIndex=in.readInt(); s.handSize=in.readInt();
            s.serializedHandSize=s.handSize;
            int trumpWord=in.readInt(); s.trump=trumpWord & 65535; s.trumpCard=trumpWord>>>16;
            s.savedHandOverFlag=in.readInt()==1; s.turn=in.readInt(); in.readInt();
            int trickCount=in.readInt(); in.readInt(); s.firstTurn=in.readInt();
            in.readInt(); s.bidding=in.readInt()==1; in.readInt(); in.readInt(); s.trickOver=in.readInt()==1;
            for(int i=0;i<5;i++)in.readInt();
            for(int i=0;i<4;i++)in.readFloat();
            in.readInt(); in.readInt();
            if(s.players<3 || s.players>6 || s.handSize<1 || s.handSize>17 || s.handIndex<0 || s.handIndex>51)
                throw new IOException("Invalid save header");
            s.names=new String[s.players];s.names[0]="Human (seat 0)";
            for(int i=1;i<s.players;i++)s.names[i]=in.readUTF();
            s.remaining=new int[s.players]; s.bids=new int[s.players]; s.won=new int[s.players];
            s.cards=new int[s.players][s.handSize];
            s.x=new float[s.players][s.handSize]; s.y=new float[s.players][s.handSize];
            s.width=new float[s.players][s.handSize]; s.height=new float[s.players][s.handSize];
            for(int p=0;p<s.players;p++) {
                s.remaining[p]=in.readInt(); s.bids[p]=signedLow(in.readInt()); s.won[p]=signedLow(in.readInt());
            }
            for(int i=0;i<s.handSize;i++)for(int p=0;p<s.players;p++) {
                s.cards[p][i]=in.readInt(); s.x[p][i]=in.readFloat(); s.y[p][i]=in.readFloat();
                for(int f=0;f<4;f++)in.readFloat();
                s.width[p][i]=in.readFloat(); s.height[p][i]=in.readFloat();
            }
            for(int i=0;i<trickCount;i++) {in.readInt(); for(int f=0;f<9;f++)in.readFloat();}
            s.scores=new int[s.handIndex+1][s.players];
            for(int h=0;h<=s.handIndex;h++)for(int p=0;p<s.players;p++) {
                int score=decode(in.readInt(),key), check=decode(in.readInt(),key);
                if(score>2000){score-=5000;check-=5000;}
                if(check!=score+99)throw new IOException("Score integrity pair mismatch");
                s.scores[h][p]=score;
            }
            for(int p=0;p<s.players;p++){in.readInt();for(int i=0;i<s.players;i++)in.readInt();}
            in.readInt();in.readInt();
            int count=in.readInt(); if(count<0 || count>52)throw new IOException("Invalid card history count");
            List<Integer> history=new ArrayList<>();for(int i=0;i<count;i++)history.add(in.readInt());
            s.history=List.copyOf(history);
            // The persisted hand_over animation flag is cleared when the score table opens.
            // Completion is evidenced by all hands empty and the full standard-card play history.
            int played=s.plays().size();
            s.handOver=!s.bidding&&Arrays.stream(s.remaining).allMatch(n->n==0)&&played>0
                    &&played%s.players==0&&Arrays.stream(s.won).sum()==played/s.players;
            // On process restart at SCORE, the original can serialize the NEXT size beside the PREVIOUS completed history.
            // Parse using the raw layout above, then expose the played size proven by empty hands, history and trick counters.
            if(s.handOver)s.handSize=played/s.players;
        }
        return s;
    }

    List<Integer> hand(int seat) {return Arrays.stream(cards[seat],0,remaining[seat]).boxed().toList();}
    List<Integer> plays() {
        return !history.isEmpty() && history.getFirst()==trumpCard && trump<4
                ? history.subList(1,history.size()) : history;
    }
    static String card(int id) {
        int rank=id%13+2; return (rank==14?"A":rank==13?"K":rank==12?"Q":rank==11?"J":Integer.toString(rank))
                + List.of("♠","♥","♣","♦").get(id/13);
    }
    String summary() {return "serialized hand index="+handIndex+"; played/current cards="+handSize+"; serialized size="+serializedHandSize+"; mode="+mode+"; dealer="+dealer
            +"; trump="+trump+"; turned card="+card(trumpCard)+"; bids="+Arrays.toString(bids)
            +"; won="+Arrays.toString(won)+"; remaining="+Arrays.toString(remaining)+"; plays="+plays().size()
            +"; handOver="+handOver+"; bidding="+bidding+"; scores="+Arrays.deepToString(scores);}
    private static int signedLow(int i){int n=i&65535;return n==65535?-1:n;}
    private static int decode(int value,int[] key){int n=0;for(int i=0;i<30;i++)n|=((value>>>key[i])&1)<<i;return n;}
    private static String line(InputStream in)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();int b;
        while((b=in.read())!=-1 && b!='\n')out.write(b);
        return out.toString(StandardCharsets.US_ASCII);
    }
}
