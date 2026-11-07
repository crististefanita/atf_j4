package ro.whistlab.core.match;

import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.PublicSalvage;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.ai.profile.HardStrategyProfile;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;

/** Offline mock of every bid and public card in a complete Romanian 1-8-1 match. */
public final class OfflineMockMatchTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    static final List<AiDifficulty> DEFAULT_ROSTER = List.of(
            AiDifficulty.NORMAL, AiDifficulty.HARD, AiDifficulty.NORMAL, AiDifficulty.HARD);

    record BidEvent(int seat, String policy, int bid, int publicBidSumBefore, String strategyProfile,
            String decision, List<MonteCarloAi.BidOption> candidates) { }
    record CardEvent(int sequence, int trick, int seat, String policy, String card,
            int wonBefore, int bid, int needBefore, int opportunities,
            List<String> tableBefore, String decision, Boolean hardContractImpossible,
            int forcedMasterWins, List<MonteCarloAi.Candidate> candidateEstimates,
            int legalOptionCount) { }
    record SeatResult(int seat, String policy, int bid, int tricksWon, int points, int total) { }
    record HandRecord(int hand, int size, String trump, String turnedCard, int dealer,
            List<BidEvent> bids, List<CardEvent> plays, List<SeatResult> results) { }
    record MockMatch(int schemaVersion, String mode, long seed, String policiesBySeat,
            int handsPlayed, int publicCardDecisions, boolean complete, List<HandRecord> hands,
            List<Integer> finalScores, int hardQuotaImpossibleDecisions,
            int hardSalvageFallbackDecisions, int hardQuotaImpossibleNoChoice,
            int hardSalvageCertificateDecisions) { }
    record MatchRun(MockMatch match, List<String> privateDealCommitments) { }

    @Test public void runsAndCapturesACompleteFourSeatRomanian181MockMatch() throws Exception {
        long seed = Long.getLong("whist.offlineMatch.seed", 20261104L);
        List<AiDifficulty> roster = readRoster();
        MatchRun run = playMatch(seed, roster);
        writeIfRequested(run.match());
    }

    static MatchRun playMatch(long seed, List<AiDifficulty> roster) {
        return playMatch(seed,roster,HardStrategyProfile.CURRENT);
    }

    static MatchRun playMatch(long seed, List<AiDifficulty> roster, HardStrategyProfile hardProfile) {
        if (roster.size() != 4 || roster.stream().anyMatch(p -> p != AiDifficulty.NORMAL && p != AiDifficulty.HARD)) {
            throw new IllegalArgumentException("A mock roster must contain four Normal/Hard players");
        }
        GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, seed, true, 10)));
        List<HandRecord> hands = new ArrayList<>();
        List<String> privateDealCommitments = new ArrayList<>();
        int totalPlays = 0;
        while (true) {
            GameSession.Snapshot start = game.snapshot();
            privateDealCommitments.add(privateDealCommitment(start));
            List<BidEvent> bids = new ArrayList<>();
            List<CardEvent> plays = new ArrayList<>();
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                GameSession.Snapshot before = game.snapshot();
                int seat = before.biddingSeat();
                AiDifficulty policy = roster.get(seat);
                String profile=policy==AiDifficulty.HARD?hardProfile.name():"NORMAL_BASELINE";
                List<MonteCarloAi.BidOption> bidCandidates=List.of();
                String bidDecision="Normal isolated strength estimate";
                int bid;
                if(policy==AiDifficulty.HARD){
                    MonteCarloAi.BidDecision choice=MonteCarloAi.chooseBid(MonteCarloAi.observe(before,seat),hardProfile);
                    bid=choice.bid();bidCandidates=choice.candidates();bidDecision=choice.method();
                }else bid=AiPolicy.chooseBid(before, seat, policy);
                bids.add(new BidEvent(seat, policy.name(), bid, before.bidTotal(),profile,bidDecision,bidCandidates));
                game.submitBid(seat, bid);
            }
            while (game.snapshot().phase() == GamePhase.PLAYING) {
                GameSession.Snapshot before = game.snapshot();
                int seat = before.currentSeat();
                AiDifficulty policy = roster.get(seat);
                GameSession.PlayerSnapshot actor = before.players().get(seat);
                List<String> table = before.currentTrick().stream()
                        .map(play -> play.seat() + ":" + play.card().shortLabel()).toList();
                int need = actor.bid() - actor.tricksWon();
                int opportunities = (before.players().stream().mapToInt(p -> p.hand().size()).sum()
                        + before.currentTrick().size()) / before.players().size();
                int legalOptions = GameRules.legalCards(actor.hand(),
                        before.currentTrick().isEmpty() ? null : before.currentTrick().get(0).card().suit(),
                        before.trump(), before.mode()).size();
                Boolean impossible = null;
                int forced = 0;
                List<MonteCarloAi.Candidate> candidateEstimates = List.of();
                Card card;
                String decision;
                if (policy == AiDifficulty.HARD) {
                    MonteCarloAi.View view = MonteCarloAi.observe(before, seat);
                    PublicSalvage.Quota quota = PublicSalvage.quota(view);
                    impossible = quota.impossible();
                    forced = quota.forcedMasterTrumps();
                    MonteCarloAi.Decision choice = MonteCarloAi.chooseCard(view,hardProfile);
                    card = choice.card();
                    candidateEstimates = choice.candidates();
                    decision = choice.method();
                } else {
                    card = AiPolicy.chooseCard(before, seat, policy);
                    decision = "Normal contract-first legal heuristic";
                }
                plays.add(new CardEvent(totalPlays + plays.size() + 1,
                        before.history().size() / before.players().size() + 1, seat, policy.name(),
                        card.shortLabel(), actor.tricksWon(), actor.bid(), need, opportunities,
                        table, decision, impossible, forced, candidateEstimates, legalOptions));
                game.playCard(seat, card);
            }
            GameSession.Snapshot scored = game.snapshot();
            if (scored.phase() != GamePhase.SCORE) throw new IllegalStateException("Hand did not score");
            int handNumber = scored.handNumber();
            List<SeatResult> results = new ArrayList<>();
            for (GameSession.PlayerSnapshot player : scored.players()) {
                results.add(new SeatResult(player.seat(), roster.get(player.seat()).name(),
                        game.scoreHistory().bid(handNumber, player.seat()), player.tricksWon(),
                        game.scoreHistory().points(handNumber, player.seat()), player.totalScore()));
            }
            totalPlays += plays.size();
            hands.add(new HandRecord(handNumber, scored.handSize(), scored.trump().name(),
                    scored.turnedCard() == null ? null : scored.turnedCard().shortLabel(),
                    scored.dealerSeat(), bids, plays, results));
            if (scored.finalHand()) break;
            game.startNextHand();
        }
        GameSession.Snapshot finished = game.snapshot();
        assertEquals("hands.size() must equal 24", 24, hands.size());
        assertEquals("totalPlays must equal 376", 376, totalPlays);
        assertTrue("hands.stream().allMatch((hand)->hand.bids().size() == 4 && hand.plays().size() == hand.size() * 4 && hand.results().size() == 4) must hold", hands.stream().allMatch(hand -> hand.bids().size() == 4
                && hand.plays().size() == hand.size() * 4 && hand.results().size() == 4));
        List<Integer> finalScores = finished.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();
        List<CardEvent> hardPlays = hands.stream().flatMap(hand -> hand.plays().stream())
                .filter(play -> "HARD".equals(play.policy())).toList();
        int impossibleCount = (int) hardPlays.stream().filter(play -> Boolean.TRUE.equals(play.hardContractImpossible())).count();
        int fallbackCount = (int) hardPlays.stream().filter(play -> play.decision().contains("publicly unreachable contract")).count();
        int noChoiceCount = (int) hardPlays.stream().filter(play -> Boolean.TRUE.equals(play.hardContractImpossible())
                && play.legalOptionCount() == 1).count();
        int certificateCount = (int) hardPlays.stream().filter(play -> play.decision().contains("public salvage dominance certificate")).count();
        if (seed == 20261104L && roster.equals(DEFAULT_ROSTER)) {
            assertEquals("impossibleCount must equal 17", 17, impossibleCount);
            assertEquals("fallbackCount must equal 6", 6, fallbackCount);
            assertEquals("noChoiceCount must equal 11", 11, noChoiceCount);
            assertTrue("hardPlays.stream().filter((play)->play.decision().contains(\"publicly unreachable contract\")).allMatch(OfflineMockMatchTest::salvageChoiceMatchesCandidateRanking) must hold", hardPlays.stream().filter(play -> play.decision().contains("publicly unreachable contract"))
                    .allMatch(OfflineMockMatchTest::salvageChoiceMatchesCandidateRanking));
        }
        MockMatch result = new MockMatch(3, GameMode.ROMANIAN_WHIST.name(), seed,
                rosterLabel(roster), hands.size(), totalPlays,
                finished.phase() == GamePhase.SCORE && finished.finalHand(), hands, finalScores,
                impossibleCount, fallbackCount, noChoiceCount, certificateCount);
        return new MatchRun(result, List.copyOf(privateDealCommitments));
    }

    private static List<AiDifficulty> readRoster() {
        String configured = System.getProperty("whist.offlineMatch.roster", "NORMAL,HARD,NORMAL,HARD");
        List<AiDifficulty> roster = java.util.Arrays.stream(configured.split(","))
                .map(String::strip).map(AiDifficulty::valueOf).toList();
        if (roster.size() != 4) throw new IllegalArgumentException("whist.offlineMatch.roster needs four seats");
        return roster;
    }

    static String rosterLabel(List<AiDifficulty> roster) {
        return java.util.stream.IntStream.range(0, roster.size())
                .mapToObj(seat -> "seat" + seat + "=" + roster.get(seat).name())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String privateDealCommitment(GameSession.Snapshot game) {
        StringBuilder canonical = new StringBuilder().append(game.handNumber()).append('|')
                .append(game.dealerSeat()).append('|').append(game.trump().name()).append('|');
        if (game.turnedCard() != null) canonical.append(game.turnedCard().shortLabel());
        for (GameSession.PlayerSnapshot player : game.players()) {
            canonical.append('|').append(player.seat()).append(':');
            player.hand().stream().sorted(Comparator.comparing(Card::suit).thenComparingInt(Card::rank))
                    .forEach(card -> canonical.append(card.suit().name()).append(card.rank()).append(','));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }

    private static boolean salvageChoiceMatchesCandidateRanking(CardEvent play) {
        MonteCarloAi.Candidate chosen = play.candidateEstimates().stream()
                .filter(candidate -> candidate.card().shortLabel().equals(play.card())).findFirst().orElse(null);
        if (chosen == null) return false;
        for (MonteCarloAi.Candidate other : play.candidateEstimates()) {
            if (chosen.meanContractPoints() < other.meanContractPoints() - 1e-9) return false;
            if (Math.abs(chosen.meanContractPoints() - other.meanContractPoints()) <= 1e-9) {
                if (chosen.meanRivalMisses() < other.meanRivalMisses() - 1e-9) return false;
                if (Math.abs(chosen.meanRivalMisses() - other.meanRivalMisses()) <= 1e-9
                        && chosen.meanStrongestRivalScore() > other.meanStrongestRivalScore() + 1e-9) return false;
            }
        }
        return true;
    }

    private static void writeIfRequested(MockMatch result) throws Exception {
        String configured = System.getProperty("whist.offlineMatch.output", "").strip();
        if (configured.isEmpty()) return;
        Path output = Path.of(configured);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n",
                StandardOpenOption.CREATE_NEW);
    }
}
