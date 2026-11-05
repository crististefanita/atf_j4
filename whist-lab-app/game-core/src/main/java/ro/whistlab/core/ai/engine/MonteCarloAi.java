package ro.whistlab.core.ai.engine;

import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.ai.simulation.PossibleDeals;
import ro.whistlab.core.ai.simulation.RolloutPolicy;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.ImmutableLists;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;
import ro.whistlab.core.session.GameSession;

import java.util.*;

/** Information-set sampling. The decision API cannot represent opponents' private cards. */
public final class MonteCarloAi {
    private static final int SAMPLES = 32;

    private MonteCarloAi() {}

    public record Seat(int remaining, Integer bid, int won, int total, int streak) {
        public Seat(int remaining, Integer bid, int won) {
            this(remaining, bid, won, 0);
        }

        public Seat(int remaining, Integer bid, int won, int total) {
            this(remaining, bid, won, total, 0);
        }
    }

    public record View(
            GameMode mode,
            Trump trump,
            int seat,
            int dealer,
            int handSize,
            List<Card> hand,
            List<Seat> seats,
            List<PlayedCard> trick,
            List<PlayedCard> history,
            Card turnedCard,
            boolean streakBonuses,
            int streakBonusPoints, int streakPenaltyPoints) {
        /** Legacy projections retain their historical five-point penalty. */
        public View(
            GameMode mode,
            Trump trump,
            int seat,
            int dealer,
            int handSize,
            List<Card> hand,
            List<Seat> seats,
            List<PlayedCard> trick,
            List<PlayedCard> history,
            Card turnedCard,
            boolean streakBonuses,
            int streakBonusPoints) {
            this(mode, trump, seat, dealer, handSize, hand, seats, trick, history, turnedCard, streakBonuses, streakBonusPoints, 5);
        }
        public View(
                GameMode mode,
                Trump trump,
                int seat,
                int dealer,
                int handSize,
                List<Card> hand,
                List<Seat> seats,
                List<PlayedCard> trick,
                List<PlayedCard> history,
                Card turnedCard) {
            this(
                    mode,
                    trump,
                    seat,
                    dealer,
                    handSize,
                    hand,
                    seats,
                    trick,
                    history,
                    turnedCard,
                    false,
                    5);
        }

        public View(
                GameMode mode,
                Trump trump,
                int seat,
                int dealer,
                int handSize,
                List<Card> hand,
                List<Seat> seats,
                List<PlayedCard> trick,
                List<PlayedCard> history) {
            this(mode, trump, seat, dealer, handSize, hand, seats, trick, history, null);
        }

        public View {
            List<Card> ordered = new ArrayList<>(hand);
            for (Card c : ordered) Objects.requireNonNull(c, "hand card");
            ordered.sort(Comparator.comparing(Card::suit).thenComparingInt(Card::rank));
            hand = ImmutableLists.copy(ordered);
            seats = List.copyOf(seats);
            trick = List.copyOf(trick);
            history = List.copyOf(history);
        }
    }

    public record Candidate(
            Card card,
            double meanContractPoints,
            int samples,
            double meanRivalMisses,
            double meanStrongestRivalScore,
            double exactContractProbability) {
        public Candidate(Card card, double meanContractPoints, int samples) {
            this(card, meanContractPoints, samples, 0.0, 0.0, 0.0);
        }

        public Candidate(
                Card card, double meanContractPoints, int samples, double meanRivalMisses) {
            this(card, meanContractPoints, samples, meanRivalMisses, 0.0, 0.0);
        }

        public Candidate(
                Card card,
                double meanContractPoints,
                int samples,
                double meanRivalMisses,
                double meanStrongestRivalScore) {
            this(card, meanContractPoints, samples, meanRivalMisses, meanStrongestRivalScore, 0.0);
        }
    }

    public record BidOption(
            int bid, double exactContractProbability, double expectedPoints, int samples) {}

    public record BidDecision(int bid, List<BidOption> candidates, String method) {
        public BidDecision {
            candidates = List.copyOf(candidates);
        }
    }

    public record Decision(Card card, List<Candidate> candidates, String method) {
        public Decision {
            candidates = List.copyOf(candidates);
        }
    }

    public static View observe(GameSession.Snapshot game, int seat) {
        return new View(
                game.mode(),
                game.trump(),
                seat,
                game.dealerSeat(),
                game.handSize(),
                game.players().get(seat).hand(),
                game.players().stream()
                        .map(
                                p ->
                                        new Seat(
                                                p.hand().size(),
                                                p.bid(),
                                                p.tricksWon(),
                                                p.totalScore(),
                                                p.streak()))
                        .collect(java.util.stream.Collectors.toList()),
                game.currentTrick(),
                game.history(),
                game.turnedCard(),
                game.streakBonuses(),
                game.streakBonusPoints(), game.streakPenaltyPoints());
    }

    public static int chooseBid(View view) {
        return chooseBid(view, HardStrategyProfile.CURRENT).bid();
    }

    public static BidDecision chooseBid(View view, HardStrategyProfile profile) {
        int prior = view.seats().stream().filter(s -> s.bid() != null).mapToInt(s -> s.bid()).sum();
        boolean last =
                view.seats().stream().filter(s -> s.bid() != null).count()
                        == view.seats().size() - 1;
        List<Integer> allowed = GameRules.allowedBids(view.handSize(), prior, last);
        boolean safety = view.mode().isRomanian();
        if (view.handSize() == 1
                && view.hand().size() == 1
                && view.trick().isEmpty()
                && view.history().isEmpty()) {
            double win = OneCardBidBelief.probability(view);
            List<BidOption> candidates = new ArrayList<>();
            for (int bid : allowed) {
                double chance = bid == 1 ? win : 1 - win;
                double expected =
                        win * points(view, view.seat(), 1, bid)
                                + (1 - win) * points(view, view.seat(), 0, bid);
                candidates.add(new BidOption(bid, chance, expected, 1));
            }
            int selected = selectBid(candidates, profile, safety);
            return new BidDecision(
                    selected, candidates, "one-card exact probability; " + profile.name());
        }
        var evidence = new BidSamples(allowed, view.handSize() + 1, safety);
        int budget = safety ? 128 : SAMPLES;
        for (int sample = 0; sample < budget; sample++) {
            List<List<Card>> hands = sample(view, new Random(seed(view, sample)));
            if (hands == null) continue;
            double[] points = new double[view.handSize() + 1],
                    exact = new double[view.handSize() + 1];
            for (int bid : allowed) {
                int[] targets = biddingTargets(view, hands, bid);
                double[] result =
                        evaluate(view, hands, null, targets, seed(view, sample) ^ 0xB1D2026L, .5);
                points[bid] = result[0];
                exact[bid] = result[2];
            }
            evidence.add(points, exact);
            if ((sample + 1) % 32 == 0 && evidence.enough()) break;
        }
        List<BidOption> candidates = evidence.options();
        int selected = selectBid(candidates, profile, safety);
        return new BidDecision(
                selected, candidates, "paired-world bid estimates; " + profile.name());
    }

    public static int selectBid(
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

    /** Paired-world uncertainty, bounded to128 samples; not a formal confidence guarantee. */
    public static final class BidSamples {
        private final List<Integer> allowed;
        private final boolean safety;
        private final double[] primary, points, exactTotals;
        private final double[][] squaredDifferences;
        private int count;

        public BidSamples(List<Integer> allowed, int size, boolean safety) {
            this.allowed = List.copyOf(allowed);
            if (allowed.isEmpty()) throw new IllegalArgumentException("No legal bid");
            this.safety = safety;
            primary = new double[size];
            points = new double[size];
            exactTotals = new double[size];
            squaredDifferences = new double[size][size];
        }

        public void add(double[] scores, double[] exact) {
            for (int bid : allowed) {
                double value = safety ? exact[bid] : scores[bid];
                if (!Double.isFinite(value)
                        || !Double.isFinite(scores[bid])
                        || !Double.isFinite(exact[bid]))
                    throw new IllegalArgumentException("Non-finite sampled utility");
                primary[bid] += value;
                points[bid] += scores[bid];
                exactTotals[bid] += exact[bid];
            }
            for (int a : allowed)
                for (int b : allowed) {
                    double d = safety ? exact[a] - exact[b] : scores[a] - scores[b];
                    squaredDifferences[a][b] += d * d;
                }
            count++;
        }

        public int best() {
            int best = allowed.get(0);
            for (int bid : allowed)
                if (primary[bid] > primary[best] + 1e-12
                        || Math.abs(primary[bid] - primary[best]) < 1e-12
                                && points[bid] > points[best] + 1e-12) best = bid;
            return best;
        }

        List<BidOption> options() {
            return Collections.unmodifiableList(
                    allowed.stream()
                            .map(
                                    bid ->
                                            new BidOption(
                                                    bid,
                                                    count == 0 ? 0.0 : exactTotals[bid] / count,
                                                    count == 0 ? 0.0 : points[bid] / count,
                                                    count))
                            .collect(java.util.stream.Collectors.toList()));
        }

        public boolean enough() {
            if (count < 32) return false;
            if (count >= 128 || allowed.size() == 1) return true;
            int leader = best();
            for (int rival : allowed)
                if (rival != leader) {
                    double gap = (primary[leader] - primary[rival]) / count;
                    double variance =
                            Math.max(
                                    0,
                                    (squaredDifferences[leader][rival] - count * gap * gap)
                                            / (count - 1));
                    if (variance > 1e-15 && gap <= 2 * Math.sqrt(variance / count)) return false;
                }
            return true;
        }

        public int samples() {
            return count;
        }
    }

    public static Decision chooseCard(View view) {
        return chooseCard(view, .5, HardStrategyProfile.CURRENT);
    }

    public static Decision chooseCard(View view, double exactWeight) {
        return chooseCard(view, exactWeight, HardStrategyProfile.CURRENT);
    }

    public static Decision chooseCard(View view, HardStrategyProfile profile) {
        return chooseCard(view, .5, profile);
    }

    private static Decision chooseCard(View view, double exactWeight, HardStrategyProfile profile) {
        var model = modelCard(view, exactWeight, profile);
        var proof = ContractGuarantee.choose(view, model.card());
        Decision accepted =
                proof.certified() && !proof.card().equals(model.card())
                        ? new Decision(
                                proof.card(),
                                model.candidates(),
                                model.method()
                                        + "; public contract certificate across"
                                        + proof.worlds()
                                        + " complete feasible deals and all legal continuations")
                        : model;
        var salvage = PublicSalvage.choose(view, accepted.card());
        return salvage.certified()
                ? new Decision(
                        salvage.card(),
                        accepted.candidates(),
                        accepted.method()
                                + "; public salvage dominance certificate: "
                                + salvage.worlds()
                                + " complete worlds, all legal continuations, no worse own points, at least "
                                + salvage.rivalMissFloor()
                                + " rival misses versus baseline ceiling "
                                + salvage.baselineRivalMissCeiling())
                : accepted;
    }

    private static Decision modelCard(View view, double exactWeight, HardStrategyProfile profile) {
        Suit led = view.trick().isEmpty() ? null : view.trick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(view.hand(), led, view.trump(), view.mode());
        if (legal.isEmpty())
            throw new IllegalArgumentException("No legal card for the observed player");
        if (legal.size() == 1)
            return new Decision(
                    legal.get(0),
                    List.of(),
                    "Only legal card; no hidden-deal sampling or score estimate required");
        boolean salvage = view.mode().isRomanian() && PublicSalvage.quota(view).impossible();
        Card dominant = LateDiscard.choose(view);
        if (dominant != null && !salvage)
            return new Decision(
                    dominant,
                    List.of(),
                    "Two-card same-suit dominance: current trick already lost, or last-seat win fixed; retain high only if one further trick is needed. No hidden cards or samples used.");
        List<Candidate> evaluations = new ArrayList<>();
        int[] targets =
                view.seats().stream().mapToInt(s -> s.bid() == null ? 0 : s.bid()).toArray();
        double[] values = new double[legal.size()],
                rivals = new double[legal.size()],
                exact = new double[legal.size()],
                misses = new double[legal.size()];
        int samples = 0;
        List<List<List<Card>>> exactDeals =
                unknownCards(view).size() <= 7 ? enumerateSmallDeals(view) : List.of();
        int worlds = exactDeals.isEmpty() ? SAMPLES : exactDeals.size();
        for (int sample = 0; sample < worlds; sample++) {
            List<List<Card>> hands =
                    exactDeals.isEmpty()
                            ? sample(view, new Random(seed(view, sample)))
                            : exactDeals.get(sample);
            if (hands == null) continue;
            samples++;
            for (int c = 0; c < legal.size(); c++) {
                double[] value =
                        evaluate(
                                view,
                                hands,
                                legal.get(c),
                                targets,
                                seed(view, sample) ^ 0xCA4D2026L,
                                exactWeight);
                values[c] += value[0];
                rivals[c] += value[1];
                exact[c] += value[2];
                misses[c] += value[3];
            }
        }
        int best = 0;
        for (int c = 0; c < legal.size(); c++) {
            evaluations.add(
                    new Candidate(
                            legal.get(c),
                            samples == 0 ? Double.NEGATIVE_INFINITY : values[c] / samples,
                            samples,
                            samples == 0 ? 0.0 : misses[c] / samples,
                            samples == 0 ? 0.0 : rivals[c] / samples,
                            samples == 0 ? 0.0 : exact[c] / samples));
            if (prefer(
                    view.mode().isRomanian(),
                    salvage,
                    exact[c],
                    values[c],
                    misses[c],
                    rivals[c],
                    exact[best],
                    values[best],
                    misses[best],
                    rivals[best],
                    profile)) best = c;
        }
        String objective =
                salvage
                        ? (profile == HardStrategyProfile.BURN_IF_DOOMED
                                ? "publicly unreachable contract: maximize expected rival misses, then preserve own points"
                                : "publicly unreachable contract: preserve own points, then maximize expected rival misses")
                        : profile == HardStrategyProfile.SCORE_FIRST
                                ? "maximize expected score, then exact-contract probability"
                                : "maximize exact-contract probability, then expected score";
        return new Decision(
                legal.get(best),
                evaluations,
                samples
                        + (exactDeals.isEmpty() ? " sampled" : " exhaustively enumerated")
                        + " feasible common possible deals; public void inference; small-endgame exact/heuristic mixture weight="
                        + exactWeight
                        + "; "
                        + profile.name()
                        + "; "
                        + objective);
    }

    public static boolean prefer(
            boolean safety,
            double exact,
            double points,
            double rival,
            double bestExact,
            double bestPoints,
            double bestRival) {
        return prefer(safety, false, exact, points, 0, rival, bestExact, bestPoints, 0, bestRival);
    }

    public static boolean prefer(
            boolean safety,
            boolean salvage,
            double exact,
            double points,
            double misses,
            double rival,
            double bestExact,
            double bestPoints,
            double bestMisses,
            double bestRival) {
        return prefer(
                safety,
                salvage,
                exact,
                points,
                misses,
                rival,
                bestExact,
                bestPoints,
                bestMisses,
                bestRival,
                HardStrategyProfile.CURRENT);
    }

    public static boolean prefer(
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

    public static double oneCardWinProbability(View v) {
        Card own = v.hand().get(0);
        List<Card> pool = unknownCards(v);
        int threats = 0, lower = 0;
        boolean ownTrump = own.suit().ordinal() == v.trump().ordinal();
        for (Card c : pool) {
            if (c.suit() == own.suit() && c.rank() < own.rank()) lower++;
            if (ownTrump
                    ? c.suit() == own.suit() && c.rank() > own.rank()
                    : c.suit().ordinal() == v.trump().ordinal()
                            || c.suit() == own.suit() && c.rank() > own.rank()) threats++;
        }
        int opponents = v.seats().size() - 1;
        if (ownTrump || v.seat() == (v.dealer() + 1) % v.seats().size())
            return avoid(pool.size(), threats, opponents);
        return lower / (double) pool.size() * avoid(pool.size() - 1, threats, opponents - 1);
    }

    private static double avoid(int pool, int threats, int draws) {
        double p = 1;
        for (int i = 0; i < draws; i++) {
            if (pool - threats - i <= 0) return 0;
            p *= (pool - threats - i) / (double) (pool - i);
        }
        return p;
    }

    public static List<List<List<Card>>> enumerateSmallDeals(View v) {
        return PossibleDeals.enumerateSmallDeals(v);
    }

    private static double[] evaluate(
            View view,
            List<List<Card>> hands,
            Card first,
            int[] targets,
            long seed,
            double exactWeight) {
        int[] greedy = rollout(view, copy(hands), first, targets, new Random(seed));
        double[] result = utility(view, greedy, targets);
        if (exactWeight > 0
                && hands.stream().allMatch(h -> h.size() <= (hands.size() == 4 ? 3 : 2))) {
            double[] exact =
                    utility(view, ContractEndgame.after(view, hands, first, targets), targets);
            for (int i = 0; i < result.length; i++)
                result[i] = (1 - exactWeight) * result[i] + exactWeight * exact[i];
        }
        return result;
    }

    public static double[] utility(View view, int[] won, int[] targets) {
        int rival = Integer.MIN_VALUE, misses = 0;
        for (int s = 0; s < won.length; s++)
            if (s != view.seat()) {
                rival =
                        Math.max(
                                rival,
                                view.seats().get(s).total() + points(view, s, won[s], targets[s]));
                if (won[s] != targets[s]) misses++;
            }
        return new double[] {
            points(view, view.seat(), won[view.seat()], targets[view.seat()]),
            rival,
            won[view.seat()] == targets[view.seat()] ? 1 : 0,
            misses
        };
    }

    public static int points(View view, int seat, int won, int bid) {
        return view.streakBonuses()
                ? RomanianScoring.score(
                                won,
                                bid,
                                view.handSize(),
                                view.seats().get(seat).streak(),
                                view.streakBonusPoints(), view.streakPenaltyPoints())
                        .totalPoints()
                : GameRules.scoreDelta(won, bid, view.mode());
    }

    private static List<List<Card>> copy(List<List<Card>> hands) {
        return PossibleDeals.copy(hands);
    }

    public static int[] biddingTargets(View view, List<List<Card>> hands, int own) {
        int[] targets = new int[hands.size()];
        int sum = 0;
        for (int i = 1; i <= hands.size(); i++) {
            int seat = (view.dealer() + i) % hands.size();
            Integer known = view.seats().get(seat).bid();
            int target =
                    seat == view.seat()
                            ? own
                            : known != null ? known : estimate(hands.get(seat), view.trump());
            if (i == hands.size()
                    && seat != view.seat()
                    && known == null
                    && sum + target == view.handSize()) target = target > 0 ? target - 1 : 1;
            targets[seat] = target;
            sum += target;
        }
        return targets;
    }

    private static int estimate(List<Card> hand, Trump trump) {
        double value = 0;
        for (Card c : hand)
            value +=
                    (c.rank() == 14
                                    ? 1
                                    : c.rank() == 13
                                            ? .7
                                            : c.rank() == 12 ? .4 : c.rank() == 11 ? .2 : .03)
                            + (c.suit().ordinal() == trump.ordinal() ? .2 : 0);
        return Math.min(hand.size(), (int) Math.round(value));
    }

    private static long seed(View view, int sample) {
        // Neither wall-clock time nor hidden hands influence decisions. Replays are reproducible.
        long seed = 0x51A7C0DEL + sample * 0x9E3779B97F4A7C15L;
        seed = seed * 31 + view.mode().samplingId();
        seed = seed * 31 + view.trump().ordinal();
        seed = seed * 31 + view.seat();
        seed = seed * 31 + view.dealer();
        seed = seed * 31 + view.handSize();
        for (Card c : view.hand()) {
            seed = seed * 31 + c.suit().ordinal();
            seed = seed * 31 + c.rank();
        }
        for (Seat s : view.seats()) {
            seed = seed * 31 + s.remaining();
            seed = seed * 31 + (s.bid() == null ? -1 : s.bid());
            seed = seed * 31 + s.won();
            seed = seed * 31 + s.total();
        }
        for (PlayedCard p : view.history()) {
            seed = seed * 31 + p.seat();
            seed = seed * 31 + p.card().suit().ordinal();
            seed = seed * 31 + p.card().rank();
        }
        if (view.turnedCard() != null) {
            seed = seed * 31 + view.turnedCard().suit().ordinal();
            seed = seed * 31 + view.turnedCard().rank();
        }
        // Rule rewards affect utility, not the distribution of cards in otherwise identical public
        // states.
        return seed;
    }

    public static List<Card> unknownCards(View view) {
        return PossibleDeals.unknownCards(view);
    }

    static List<List<Card>> sample(View view, Random random) {
        return PossibleDeals.sample(view, random);
    }

    private static boolean[][] knownVoids(View view) {
        return PossibleDeals.knownVoids(view);
    }

    private static int[] rollout(
            View view, List<List<Card>> hands, Card first, int[] targets, Random random) {
        return RolloutPolicy.rollout(view, hands, first, targets, random);
    }

    private static Card greedy(
            List<Card> hand,
            List<PlayedCard> trick,
            View view,
            int seat,
            int needed,
            Random random) {
        return RolloutPolicy.greedy(hand, trick, view, seat, needed, random);
    }
}
