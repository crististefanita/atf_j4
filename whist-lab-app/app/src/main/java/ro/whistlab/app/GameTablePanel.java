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
import android.widget.HorizontalScrollView;
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

/** Player-facing table, public trick and private hand, composed from a game snapshot. */
final class GameTablePanel extends LinearLayout {
    private static final int WHITE = Color.rgb(255, 254, 248);
    private static final int INK = Color.rgb(41, 54, 46);
    private static final int MUTED = Color.rgb(105, 117, 106);
    private static final int GOLD = Color.rgb(220, 181, 105);
    private static final int CARD_RED = Color.rgb(173, 57, 58);
    private static final int CARD_DARK = Color.rgb(42, 53, 47);
    private static final int SOFT_BORDER = Color.rgb(211, 220, 210);
    private static final int PLAY_HINT_SLOT_HEIGHT_DP = 32;

    private final Context context;
    private final GameSession.Snapshot state;
    private final WhistUiText copy;
    private final boolean bidding;
    private final Set<Card> playableCards;
    private final Consumer<Card> onCardSelected;
    private final List<Integer> bidOptions;
    private final List<Button> bidChoiceButtons = new ArrayList<>();
    private final IntConsumer onBidSelected;
    private final Runnable onBidSubmitted;
    private int selectedBid;

    static GameTablePanel bidding(Context context, GameSession.Snapshot state,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted, WhistUiText copy) {
        return new GameTablePanel(context, state, true, Collections.emptyList(), null,
                null, bidOptions, selectedBid, onBidSelected, onBidSubmitted, copy);
    }

    static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint, WhistUiText copy) {
        return new GameTablePanel(context, state, false, legalCards, onCardSelected, hint,
                Collections.emptyList(), 0, null, null, copy);
    }

    private GameTablePanel(Context context, GameSession.Snapshot state, boolean bidding,
            List<Card> playableCards, Consumer<Card> onCardSelected, String hint,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted,
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
        this.onBidSubmitted = onBidSubmitted;

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(8), dp(4), dp(8), dp(4));
        setBackgroundColor(Color.TRANSPARENT);
        setClipChildren(false);
        setContentDescription(copy.tableAccessibility(state.players().size()));

        View board = buildRoundTable();
        LayoutParams boardParams = new LayoutParams(dp(Math.min(availableContentWidthDp(), 760)),
                dp(roundTableHeightDp()));
        boardParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(board, boardParams);
        if (!bidding) {
            FrameLayout hintSlot = new FrameLayout(context);
            TextView help = text(hint == null ? "" : hint, 11, Color.rgb(222, 237, 225), false);
            help.setGravity(Gravity.CENTER);
            help.setMaxLines(2);
            help.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (hint == null || hint.isBlank()) help.setVisibility(View.INVISIBLE);
            else help.setContentDescription(hint);
            hintSlot.addView(help, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(PLAY_HINT_SLOT_HEIGHT_DP));
            params.topMargin = dp(4);
            addView(hintSlot, params);
        }
        LayoutParams handParams = new LayoutParams(dp(handWidthDp()), dp(HandLayoutMetrics.reservedHandHeightDp()));
        handParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(buildHand(), handParams);
    }

    private View buildRoundTable() {
        FrameLayout board = new FrameLayout(context);
        board.setBackground(tableSurface());
        board.setPadding(dp(4), dp(4), dp(4), dp(4));
        board.setContentDescription(copy.boardSurface());
        board.setTag("game-board");
        board.setClipChildren(false);

        int tableWidthDp = Math.min(availableContentWidthDp(), 760);
        TableSeatRingLayout seats = new TableSeatRingLayout(context, state.players().size(), bidding,
                dp(TableLayoutMetrics.seatWidthDp(tableWidthDp)), tableWidthDp);
        for (GameSession.PlayerSnapshot player : state.players()) {
            seats.addView(buildSeat(player));
        }
        board.addView(seats, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (bidding) {
            board.addView(buildBiddingCenter(), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER));
        } else {
            board.addView(buildCurrentTrick(), new FrameLayout.LayoutParams(
                    dp(TableLayoutMetrics.centerPlayWidthDp(
                            Math.min(availableContentWidthDp(), 760))),
                    dp(TableLayoutMetrics.playingCenterRowHeightDp(state.players().size(),
                            getResources().getConfiguration().fontScale)), Gravity.CENTER));
        }
        return board;
    }

    private View buildBiddingCenter() {
        LinearLayout center = new LinearLayout(context);
        center.setOrientation(VERTICAL);
        center.setGravity(Gravity.CENTER);
        int columns = BidGridMetrics.columnCount(bidOptions.size(), availableContentWidthDp());

        if (columns == BidGridMetrics.DENSE_COLUMNS) {
            center.setContentDescription(copy.bidSelector());
        } else {
            TextView heading = text(copy.bidChoiceHeading(), 11, WHITE, true);
            heading.setGravity(Gravity.CENTER);
            heading.setContentDescription(copy.bidSelector());
            center.addView(heading, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        for (int start = 0; start < bidOptions.size(); start += columns) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            int end = Math.min(start + columns, bidOptions.size());
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
                params.setMargins(dp(2), dp(1), dp(2), dp(1));
                row.addView(choice, params);
            }
            center.addView(row, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(BidGridMetrics.CHOICE_TARGET_DP)));
        }
        center.addView(buildBidSubmission(), new LayoutParams(dp(132), dp(BidGridMetrics.SUBMIT_TARGET_DP)));
        return center;
    }

    private View buildBidSubmission() {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER);

        Button submit = new Button(context);
        submit.setAllCaps(false);
        submit.setText(copy.placeBid());
        submit.setTextSize(14);
        submit.setTextColor(Color.rgb(245, 248, 240));
        submit.setContentDescription(copy.placeBid());
        submit.setTag("btn_submit_bid");
        submit.setMinimumWidth(0);
        submit.setMinHeight(dp(48));
        submit.setStateListAnimator(null);
        submit.setBackground(rounded(Color.TRANSPARENT, Color.rgb(194, 216, 199), 13));
        submit.setOnClickListener(ignored -> {
            if (onBidSubmitted != null) onBidSubmitted.run();
        });
        row.addView(submit, new LayoutParams(dp(132), dp(48)));
        return row;
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

    private View buildSeat(GameSession.PlayerSnapshot player) {
        int activeSeat = bidding ? state.biddingSeat() : state.currentSeat();
        boolean active = player.seat() == activeSeat;
        boolean dealer = player.seat() == state.dealerSeat();
        LinearLayout seat = new LinearLayout(context);
        seat.setOrientation(VERTICAL);
        seat.setGravity(Gravity.CENTER);
        seat.setPadding(dp(4), dp(3), dp(4), dp(3));
        seat.setBackground(rounded(active ? Color.rgb(241, 230, 200) : WhistUiPalette.TABLE_FELT,
                active ? GOLD : Color.rgb(84, 127, 98), 11));
        seat.setElevation(dp(active ? 3 : 1));

        FrameLayout actionSlot = new FrameLayout(context);
        FrameLayout.LayoutParams actionBadgeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        if (active) {
            TextView action = text(copy.seatTurnBadge(bidding), 9, INK, true);
            action.setGravity(Gravity.CENTER);
            action.setPadding(dp(6), dp(2), dp(6), dp(2));
            action.setBackground(rounded(GOLD, Color.rgb(105, 83, 45), 20));
            GradientDrawable turnDot = new GradientDrawable();
            turnDot.setShape(GradientDrawable.OVAL);
            turnDot.setColor(Color.rgb(105, 83, 45));
            turnDot.setBounds(0, 0, dp(5), dp(5));
            action.setCompoundDrawablesRelative(turnDot, null, null, null);
            action.setCompoundDrawablePadding(dp(3));
            action.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            actionSlot.addView(action, actionBadgeParams);
        }
        seat.addView(actionSlot, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));

        TextView name = text(copy.seatName(player), 13,
                active ? INK : Color.rgb(229, 239, 230), true);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seat.addView(name, matchWrap());
        TextView facts = text(copy.seatFacts(player), 11,
                active ? INK : Color.rgb(229, 239, 230), false);
        facts.setGravity(Gravity.CENTER);
        facts.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seat.addView(facts, matchWrap());
        seat.setTag("table-seat:" + player.seat());
        seat.setContentDescription(copy.seatAccessibility(player, dealer, active, bidding));
        seat.setFocusable(true);
        seat.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return seat;
    }

    private View buildCurrentTrick() {
        if (state.currentTrick().isEmpty()) {
            TextView empty = text("", 13, Color.TRANSPARENT, false);
            empty.setContentDescription(copy.noCardsYet());
            empty.setTag("current-trick");
            return empty;
        }

        LinearLayout cards = new LinearLayout(context);
        cards.setOrientation(VERTICAL);
        cards.setGravity(Gravity.CENTER);
        cards.setContentDescription(copy.currentTrick());
        cards.setTag("current-trick");
        List<PlayedCard> plays = state.currentTrick();
        for (int start = 0; start < plays.size(); start += 2) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            int end = Math.min(start + 2, plays.size());
            for (int index = start; index < end; index++) {
                PlayedCard play = plays.get(index);
                GameSession.PlayerSnapshot player = state.players().get(play.seat());
                LinearLayout item = new LinearLayout(context);
                item.setOrientation(VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(dp(2), dp(2), dp(2), dp(2));
                TextView card = text(play.card().shortLabel(), 14,
                        play.card().suit().red() ? CARD_RED : CARD_DARK, true);
                card.setGravity(Gravity.CENTER);
                card.setPadding(dp(3), dp(4), dp(3), dp(4));
                card.setBackground(rounded(WHITE, Color.rgb(220, 227, 238), 9));
                card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                item.addView(card, new LayoutParams(dp(36), dp(41)));
                TextView who = text(copy.trickPlayer(player), 10, Color.rgb(212, 223, 209), false);
                who.setGravity(Gravity.CENTER);
                who.setSingleLine(true);
                who.setEllipsize(android.text.TextUtils.TruncateAt.END);
                who.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                item.addView(who, matchWrap());
                item.setContentDescription(copy.trickCardAccessibility(player, play.card()));
                item.setTag("trick-card:" + play.seat());
                item.setFocusable(true);
                item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(dp(1), 0, dp(1), 0);
                row.addView(item, params);
            }
            cards.addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return cards;
    }

    private View buildHand() {
        HorizontalScrollView hand = new HorizontalScrollView(context);
        hand.setHorizontalScrollBarEnabled(false);
        hand.setFillViewport(false);
        hand.setClipToPadding(false);
        hand.setOverScrollMode(View.OVER_SCROLL_NEVER);
        int cardCount = state.players().get(0).hand().size();
        int availableWidthDp = handWidthDp();
        float fontScale = getResources().getConfiguration().fontScale;
        hand.setContentDescription(copy.handAccessibility(cardCount));
        hand.setTag("player-hand");
        FrameLayout fan = new FrameLayout(context);
        fan.setClipChildren(false);
        fan.setClipToPadding(false);
        int advanceDp = HandLayoutMetrics.horizontalAdvanceDp(cardCount, availableWidthDp, fontScale);
        int cardWidthDp = HandLayoutMetrics.cardWidthDp(cardCount, availableWidthDp, fontScale);
        int handContentWidthDp = HandLayoutMetrics.contentWidthDp(cardCount, availableWidthDp, fontScale);
        int fanWidthDp = Math.max(availableWidthDp, handContentWidthDp);
        int handLeadingInsetDp = Math.max(0, (fanWidthDp - handContentWidthDp) / 2);
        int cardHeightDp = HandLayoutMetrics.cardHeightDp();
        int liftClearanceDp = HandLayoutMetrics.LIFT_CLEARANCE_DP;
        fan.setTag("player-hand-fan");
        for (int index = 0; index < cardCount; index++) {
            Card card = state.players().get(0).hand().get(index);
            boolean playable = !bidding && onCardSelected != null && playableCards.contains(card);
            boolean humanTurn = state.phase() != ro.whistlab.core.GamePhase.PLAYING
                    || state.currentSeat() == 0;
            FrameLayout slot = new FrameLayout(context);
            slot.setClipChildren(false);
            slot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            slot.setFocusable(true);
            slot.setTag("hand-card:" + card.suit().name() + ":" + card.rank());
            slot.setContentDescription(copy.cardLabel(card) + ". "
                    + copy.cardState(bidding, humanTurn, playable));
            slot.setEnabled(true);
            slot.setClickable(playable);
            slot.setTranslationZ(dp(HandLayoutMetrics.NEIGHBOR_Z_DP));
            FrameLayout face = cardFace(card, playable);
            FrameLayout.LayoutParams faceParams = new FrameLayout.LayoutParams(dp(cardWidthDp),
                    dp(cardHeightDp), Gravity.TOP | Gravity.START);
            faceParams.topMargin = dp(liftClearanceDp);
            slot.addView(face, faceParams);
            if (playable) {
                slot.setOnClickListener(ignored -> {
                    slot.setClickable(false);
                    slot.setTranslationZ(dp(HandLayoutMetrics.SELECTED_Z_DP));
                    face.animate().translationY(-dp(HandLayoutMetrics.CARD_LIFT_DP))
                            .scaleX(HandLayoutMetrics.CARD_LIFT_SCALE)
                            .scaleY(HandLayoutMetrics.CARD_LIFT_SCALE)
                            .setDuration(150L)
                            .withEndAction(() -> onCardSelected.accept(card))
                            .start();
                });
            }
            FrameLayout.LayoutParams slotParams = new FrameLayout.LayoutParams(
                    dp(index == cardCount - 1 ? cardWidthDp : advanceDp),
                    dp(HandLayoutMetrics.reservedHandHeightDp()));
            slotParams.leftMargin = dp(handLeadingInsetDp + index * advanceDp);
            fan.addView(slot, slotParams);
        }
        hand.addView(fan, new FrameLayout.LayoutParams(dp(fanWidthDp),
                dp(HandLayoutMetrics.reservedHandHeightDp())));
        return hand;
    }

    private FrameLayout cardFace(Card card, boolean playable) {
        FrameLayout face = new FrameLayout(context);
        face.setBackground(rounded(WHITE, playable ? GOLD : SOFT_BORDER, 9));
        face.setFocusable(false);
        face.setClickable(false);

        TextView label = text(card.shortLabel(), HandLayoutMetrics.cardLabelSizeSp(),
                card.suit().red() ? CARD_RED : CARD_DARK, true);
        label.setGravity(Gravity.TOP | Gravity.START);
        label.setPadding(dp(4), dp(5), dp(2), 0);
        face.addView(label, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return face;
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

    private int handWidthDp() {
        return Math.min(availableContentWidthDp(), HandLayoutMetrics.maxViewportWidthDp());
    }

    private int availableContentWidthDp() {
        float density = getResources().getDisplayMetrics().density;
        return Math.max(1, Math.round(getResources().getDisplayMetrics().widthPixels / density) - 40);
    }

    private int roundTableHeightDp() {
        return bidding
                ? TableLayoutMetrics.biddingHeightDp(state.players().size(), bidOptions.size(),
                        availableContentWidthDp(),
                        getResources().getConfiguration().fontScale)
                : TableLayoutMetrics.playingHeightDp(state.players().size(),
                        getResources().getConfiguration().fontScale);
    }

    private GradientDrawable tableSurface() {
        GradientDrawable surface = new GradientDrawable();
        surface.setShape(GradientDrawable.OVAL);
        surface.setColor(WhistUiPalette.TABLE_FELT);
        surface.setStroke(dp(1), Color.rgb(66, 111, 82));
        return surface;
    }

}
