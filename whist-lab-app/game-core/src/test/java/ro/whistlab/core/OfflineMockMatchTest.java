package ro.whistlab.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Offline mock of every bid and card in a complete Romanian 1-8-1 match. */
public final class OfflineMockMatchTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<AiDifficulty> POLICIES = List.of(
            AiDifficulty.NORMAL, AiDifficulty.HARD, AiDifficulty.NORMAL, AiDifficulty.HARD);
    record BidEvent(int seat, String policy, int bid, int publicBidSumBefore) { }
    record CardEvent(int sequence, int trick, int seat, String policy, String card,
            int wonBefore, int bid, int needBefore, int opportunities,
            List<String> tableBefore, String decision, Boolean hardContractImpossible,
            int forcedMasterWins) { }
    record SeatResult(int seat, String policy, int bid, int tricksWon, int points, int total) { }
    record HandRecord(int hand, int size, String trump, String turnedCard, int dealer,
            List<BidEvent> bids, List<CardEvent> plays, List<SeatResult> results) { }
    record MockMatch(int schemaVersion, String mode, long seed, String policiesBySeat,
            int handsPlayed, int publicCardDecisions, boolean complete, List<HandRecord> hands,
            List<Integer> finalScores, int hardSalvageDecisions) { }

    @Test public void runsAndCapturesACompleteFourSeatRomanian181MockMatch() throws Exception {
        long seed = Long.getLong("whist.offlineMatch.seed", 20261104L);
        GameSession game = GameSession.newGame(new GameConfig(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, seed, true, 10));
        List<HandRecord> hands = new ArrayList<>();
        int totalPlays = 0;
        int salvage = 0;
        while (true) {
            List<BidEvent> bids = new ArrayList<>();
            List<CardEvent> plays = new ArrayList<>();
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                GameSession.Snapshot before = game.snapshot();
                int seat = before.biddingSeat();
                AiDifficulty policy = POLICIES.get(seat);
                int bid = AiPolicy.chooseBid(before, seat, policy);
                bids.add(new BidEvent(seat, policy.name(), bid, before.bidTotal()));
                game.submitBid(seat, bid);
            }
            while (game.snapshot().phase() == GamePhase.PLAYING) {
                GameSession.Snapshot before = game.snapshot();
                int seat = before.currentSeat();
                AiDifficulty policy = POLICIES.get(seat);
                GameSession.PlayerSnapshot actor = before.players().get(seat);
                List<String> table = before.currentTrick().stream()
                        .map(play -> play.seat() + ":" + play.card().shortLabel()).toList();
                int need = actor.bid() - actor.tricksWon();
                int opportunities = (before.players().stream().mapToInt(p -> p.hand().size()).sum()
                        + before.currentTrick().size()) / before.players().size();
                Boolean impossible = null;
                int forced = 0;
                Card card;
                String decision;
                if (policy == AiDifficulty.HARD) {
                    MonteCarloAi.View view = MonteCarloAi.observe(before, seat);
                    PublicSalvage.Quota quota = PublicSalvage.quota(view);
                    impossible = quota.impossible();
                    forced = quota.forcedMasterTrumps();
                    MonteCarloAi.Decision choice = MonteCarloAi.chooseCard(view);
                    card = choice.card();
                    decision = choice.method();
                    if (decision.contains("public salvage dominance certificate")) salvage++;
                } else {
                    card = AiPolicy.chooseCard(before, seat, policy);
                    decision = "Normal contract-first legal heuristic";
                }
                plays.add(new CardEvent(totalPlays + plays.size() + 1,
                        before.history().size() / before.players().size() + 1, seat, policy.name(),
                        card.shortLabel(), actor.tricksWon(), actor.bid(), need, opportunities,
                        table, decision, impossible, forced));
                game.playCard(seat, card);
            }
            GameSession.Snapshot scored = game.snapshot();
            assertEquals(GamePhase.SCORE, scored.phase());
            int handNumber = scored.handNumber();
            List<SeatResult> results = new ArrayList<>();
            for (GameSession.PlayerSnapshot player : scored.players()) {
                results.add(new SeatResult(player.seat(), POLICIES.get(player.seat()).name(),
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
        assertEquals(24, hands.size());
        assertEquals(376, totalPlays);
        assertTrue(hands.stream().allMatch(hand -> hand.bids().size() == 4
                && hand.plays().size() == hand.size() * 4 && hand.results().size() == 4));
        assertTrue(hands.stream().flatMap(hand -> hand.plays().stream())
                .anyMatch(play -> "HARD".equals(play.policy())));
        List<Integer> finalScores = finished.players().stream().map(GameSession.PlayerSnapshot::totalScore).toList();
        MockMatch result = new MockMatch(1, GameMode.ROMANIAN_WHIST.name(), seed,
                "seat0=NORMAL,seat1=HARD,seat2=NORMAL,seat3=HARD", hands.size(), totalPlays,
                finished.phase() == GamePhase.SCORE && finished.finalHand(), hands, finalScores, salvage);
        writeIfRequested(result);
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