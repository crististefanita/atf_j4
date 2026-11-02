package ro.whistlab.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import ro.whistlab.core.Card;
import ro.whistlab.core.GameSession;
import ro.whistlab.core.PlayedCard;
import ro.whistlab.core.Suit;

/** Player-facing table, public trick and private hand, composed from a game snapshot. */
final class GameTablePanel extends LinearLayout {
    private static final int FELT_TOP = Color.rgb(39, 111, 82);
    private static final int FELT_BOTTOM = Color.rgb(23, 79, 60);
    private static final int FELT_EDGE = Color.rgb(112, 157, 125);
    private static final int WHITE = Color.rgb(255, 254, 248);
    private static final int INK = Color.rgb(41, 54, 46);
    private static final int MUTED = Color.rgb(105, 117, 106);
    private static final int GOLD = Color.rgb(220, 181, 105);
    private static final int CARD_RED = Color.rgb(173, 57, 58);
    private static final int CARD_DARK = Color.rgb(42, 53, 47);
    private static final int SOFT_BORDER = Color.rgb(211, 220, 210);

    private final Context context;
    private final GameSession.Snapshot state;
    private final WhistUiText copy;
    private final boolean bidding;
    private final Set<Card> playableCards;
    private final Consumer<Card> onCardSelected;
    private final List<Integer> bidOptions;
    private final List<Button> bidChoiceButtons = new ArrayList<>();
    private final IntConsumer onBidSelected;
    private int selectedBid;

    static GameTablePanel bidding(Context context, GameSession.Snapshot state,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            WhistUiText copy) {
        return new GameTablePanel(context, state, true, Collections.emptyList(), null,
                null, bidOptions, selectedBid, onBidSelected, copy);
    }

    static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint, WhistUiText copy) {
        return new GameTablePanel(context, state, false, legalCards, onCardSelected, hint,
                Collections.emptyList(), 0, null, copy);
    }

    private GameTablePanel(Context context, GameSession.Snapshot state, boolean bidding,
            List<Card> playableCards, Consumer<Card> onCardSelected, String hint,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            WhistUiText copy) {
        super(context);
        this.context = context;
        this.state = state;
        this.copy = copy;
        this.bidding = bidding;
        this.playableCards = new HashSet<>(playableCards);
        this.onCardSelected = onCardSelected;
        this.bidOptions = List.copyOf(bidOptions);
        this.selectedBid = selectedBid;
        this.onBidSelected = onBidSelected;

        setOrientation(VERTICAL);
        setPadding(dp(10), dp(8), dp(10), dp(8));
        setBackground(rounded(WHITE, SOFT_BORDER, 16));
        setElevation(dp(2));
        setContentDescription(copy.tableAccessibility(state.players().size()));

        int activeSeat = bidding ? state.biddingSeat() : state.currentSeat();
        if (!bidding) {
            String turnText = activeSeat == 0 ? copy.yourTurn()
                    : copy.waitingFor(copy.playerName(state.players().get(activeSeat)));
            TextView turn = text(turnText, 15, INK, true);
            turn.setContentDescription(turnText);
            addView(turn, matchWrap());
        }

        addView(buildRoundTable(), new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(roundTableHeightDp())));
        if (!bidding) {
            addSectionTitle(copy.currentTrick(), copy.cardsPlayed(
                    state.currentTrick().size(), state.players().size()));
            addView(buildCurrentTrick(), matchWrap());
        }
        if (!bidding && hint != null && !hint.isBlank()) {
            TextView help = text(hint, 13, INK, false);
            help.setPadding(dp(10), dp(9), dp(10), dp(9));
            help.setBackground(rounded(Color.rgb(235, 241, 233),
                    Color.rgb(205, 219, 204), 10));
            help.setContentDescription(hint);
            LayoutParams params = matchWrap();
            params.topMargin = dp(10);
            addView(help, params);
        }
        addView(buildHand(), matchWrap());
    }

    private View buildRoundTable() {
        FrameLayout board = new FrameLayout(context);
        GradientDrawable felt = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{FELT_TOP, FELT_BOTTOM});
        felt.setCornerRadius(dp(24));
        felt.setStroke(dp(1), FELT_EDGE);
        board.setBackground(felt);
        board.setPadding(dp(5), dp(5), dp(5), dp(5));
        board.setContentDescription(copy.boardSurface());

        GridLayout seats = new GridLayout(context);
        seats.setRowCount(3);
        seats.setColumnCount(3);
        seats.setUseDefaultMargins(false);
        seats.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        int[][] positions = seatPositions(state.players().size());
        for (int cellIndex = 0; cellIndex < 9; cellIndex++) {
            int row = cellIndex / 3;
            int column = cellIndex % 3;
            FrameLayout cell = new FrameLayout(context);
            cell.setMinimumHeight(dp(bidding ? (row == 1 ? 50 : 60) : 70));
            float rowWeight = bidding && row == 1 ? 2f : 1f;
            GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams(
                    GridLayout.spec(row, 1, GridLayout.FILL, rowWeight),
                    GridLayout.spec(column, 1, GridLayout.FILL, 1f));
            cellParams.width = 0;
            cellParams.height = 0;
            cellParams.setMargins(dp(2), dp(2), dp(2), dp(2));
            seats.addView(cell, cellParams);

            if (!bidding && row == 1 && column == 1) {
                cell.addView(buildTrumpMarker(), new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER));
            }
            for (int seat = 0; seat < positions.length; seat++) {
                if (positions[seat][0] == row && positions[seat][1] == column) {
                    cell.addView(buildSeat(state.players().get(seat)), new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER));
                    break;
                }
            }
        }
        board.addView(seats, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (bidding) {
            board.addView(buildBiddingCenter(), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER));
        }
        return board;
    }

    private View buildBiddingCenter() {
        LinearLayout center = new LinearLayout(context);
        center.setOrientation(VERTICAL);
        center.setGravity(Gravity.CENTER);

        TextView heading = text(copy.bidChoiceHeading() + "  ·  " + copy.compactTrump(state),
                11, WHITE, true);
        heading.setGravity(Gravity.CENTER);
        heading.setContentDescription(copy.bidSelector() + ". " + copy.trumpAccessibility(state));
        center.addView(heading, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int start = 0; start < bidOptions.size(); start += BidGridMetrics.CHOICES_PER_ROW) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            int end = Math.min(start + BidGridMetrics.CHOICES_PER_ROW, bidOptions.size());
            for (int index = start; index < end; index++) {
                int bid = bidOptions.get(index);
                Button choice = new Button(context);
                choice.setAllCaps(false);
                choice.setText(Integer.toString(bid));
                choice.setTextSize(13);
                choice.setPadding(0, 0, 0, 0);
                choice.setMinimumWidth(0);
                choice.setMinHeight(dp(48));
                choice.setStateListAnimator(null);
                choice.setTag("bid-choice:" + bid);
                choice.setSelected(bid == selectedBid);
                choice.setFocusable(true);
                styleBidChoice(choice, bid == selectedBid);
                choice.setOnClickListener(ignored -> selectBid(bid));
                bidChoiceButtons.add(choice);

                LayoutParams params = new LayoutParams(dp(48), dp(48));
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(choice, params);
            }
            center.addView(row, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        }
        return center;
    }

    private void selectBid(int bid) {
        selectedBid = bid;
        for (Button choice : bidChoiceButtons) {
            boolean selected = Integer.parseInt(String.valueOf(choice.getTag()).substring("bid-choice:".length())) == bid;
            choice.setSelected(selected);
            styleBidChoice(choice, selected);
        }
        if (onBidSelected != null) onBidSelected.accept(bid);
    }

    private void styleBidChoice(Button choice, boolean selected) {
        choice.setTextColor(INK);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(selected ? GOLD : Color.rgb(232, 239, 231));
        shape.setCornerRadius(dp(9));
        shape.setStroke(dp(selected ? 2 : 1), selected ? Color.rgb(105, 83, 45)
                : Color.rgb(190, 207, 191));
        choice.setBackground(new InsetDrawable(shape, dp(7), dp(7), dp(7), dp(7)));
        choice.setElevation(dp(selected ? 1 : 0));
        int bid = Integer.parseInt(String.valueOf(choice.getTag()).substring("bid-choice:".length()));
        choice.setContentDescription(copy.bidChoiceDescription(bid, selected));
    }

    private View buildTrumpMarker() {
        LinearLayout marker = new LinearLayout(context);
        marker.setOrientation(VERTICAL);
        marker.setGravity(Gravity.CENTER);
        marker.setContentDescription(copy.trumpAccessibility(state));
        marker.setFocusable(true);
        marker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        TextView title = text(copy.trumpHeading(), 9, Color.rgb(211, 239, 226), true);
        title.setGravity(Gravity.CENTER);
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        marker.addView(title, matchWrap());

        if (state.turnedCard() != null) {
            Card turnedCard = state.turnedCard();
            TextView card = text(turnedCard.shortLabel(), 24,
                    turnedCard.suit().red() ? Color.rgb(255, 177, 177) : WHITE, true);
            card.setGravity(Gravity.CENTER);
            card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            marker.addView(card, matchWrap());
        } else if (state.trump().suit().isPresent()) {
            Suit suit = state.trump().suit().get();
            TextView symbol = text(suit.symbol(), 30, suit.red() ? Color.rgb(255, 177, 177) : WHITE, true);
            symbol.setGravity(Gravity.CENTER);
            symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            marker.addView(symbol, matchWrap());
        } else {
            TextView noTrump = text("—", 30, WHITE, true);
            noTrump.setGravity(Gravity.CENTER);
            noTrump.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            marker.addView(noTrump, matchWrap());
        }
        return marker;
    }

    private View buildSeat(GameSession.PlayerSnapshot player) {
        int activeSeat = bidding ? state.biddingSeat() : state.currentSeat();
        boolean active = player.seat() == activeSeat;
        boolean dealer = player.seat() == state.dealerSeat();
        LinearLayout seat = new LinearLayout(context);
        seat.setOrientation(VERTICAL);
        seat.setGravity(Gravity.CENTER);
        seat.setPadding(dp(4), dp(3), dp(4), dp(3));
        seat.setBackground(rounded(active ? GOLD : Color.rgb(38, 101, 74),
                active ? WHITE : Color.rgb(111, 157, 126), 11));
        seat.setElevation(dp(active ? 3 : 1));

        String markers = dealer ? copy.dealerLabel() : "";
        if (!markers.isEmpty()) {
            TextView marker = text(markers, 8, active ? INK : Color.rgb(229, 239, 230), true);
            marker.setGravity(Gravity.CENTER);
            marker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            seat.addView(marker, matchWrap());
        }

        TextView facts = text(copy.seatFacts(player), 12,
                active ? INK : Color.rgb(229, 239, 230), false);
        facts.setGravity(Gravity.CENTER);
        facts.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seat.addView(facts, matchWrap());
        TextView result = text(copy.seatResult(player), 12,
                active ? INK : Color.rgb(229, 239, 230), false);
        result.setGravity(Gravity.CENTER);
        result.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seat.addView(result, matchWrap());
        seat.setTag("table-seat:" + player.seat());
        seat.setContentDescription(copy.seatAccessibility(player, dealer, active));
        seat.setFocusable(true);
        seat.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return seat;
    }

    private View buildCurrentTrick() {
        if (state.currentTrick().isEmpty()) {
            TextView empty = text(bidding ? copy.noCardsYet() : copy.firstLead(), 13, MUTED, false);
            empty.setPadding(dp(8), dp(8), dp(8), dp(8));
            empty.setContentDescription(bidding ? copy.noCardsYet() : copy.firstLead());
            return empty;
        }

        GridLayout cards = new GridLayout(context);
        cards.setColumnCount(3);
        cards.setUseDefaultMargins(false);
        for (PlayedCard play : state.currentTrick()) {
            GameSession.PlayerSnapshot player = state.players().get(play.seat());
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(3), dp(2), dp(3));
            TextView card = text(play.card().shortLabel(), 18,
                    play.card().suit().red() ? CARD_RED : CARD_DARK, true);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(5), dp(7), dp(5), dp(7));
            card.setBackground(rounded(WHITE, Color.rgb(220, 227, 238), 9));
            card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.addView(card, new LayoutParams(dp(58), dp(48)));
            TextView who = text(copy.trickPlayer(player), 10, MUTED, false);
            who.setGravity(Gravity.CENTER);
            who.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.addView(who, matchWrap());
            item.setContentDescription(copy.trickCardAccessibility(player, play.card()));
            item.setTag("trick-card:" + play.seat());
            item.setFocusable(true);
            item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1f));
            params.width = 0;
            params.setMargins(dp(2), 0, dp(2), 0);
            cards.addView(item, params);
        }
        cards.setContentDescription(copy.currentTrick());
        return cards;
    }

    private View buildHand() {
        GridLayout hand = new GridLayout(context);
        int cardCount = state.players().get(0).hand().size();
        boolean compactSingleRow = HandLayoutMetrics.usesSingleRow(cardCount);
        hand.setColumnCount(HandLayoutMetrics.columnCount(cardCount));
        hand.setUseDefaultMargins(false);
        hand.setContentDescription(copy.handAccessibility(cardCount));
        for (Card card : state.players().get(0).hand()) {
            boolean playable = !bidding && onCardSelected != null && playableCards.contains(card);
            boolean humanTurn = state.phase() != ro.whistlab.core.GamePhase.PLAYING
                    || state.currentSeat() == 0;
            FrameLayout face = cardFace(card, playable, compactSingleRow);
            face.setEnabled(true);
            face.setAlpha(bidding || playable ? 1f : 0.55f);
            face.setContentDescription(copy.cardLabel(card) + ". "
                    + copy.cardState(bidding, humanTurn, playable));
            face.setTag("hand-card:" + card.suit().name() + ":" + card.rank());
            face.setFocusable(true);
            face.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            if (playable) {
                face.setOnClickListener(ignored -> onCardSelected.accept(card));
            }
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1,
                            compactSingleRow ? GridLayout.FILL : GridLayout.CENTER,
                            compactSingleRow ? 1f : 0f));
            params.width = compactSingleRow ? 0 : dp(66);
            params.height = dp(HandLayoutMetrics.cardHeightDp(cardCount));
            int horizontalMargin = HandLayoutMetrics.horizontalMarginDp(cardCount);
            params.setMargins(dp(horizontalMargin), dp(2), dp(horizontalMargin), dp(2));
            hand.addView(face, params);
        }
        return hand;
    }

    private FrameLayout cardFace(Card card, boolean playable, boolean compact) {
        FrameLayout face = new FrameLayout(context);
        face.setBackground(rounded(WHITE, playable ? GOLD : SOFT_BORDER, playable ? 11 : 9));
        face.setElevation(dp(playable ? 3 : 1));
        face.setFocusable(true);
        face.setClickable(playable);

        TextView label = text(card.shortLabel(), HandLayoutMetrics.cardLabelSizeSp(compact),
                card.suit().red() ? CARD_RED : CARD_DARK, true);
        label.setGravity(Gravity.CENTER);
        face.addView(label, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (playable) {
            TextView mark = text("✓", 11, INK, true);
            mark.setGravity(Gravity.CENTER);
            mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            mark.setBackground(rounded(GOLD, WHITE, 8));
            face.addView(mark, new FrameLayout.LayoutParams(dp(18), dp(18),
                    Gravity.TOP | Gravity.END));
        }
        return face;
    }

    private void addSectionTitle(String title, String detail) {
        TextView heading = text(title, 15, INK, true);
        LayoutParams headingParams = matchWrap();
        headingParams.topMargin = dp(8);
        addView(heading, headingParams);
        if (detail == null || detail.isBlank()) return;
        TextView caption = text(detail, 12, MUTED, false);
        LayoutParams captionParams = matchWrap();
        captionParams.topMargin = dp(2);
        captionParams.bottomMargin = dp(2);
        addView(caption, captionParams);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private LayoutParams matchWrap() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int roundTableHeightDp() {
        float fontScale = getResources().getConfiguration().fontScale;
        int accessibilityAllowance = Math.round(Math.max(0f, fontScale - 1f) * 90f);
        return 200 + (state.players().size() - 3) * 18
                + (bidding ? 50 : 0)
                + (bidding ? BidGridMetrics.additionalTableHeightDp(bidOptions.size()) : 0)
                + accessibilityAllowance;
    }

    private static int[][] seatPositions(int count) {
        return switch (count) {
            case 3 -> new int[][]{{2, 1}, {0, 0}, {0, 2}};
            case 4 -> new int[][]{{2, 1}, {0, 0}, {0, 2}, {2, 0}};
            case 5 -> new int[][]{{2, 1}, {2, 0}, {0, 0}, {0, 1}, {0, 2}};
            case 6 -> new int[][]{{2, 1}, {2, 0}, {0, 0}, {0, 1}, {0, 2}, {2, 2}};
            default -> throw new IllegalArgumentException("Whist tables support 3–6 players: " + count);
        };
    }
}
