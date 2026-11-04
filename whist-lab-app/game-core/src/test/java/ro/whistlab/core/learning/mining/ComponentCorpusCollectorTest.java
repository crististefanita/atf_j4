package ro.whistlab.core.learning.mining;

import ro.whistlab.core.testing.match.OfflineMatchHarness.Policy;
import static ro.whistlab.core.testing.match.OfflineMatchHarness.play;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;

/** Explicit collection entrypoint; simulation and public projections belong to shared test support. */
public class ComponentCorpusCollectorTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Test public void collectsEveryDeclaredGameIncludingLossesAsSimpleJson()throws Exception {
        String output=System.getProperty("whist.corpusDir","");assertFalse("Explicit external corpus folder required",output.isBlank());
        Path folder=Path.of(output).toAbsolutePath().resolve("games/development");Files.createDirectories(folder);
        int first=number("whist.corpusFirstSeed",800),seeds=number("whist.corpusSeeds",64);assertTrue(seeds>0);
        String variant=System.getProperty("whist.corpusVariant","");if(variant.isBlank())variant="V2_BASELINE";
        Policy hero=switch(variant){case "V2_BASELINE"->Policy.PLAYER_V2;case "PLAYER_CANDIDATE"->Policy.PLAYER_CANDIDATE;case "EXPERT_CANDIDATE"->Policy.EXPERT_CANDIDATE;default->throw new IllegalArgumentException("Unknown corpus variant: "+variant);};
        Policy opponent=switch(System.getProperty("whist.corpusOpponent","")){case "", "V2_EXPERT", "EXPERT_V2"->Policy.EXPERT_V2;case "V2_PLAYER", "PLAYER_V2"->Policy.PLAYER_V2;case "EXPERT_CANDIDATE"->Policy.EXPERT_CANDIDATE;case "PLAYER_CANDIDATE"->Policy.PLAYER_CANDIDATE;default->throw new IllegalArgumentException("Unknown corpus opponent");};
        int completed=0,created=0;long started=System.nanoTime();
        for(int seed=first;seed<first+seeds;seed++)for(int seat=0;seat<4;seat++){
            String id="component-"+variant.toLowerCase(Locale.ROOT)+"-"+opponent.name().toLowerCase(Locale.ROOT)+"-seed"+seed+"-hero"+seat;
            Path target=folder.resolve(id+".json");
            if(Files.exists(target)){var existing=JSON.readTree(Files.readString(target));assertEquals(id,existing.path("id").asText());assertEquals(24,existing.path("hands").size());}
            else {
                var game=play(id,seed,seat,hero,opponent);String encoded=JSON.writerWithDefaultPrettyPrinter().writeValueAsString(game)+"\n";
                Path temporary=Files.createTempFile(folder,id+"-",".tmp");Files.writeString(temporary,encoded);
                try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException notAtomic){Files.move(temporary,target);}created++;
            }
            completed++;Properties progress=new Properties();progress.setProperty("status","RUNNING");progress.setProperty("variant",variant);progress.setProperty("completed",""+completed);progress.setProperty("created",""+created);progress.setProperty("planned",""+(seeds*4));progress.setProperty("elapsedMillis",""+(System.nanoTime()-started)/1_000_000);
            try(var stream=Files.newOutputStream(Path.of(output).resolve("collection-progress.properties"))){progress.store(stream,"Every loss and tie retained; replay validation follows collection");}
        }
        assertEquals(seeds*4,completed);
        Properties done=new Properties();done.setProperty("status","PASS");done.setProperty("variant",variant);done.setProperty("completed",""+completed);done.setProperty("created",""+created);done.setProperty("planned",""+(seeds*4));try(var stream=Files.newOutputStream(Path.of(output).resolve("collection-progress.properties"))){done.store(stream,"Collection complete; independent replay validation is a separate receipt");}
    }

    private static int number(String key,int fallback){String value=System.getProperty(key,"");return value.isBlank()?fallback:Integer.parseInt(value);}
}
