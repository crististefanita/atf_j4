package ro.whistlab.ios;

import com.google.j2objc.annotations.ObjectiveCName;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/** Narrow Objective-C surface for the native iOS view; the game engine stays shared. */
@ObjectiveCName("WhistGameBridge")
public final class WhistGameBridge {
    private static final int CHECKPOINT_VERSION = 1;
    private static GameSession session;

    private WhistGameBridge() { }

    @ObjectiveCName("startGameWithPlayerCount:mode:difficulty:humanName:streakBonuses:streakPoints:allowZeroGame:seed:")
    public static synchronized String startGame(
            int playerCount, String mode, String difficulty, String humanName,
            boolean streakBonuses, int streakPoints, boolean allowZeroGame, long seed) {
        try {
            GameConfig config = GameConfig.newMatch(
                    playerCount,
                    GameMode.valueOf(Objects.requireNonNull(mode, "mode")),
                    AiDifficulty.valueOf(Objects.requireNonNull(difficulty, "difficulty")),
                    seed,
                    streakBonuses,
                    streakPoints,
                    allowZeroGame);
            session = GameSession.newGame(config, humanName);
            advanceComputerTurns();
            return stateJson();
        } catch (RuntimeException error) {
            session = null;
            return errorJson(error);
        }
    }

    @ObjectiveCName("currentState")
    public static synchronized String currentState() {
        try {
            return session == null ? "" : stateJson();
        } catch (RuntimeException error) {
            return errorJson(error);
        }
    }

    @ObjectiveCName("submitBidWithBid:")
    public static synchronized String submitBid(int bid) {
        try {
            requireHumanTurn(GamePhase.BIDDING);
            session.submitBid(0, bid);
            advanceComputerTurns();
            return stateJson();
        } catch (RuntimeException error) {
            return errorJson(error);
        }
    }

    @ObjectiveCName("playCardWithSuitName:rank:")
    public static synchronized String playCard(String suitName, int rank) {
        try {
            requireHumanTurn(GamePhase.PLAYING);
            session.playCard(0, new Card(Suit.valueOf(suitName), rank));
            advanceComputerTurns();
            return stateJson();
        } catch (RuntimeException error) {
            return errorJson(error);
        }
    }

    @ObjectiveCName("startNextHand")
    public static synchronized String startNextHand() {
        try {
            if (session == null) throw new IllegalStateException("No game is active");
            session.startNextHand();
            advanceComputerTurns();
            return stateJson();
        } catch (RuntimeException error) {
            return errorJson(error);
        }
    }

    @ObjectiveCName("saveCheckpoint")
    public static synchronized String saveCheckpoint() {
        if (session == null) return "";
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            writeSaveState(out, session.saveState());
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException | RuntimeException error) {
            return "";
        }
    }

    @ObjectiveCName("restoreGameWithCheckpoint:")
    public static synchronized String restoreGame(String checkpoint) {
        try {
            byte[] bytes = Base64.getDecoder().decode(checkpoint);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            session = GameSession.restoreComplete(readSaveState(in));
            if (in.available() != 0) throw new IOException("Unexpected checkpoint data");
            return stateJson();
        } catch (IOException | RuntimeException invalidCheckpoint) {
            session = null;
            return errorJson(new IllegalArgumentException("Saved game is invalid or unsupported"));
        }
    }

    private static String errorJson(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        return "{\"error\":" + quoted(message) + "}";
    }

    private static void requireHumanTurn(GamePhase phase) {
        if (session == null) throw new IllegalStateException("No game is active");
        GameSession.Snapshot state = session.snapshot();
        if (state.phase() != phase) throw new IllegalStateException("The game is not awaiting this action");
        int actingSeat = phase == GamePhase.BIDDING ? state.biddingSeat() : state.currentSeat();
        if (actingSeat != 0) throw new IllegalStateException("It is not the human player's turn");
    }

    private static void advanceComputerTurns() {
        for (int steps = 0; steps < 256; steps++) {
            GameSession.Snapshot state = session.snapshot();
            if (state.phase() == GamePhase.BIDDING) {
                if (state.biddingSeat() == 0) return;
                int seat = state.biddingSeat();
                session.submitBid(seat, AiPolicy.chooseBid(state, seat, session.config().aiDifficulty()));
            } else if (state.phase() == GamePhase.PLAYING) {
                if (state.currentSeat() == 0) return;
                int seat = state.currentSeat();
                session.playCard(seat, AiPolicy.chooseCard(state, seat, session.config().aiDifficulty()));
            } else {
                return;
            }
        }
        throw new IllegalStateException("Computer turns exceeded the expected hand limit");
    }

    private static String stateJson() {
        GameSession.Snapshot state = session.snapshot();
        StringBuilder json = new StringBuilder(1024);
        json.append('{')
                .append("\"phase\":").append(quoted(state.phase().name())).append(',')
                .append("\"mode\":").append(quoted(state.mode().name())).append(',')
                .append("\"handNumber\":").append(state.handNumber()).append(',')
                .append("\"handSize\":").append(state.handSize()).append(',')
                .append("\"trump\":").append(quoted(state.trump().name())).append(',')
                .append("\"dealerSeat\":").append(state.dealerSeat()).append(',')
                .append("\"biddingSeat\":").append(state.biddingSeat()).append(',')
                .append("\"currentSeat\":").append(state.currentSeat()).append(',')
                .append("\"bidsSubmitted\":").append(state.bidsSubmitted()).append(',')
                .append("\"bidTotal\":").append(state.bidTotal()).append(',')
                .append("\"recommendedBid\":");
        if (state.phase() == GamePhase.BIDDING && state.biddingSeat() == 0) {
            json.append(AiPolicy.chooseBid(state, 0, session.config().aiDifficulty()));
        } else {
            json.append("null");
        }
        json.append(',')
                .append("\"lastTrickWinnerSeat\":").append(state.lastTrickWinnerSeat()).append(',')
                .append("\"finalHand\":").append(state.finalHand()).append(',')
                .append("\"allowZeroGame\":").append(state.allowZeroGame()).append(',')
                .append("\"players\":[");
        for (int index = 0; index < state.players().size(); index++) {
            if (index > 0) json.append(',');
            GameSession.PlayerSnapshot player = state.players().get(index);
            json.append('{')
                    .append("\"seat\":").append(player.seat()).append(',')
                    .append("\"name\":").append(quoted(player.name())).append(',')
                    .append("\"human\":").append(player.human()).append(',')
                    .append("\"handSize\":").append(player.hand().size()).append(',')
                    .append("\"bid\":").append(player.bid() == null ? "null" : player.bid()).append(',')
                    .append("\"tricksWon\":").append(player.tricksWon()).append(',')
                    .append("\"totalScore\":").append(player.totalScore()).append(',')
                    .append("\"streak\":").append(player.streak()).append(',')
                    .append("\"streakPoints\":").append(player.streakPoints())
                    .append('}');
        }
        json.append("],\"hand\":");
        appendCards(json, state.players().get(0).hand());
        json.append(",\"currentTrick\":");
        appendPlayedCards(json, state.currentTrick());
        json.append(",\"lastTrick\":");
        appendPlayedCards(json, state.lastTrick());
        json.append(",\"allowedBids\":[");
        List<Integer> bids = GameRules.allowedBids(
                state.handSize(), state.bidTotal(), state.bidsSubmitted() == state.players().size() - 1);
        for (int index = 0; index < bids.size(); index++) {
            if (index > 0) json.append(',');
            json.append(bids.get(index));
        }
        json.append("],\"legalCards\":");
        if (state.phase() == GamePhase.PLAYING && state.currentSeat() == 0) {
            var trick = state.currentTrick();
            var ledSuit = trick.isEmpty() ? null : trick.get(0).card().suit();
            appendCards(json, GameRules.legalCards(state.players().get(0).hand(), ledSuit,
                    state.trump(), state.mode()));
        } else {
            json.append("[]");
        }
        json.append(",\"history\":");
        appendPlayedCards(json, state.history());
        json.append(",\"scoreHistory\":[");
        ScoreHistory scores = session.scoreHistory();
        for (int hand = scores.priorHands() + 1; hand <= scores.completedHands(); hand++) {
            if (hand > scores.priorHands() + 1) json.append(',');
            json.append("{\"hand\":").append(hand).append(",\"players\":[");
            for (int seat = 0; seat < state.players().size(); seat++) {
                if (seat > 0) json.append(',');
                Integer bid = scores.bid(hand, seat);
                Integer tricks = scores.tricks(hand, seat);
                json.append("{\"seat\":").append(seat)
                        .append(",\"bid\":").append(bid == null ? "null" : bid)
                        .append(",\"tricks\":").append(tricks == null ? "null" : tricks)
                        .append(",\"points\":").append(scores.points(hand, seat))
                        .append(",\"total\":").append(scores.total(hand, seat)).append('}');
            }
            json.append("]}");
        }
        json.append(']');
        json.append('}');
        return json.toString();
    }

    private static void appendCards(StringBuilder json, List<Card> cards) {
        json.append('[');
        for (int index = 0; index < cards.size(); index++) {
            if (index > 0) json.append(',');
            appendCard(json, cards.get(index));
        }
        json.append(']');
    }

    private static void appendCard(StringBuilder json, Card card) {
        json.append("{\"suit\":").append(quoted(card.suit().name()))
                .append(",\"rank\":").append(card.rank())
                .append(",\"label\":").append(quoted(card.shortLabel())).append('}');
    }

    private static void appendPlayedCards(StringBuilder json, List<PlayedCard> cards) {
        json.append('[');
        for (int index = 0; index < cards.size(); index++) {
            if (index > 0) json.append(',');
            PlayedCard played = cards.get(index);
            json.append("{\"seat\":").append(played.seat()).append(",\"card\":");
            appendCard(json, played.card());
            json.append('}');
        }
        json.append(']');
    }

    private static String quoted(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '"' || current == '\\') out.append('\\');
            if (current < 0x20) out.append(String.format("\\u%04x", (int) current));
            else out.append(current);
        }
        return out.append('"').toString();
    }

    private static void writeSaveState(DataOutputStream out, GameSession.SaveState save) throws IOException {
        out.writeInt(CHECKPOINT_VERSION);
        GameConfig config = save.config();
        out.writeInt(config.playerCount());
        out.writeUTF(config.mode().name());
        out.writeUTF(config.aiDifficulty().name());
        out.writeLong(config.seed());
        out.writeBoolean(config.streakBonuses());
        out.writeInt(config.streakBonusPoints());
        out.writeInt(config.streakPenaltyPoints());
        out.writeBoolean(config.allowZeroGame());
        out.writeUTF(save.phase().name());
        out.writeInt(save.handIndex());
        out.writeInt(save.handSize());
        out.writeInt(save.dealerSeat());
        out.writeInt(save.biddingSeat());
        out.writeInt(save.currentSeat());
        out.writeInt(save.bidsSubmitted());
        out.writeInt(save.bidTotal());
        out.writeUTF(save.trump().name());
        out.writeInt(save.lastTrickWinnerSeat());
        out.writeInt(save.players().size());
        for (GameSession.PlayerState player : save.players()) {
            out.writeInt(player.seat());
            out.writeUTF(player.name());
            out.writeBoolean(player.human());
            writeCards(out, player.hand());
            writeNullableInt(out, player.bid());
            out.writeInt(player.tricksWon());
            out.writeInt(player.totalScore());
            out.writeInt(player.streak());
            out.writeInt(player.streakPoints());
        }
        writePlayedCards(out, save.currentTrick());
        writePlayedCards(out, save.lastTrick());
        writePlayedCards(out, save.history());
        writeNullableCard(out, save.turnedCard());
        ScoreHistory scores = save.scoreHistory();
        out.writeInt(scores.priorHands());
        writeInts(out, scores.openingTotals());
        writeMatrix(out, scores.closingTotals());
        writeMatrix(out, scores.recordedBids());
        writeMatrix(out, scores.recordedTricks());
    }

    private static GameSession.SaveState readSaveState(DataInputStream in) throws IOException {
        if (in.readInt() != CHECKPOINT_VERSION) throw new IOException("Unknown checkpoint version");
        GameConfig config = new GameConfig(in.readInt(), GameMode.valueOf(in.readUTF()),
                AiDifficulty.valueOf(in.readUTF()), in.readLong(), in.readBoolean(),
                in.readInt(), in.readInt(), in.readBoolean());
        GamePhase phase = GamePhase.valueOf(in.readUTF());
        int handIndex = in.readInt();
        int handSize = in.readInt();
        int dealerSeat = in.readInt();
        int biddingSeat = in.readInt();
        int currentSeat = in.readInt();
        int bidsSubmitted = in.readInt();
        int bidTotal = in.readInt();
        var trump = ro.whistlab.core.model.Trump.valueOf(in.readUTF());
        int lastWinner = in.readInt();
        int playerCount = boundedCount(in.readInt(), 3, 6);
        List<GameSession.PlayerState> players = new ArrayList<>(playerCount);
        for (int index = 0; index < playerCount; index++) {
            players.add(new GameSession.PlayerState(in.readInt(), in.readUTF(), in.readBoolean(),
                    readCards(in), readNullableInt(in), in.readInt(), in.readInt(), in.readInt(), in.readInt()));
        }
        List<PlayedCard> currentTrick = readPlayedCards(in);
        List<PlayedCard> lastTrick = readPlayedCards(in);
        List<PlayedCard> history = readPlayedCards(in);
        Card turnedCard = readNullableCard(in);
        int priorHands = boundedCount(in.readInt(), 0, 10000);
        List<Integer> opening = readInts(in, 6);
        List<List<Integer>> closing = readMatrix(in, 1000, 6);
        List<List<Integer>> bids = readMatrix(in, 1000, 6);
        List<List<Integer>> tricks = readMatrix(in, 1000, 6);
        ScoreHistory scoreHistory = new ScoreHistory(priorHands, opening, closing, bids, tricks);
        return new GameSession.SaveState(config, phase, handIndex, handSize, dealerSeat,
                biddingSeat, currentSeat, bidsSubmitted, bidTotal, trump, lastWinner, players,
                currentTrick, lastTrick, history, turnedCard, scoreHistory);
    }

    private static void writeCards(DataOutputStream out, List<Card> cards) throws IOException {
        out.writeInt(cards.size());
        for (Card card : cards) {
            out.writeUTF(card.suit().name());
            out.writeInt(card.rank());
        }
    }

    private static List<Card> readCards(DataInputStream in) throws IOException {
        int count = boundedCount(in.readInt(), 0, 13);
        List<Card> cards = new ArrayList<>(count);
        for (int index = 0; index < count; index++) cards.add(new Card(Suit.valueOf(in.readUTF()), in.readInt()));
        return cards;
    }

    private static void writePlayedCards(DataOutputStream out, List<PlayedCard> cards) throws IOException {
        out.writeInt(cards.size());
        for (PlayedCard card : cards) {
            out.writeInt(card.seat());
            out.writeUTF(card.card().suit().name());
            out.writeInt(card.card().rank());
        }
    }

    private static List<PlayedCard> readPlayedCards(DataInputStream in) throws IOException {
        int count = boundedCount(in.readInt(), 0, 52);
        List<PlayedCard> cards = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            cards.add(new PlayedCard(in.readInt(), new Card(Suit.valueOf(in.readUTF()), in.readInt())));
        }
        return cards;
    }

    private static void writeNullableCard(DataOutputStream out, Card card) throws IOException {
        out.writeBoolean(card != null);
        if (card != null) {
            out.writeUTF(card.suit().name());
            out.writeInt(card.rank());
        }
    }

    private static Card readNullableCard(DataInputStream in) throws IOException {
        return in.readBoolean() ? new Card(Suit.valueOf(in.readUTF()), in.readInt()) : null;
    }

    private static void writeNullableInt(DataOutputStream out, Integer value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) out.writeInt(value);
    }

    private static Integer readNullableInt(DataInputStream in) throws IOException {
        return in.readBoolean() ? in.readInt() : null;
    }

    private static void writeInts(DataOutputStream out, List<Integer> values) throws IOException {
        out.writeInt(values.size());
        for (int value : values) out.writeInt(value);
    }

    private static List<Integer> readInts(DataInputStream in, int maximum) throws IOException {
        int count = boundedCount(in.readInt(), 1, maximum);
        List<Integer> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) values.add(in.readInt());
        return values;
    }

    private static void writeMatrix(DataOutputStream out, List<List<Integer>> rows) throws IOException {
        out.writeInt(rows.size());
        for (List<Integer> row : rows) writeInts(out, row);
    }

    private static List<List<Integer>> readMatrix(DataInputStream in, int maximumRows, int maximumColumns)
            throws IOException {
        int rows = boundedCount(in.readInt(), 0, maximumRows);
        List<List<Integer>> values = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) values.add(readInts(in, maximumColumns));
        return values;
    }

    private static int boundedCount(int value, int minimum, int maximum) throws IOException {
        if (value < minimum || value > maximum) throw new IOException("Checkpoint count is out of bounds");
        return value;
    }
}
