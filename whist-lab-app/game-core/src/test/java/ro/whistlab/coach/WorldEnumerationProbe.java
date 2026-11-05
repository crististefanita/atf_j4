package ro.whistlab.coach;

import java.util.List;
import java.util.Random;

/** Test adapter for hypothetical worlds; keeps diagnostic operations out of the player public API. */
public final class WorldEnumerationProbe {
    public record Proof(int card, boolean certified, int worlds, int nodes) {}

    private WorldEnumerationProbe() {}

    public static Proof certificate(IndependentPlayer.Position p, int selected, int limit) {
        var result = ContractGuarantee.choose(p, selected, limit);
        return new Proof(result.card(), result.certified(), result.worlds(), result.nodes());
    }

    public static List<List<List<Integer>>> enumerate(IndependentPlayer.Position p) {
        return IndependentPlayer.smallDeals(p);
    }

    public static int[] continueAfter(
            IndependentPlayer.Position p, List<List<Integer>> hands,
            int first, int[] goals, Random random) {
        return IndependentContinuations.roll(p, hands, first, goals, random);
    }
}
