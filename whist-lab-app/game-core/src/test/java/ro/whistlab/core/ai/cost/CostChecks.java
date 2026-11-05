package ro.whistlab.core.ai.cost;

import static org.junit.Assert.*;
import ro.whistlab.core.ai.simulation.PossibleDeals;
import ro.whistlab.core.rules.GameRules;

/** Domain and structural bounds, separate from the clocks and publication of a measurement. */
final class CostChecks {
    private CostChecks() { }

    static void legal(CostPositions.Scenario scenario, int value) {
        if (scenario.bidding()) {
            assertTrue(value >= 0 && value <= scenario.snapshot().handSize());
            int count = 0, sum = 0;
            for (var player : scenario.snapshot().players()) {
                if (player.bid() != null) { count++; sum += player.bid(); }
            }
            if (count == scenario.snapshot().players().size() - 1)
                assertNotEquals(scenario.snapshot().handSize(), sum + value);
        } else {
            var view = scenario.observation();
            var legal = GameRules.legalCards(view.hand(), view.trick().isEmpty() ? null : view.trick().get(0).card().suit(), view.trump(), view.mode());
            assertTrue(legal.stream().anyMatch(card -> card.suit().ordinal() * 13 + card.rank() - 2 == value));
        }
    }

    static void bounds(CostProfile profile, CostPositions.Scenario scenario, CostPlayer player, CostPlayer.Work work) {
        boolean independent = player == CostPlayer.INDEPENDENT;
        var events = work.events();
        var limits = profile.limits(player);
        long samples = events.getOrDefault("SAMPLE", 0L), worlds = events.getOrDefault("WORLD", 0L);
        assertTrue(samples <= (scenario.bidding() ? limits.bidSamples() : limits.cardSamples()));
        assertTrue(worlds <= (independent || scenario.bidding() ? samples : Math.max(samples, limits.enumeratedWorlds())));
        assertTrue(events.getOrDefault("PROOF", 0L) <= limits.proofNodes());
        var view = scenario.observation();
        int cards = view.seats().stream().mapToInt(seat -> seat.remaining()).sum();
        int choices = scenario.bidding() ? view.handSize() + 1 : view.hand().size();
        assertTrue(events.getOrDefault("ROLLOUT", 0L) <= worlds * choices * cards);
        assertTrue(events.getOrDefault("ENDGAME", 0L) <= endgameBound(scenario, independent, worlds, choices, cards));
        if (independent) assertTrue(events.getOrDefault("REJECTION", 0L) <= 64 * samples);
        else {
            long pool = PossibleDeals.unknownCards(view).size();
            assertTrue(events.getOrDefault("MATCHING", 0L) <= samples * (cards - view.hand().size()) * (pool + 1));
        }
    }

    private static long endgameBound(CostPositions.Scenario scenario, boolean independent, long worlds, int choices, int cards) {
        if (independent || scenario.observation().seats().stream().anyMatch(seat -> seat.remaining() > 3)) return 0;
        long leaves = 1;
        for (var seat : scenario.observation().seats())
            for (int factor = 2; factor <= seat.remaining(); factor++) leaves *= factor;
        return worlds * choices * (cards + 1) * leaves;
    }
}
