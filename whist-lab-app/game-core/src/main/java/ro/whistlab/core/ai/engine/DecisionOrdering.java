package ro.whistlab.core.ai.engine;

import java.util.Comparator;
import java.util.List;
import ro.whistlab.core.ai.engine.MonteCarloAi.BidOption;
import ro.whistlab.core.ai.profile.HardStrategyProfile;

/** Pure ordering of already evaluated alternatives, preserving established tie tolerances. */
final class DecisionOrdering {
    private DecisionOrdering() { }

    static int selectBid(
            List<BidOption> candidates, HardStrategyProfile profile, boolean safety) {
        if (candidates.isEmpty()) throw new IllegalArgumentException("No legal bid candidates");
        Comparator<BidOption> safeOrder =
                Comparator.comparingDouble(BidOption::exactContractProbability)
                        .thenComparingDouble(BidOption::expectedPoints)
                        .thenComparingInt(option -> -option.bid());
        if (profile == HardStrategyProfile.SCORE_FIRST) {
            return candidates.stream()
                    .max(
                            Comparator.comparingDouble(BidOption::expectedPoints)
                                    .thenComparingDouble(BidOption::exactContractProbability)
                                    .thenComparingInt(option -> -option.bid()))
                    .orElseThrow()
                    .bid();
        }
        if (profile.bidProbabilityGap() > 0.0 && safety) {
            double best =
                    candidates.stream()
                            .mapToDouble(BidOption::exactContractProbability)
                            .max()
                            .orElseThrow();
            return candidates.stream()
                    .filter(
                            option ->
                                    option.exactContractProbability()
                                            >= best - profile.bidProbabilityGap())
                    .min(
                            Comparator.comparingInt(BidOption::bid)
                                    .thenComparing(
                                            Comparator.comparingDouble(
                                                            BidOption::exactContractProbability)
                                                    .reversed())
                                    .thenComparing(
                                            Comparator.comparingDouble(BidOption::expectedPoints)
                                                    .reversed()))
                    .orElseThrow()
                    .bid();
        }
        Comparator<BidOption> order =
                safety
                        ? safeOrder
                        : Comparator.comparingDouble(BidOption::expectedPoints)
                                .thenComparingDouble(BidOption::exactContractProbability)
                                .thenComparingInt(option -> -option.bid());
        return candidates.stream().max(order).orElseThrow().bid();
    }

    static boolean prefer(
            boolean safety,
            boolean salvage,
            double exact,
            double points,
            double misses,
            double rival,
            double bestExact,
            double bestPoints,
            double bestMisses,
            double bestRival,
            HardStrategyProfile profile) {
        if (salvage) {
            if (profile == HardStrategyProfile.BURN_IF_DOOMED) {
                if (misses > bestMisses + 1e-9) return true;
                if (Math.abs(misses - bestMisses) > 1e-9) return false;
                if (points > bestPoints + 1e-9) return true;
                if (Math.abs(points - bestPoints) > 1e-9) return false;
                return rival < bestRival;
            }
            if (points > bestPoints + 1e-9) return true;
            if (Math.abs(points - bestPoints) > 1e-9) return false;
            if (misses > bestMisses + 1e-9) return true;
            if (Math.abs(misses - bestMisses) > 1e-9) return false;
            return rival < bestRival;
        }
        if (profile == HardStrategyProfile.SCORE_FIRST) {
            return points > bestPoints + 1e-9
                    || Math.abs(points - bestPoints) < 1e-9
                            && (exact > bestExact + 1e-9
                                    || Math.abs(exact - bestExact) < 1e-9 && rival < bestRival);
        }
        double primary = safety ? exact : points, bestPrimary = safety ? bestExact : bestPoints;
        return primary > bestPrimary + 1e-9
                || Math.abs(primary - bestPrimary) < 1e-9
                        && (points > bestPoints + 1e-9
                                || Math.abs(points - bestPoints) < 1e-9 && rival < bestRival);
    }
}
