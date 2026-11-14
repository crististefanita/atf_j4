package ro.whistlab.app.fixtures;

import android.content.Intent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ro.whistlab.app.bidding.HumanBidSelection;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Owns deterministic launch fixtures; this implementation is compiled only into debug builds. */
public final class DebugFixtureFactory {
    public enum Scenario {
        REPLAY,
        ONE_TRICK,
        TRICK_PAUSE,
        PLAYED_CARD,
        HARD_AI_BID,
        HAND_FAN,
        NINE_BIDS,
        COMPACT_TABLE
    }

    private DebugFixtureFactory() { }

    public static DebugLaunch fromIntent(Intent intent, File filesDirectory) throws IOException {
        Scenario scenario = requestedScenario(intent);
        if (scenario == null) return null;
        boolean persistFixture = scenario == Scenario.ONE_TRICK
                && intent.getBooleanExtra("persist_fixture", false);
        return create(scenario, persistFixture, filesDirectory);
    }

    public static DebugLaunch create(Scenario scenario, boolean persistFixture, File filesDirectory) throws IOException {
        return switch (scenario) {
            case REPLAY -> replay(filesDirectory);
            case ONE_TRICK -> launch(oneTrick(), null, List.of(), false, persistFixture);
            case TRICK_PAUSE -> launch(trickPause(), null, List.of(), false, true);
            case PLAYED_CARD -> launch(playedCard(), null, List.of(), false, false);
            case HARD_AI_BID -> launch(hardAiBid(), null, List.of(), false, false);
            case HAND_FAN -> launch(display(13, true), null, List.of(), false, false);
            case NINE_BIDS -> launch(display(8, false), null, List.of(), false, false);
            case COMPACT_TABLE -> compactTable();
        };
    }

    private static Scenario requestedScenario(Intent intent) {
        if (intent.getBooleanExtra("debug_replay", false)) return Scenario.REPLAY;
        if (intent.getBooleanExtra("debug_fixture", false)) return Scenario.ONE_TRICK;
        if (intent.getBooleanExtra("debug_trick_pause_fixture", false)) return Scenario.TRICK_PAUSE;
        if (intent.getBooleanExtra("debug_played_card_fixture", false)) return Scenario.PLAYED_CARD;
        if (intent.getBooleanExtra("debug_hard_ai_bid_fixture", false)) return Scenario.HARD_AI_BID;
        if (intent.getBooleanExtra("debug_hand_fan_fixture", false)) return Scenario.HAND_FAN;
        if (intent.getBooleanExtra("debug_nine_bids_fixture", false)) return Scenario.NINE_BIDS;
        if (intent.getBooleanExtra("debug_compact_table_fixture", false)) return Scenario.COMPACT_TABLE;
        return null;
    }

    private static DebugLaunch replay(File filesDirectory) throws IOException {
        OriginalMatchReplay replay = new OriginalMatchReplay(filesDirectory);
        return launch(replay.startHand(0, ScoreHistory.newMatch(4)), replay, List.of(), false, true);
    }

    private static DebugLaunch compactTable() {
        DisplayFixtureDeals.CompletedTrick fixture = DisplayFixtureDeals.completedThirteenCardTrick();
        return launch(fixture.game(), null, fixture.cards(), true, false);
    }

    private static GameSession oneTrick() {
        GameConfig config = config(GameMode.REVERSE_OH_HELL, AiDifficulty.NORMAL, 20261101L);
        List<List<Card>> hands = Arrays.asList(
                Collections.singletonList(new Card(Suit.HEARTS, 14)),
                Collections.singletonList(new Card(Suit.HEARTS, 13)),
                Collections.singletonList(new Card(Suit.HEARTS, 12)),
                Collections.singletonList(new Card(Suit.HEARTS, 11)));
        return GameSession.fromDeal(config, hands, Trump.SPADES, 3);
    }

    private static GameSession trickPause() {
        return twoCardGame(GameMode.OH_HELL, AiDifficulty.NORMAL, 20261103L);
    }

    private static GameSession playedCard() {
        GameSession game = twoCardGame(GameMode.OH_HELL, AiDifficulty.NORMAL, 20261104L);
        finishBidding(game);
        if (game.snapshot().currentSeat() != 0) {
            throw new IllegalStateException("Played-card fixture must begin with the human lead");
        }
        game.playCard(0, new Card(Suit.HEARTS, 14));
        return game;
    }

    private static GameSession hardAiBid() {
        GameSession game = twoCardGame(GameMode.OH_HELL, AiDifficulty.HARD, 20261104L);
        new HumanBidSelection().select(game, 0);
        return game;
    }

    private static GameSession display(int cards, boolean playing) {
        GameConfig config = config(GameMode.DESCENDING, AiDifficulty.NORMAL, 20261103L);
        GameSession game = GameSession.fromDeal(
                config, DisplayFixtureDeals.fourPlayerHands(cards), Trump.SPADES, 3);
        if (playing) finishBidding(game);
        return game;
    }

    private static GameSession twoCardGame(GameMode mode, AiDifficulty difficulty, long seed) {
        GameConfig config = config(mode, difficulty, seed);
        List<List<Card>> hands = Arrays.asList(
                Arrays.asList(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 14)),
                Arrays.asList(new Card(Suit.HEARTS, 13), new Card(Suit.CLUBS, 13)),
                Arrays.asList(new Card(Suit.HEARTS, 12), new Card(Suit.CLUBS, 12)),
                Arrays.asList(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 11)));
        return GameSession.fromDeal(config, hands, Trump.SPADES, 3);
    }

    private static GameConfig config(GameMode mode, AiDifficulty difficulty, long seed) {
        return new GameConfig(4, mode, difficulty, seed);
    }

    private static void finishBidding(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            game.submitBid(game.snapshot().biddingSeat(), 0);
        }
    }

    private static DebugLaunch launch(
            GameSession game,
            DebugReplay replay,
            List<PlayedCard> renderedCards,
            boolean renderImmediately,
            boolean persistBeforeDrive) {
        return new DebugLaunch(game, replay, renderedCards, renderImmediately, persistBeforeDrive);
    }
}