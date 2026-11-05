package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.cards.CardFaceBackground;
import ro.whistlab.app.hand.CenteredHandView;
import ro.whistlab.app.hand.HandLayoutMetrics;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.theme.WhistUiPalette;
import ro.whistlab.app.window.WindowContent;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import ro.whistlab.core.model.Card;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.PlayedCard;

/** Player-facing table, public trick and private hand, composed from a game snapshot. */
public final class GameTablePanel extends LinearLayout {
    private static final int WHITE = Color.rgb(255, 254, 248);
    private static final int INK = WhistUiPalette.ACTIVE_SEAT_TEXT;
    private static final int MUTED = Color.rgb(105, 117, 106);
    private static final int GOLD = Color.rgb(220, 181, 105);
    private static final int CARD_RED = Color.rgb(173, 57, 58);
    private static final int CARD_DARK = Color.rgb(42, 53, 47);
    private static final int SOFT_BORDER = Color.rgb(211, 220, 210);
    private static final int PLAY_HINT_SLOT_HEIGHT_DP = 32;

    private final Context context;
    private final TableViewportPolicy viewport;
    private final GameSession.Snapshot state;
    private final WhistUiText copy;
    private final boolean bidding;
    private final boolean interactionPaused;
    private final List<PlayedCard> completedTrickToDisplay;
    private final Set<Card> playableCards;
    private final Consumer<Card> onCardSelected;
    private final List<Integer> bidOptions;
    private final List<Button> bidChoiceButtons = new ArrayList<>();
    private final IntConsumer onBidSelected;
    private final Runnable onBidSubmitted;
    private int selectedBid;

    public static GameTablePanel bidding(Context context, GameSession.Snapshot state,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted, WhistUiText copy) {
        return new GameTablePanel(context, state, true, Collections.emptyList(), null,
                null, bidOptions, selectedBid, onBidSelected, onBidSubmitted, copy,
                Collections.emptyList(), false);
    }

    public static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint, WhistUiText copy) {
        return playing(context, state, legalCards, onCardSelected, hint,
                Collections.emptyList(), copy);
    }

    public static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint,
            List<PlayedCard> completedTrickToDisplay, WhistUiText copy) {
        return new GameTablePanel(context, state, false, legalCards, onCardSelected, hint,
                Collections.emptyList(), 0, null, null, copy, completedTrickToDisplay, false);
    }

    public static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint,
            List<PlayedCard> completedTrickToDisplay, boolean interactionPaused, WhistUiText copy) {
        return new GameTablePanel(context, state, false, legalCards, onCardSelected, hint,
                Collections.emptyList(), 0, null, null, copy, completedTrickToDisplay, interactionPaused);
    }

    private GameTablePanel(Context context, GameSession.Snapshot state, boolean bidding,
            List<Card> playableCards, Consumer<Card> onCardSelected, String hint,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted,
            WhistUiText copy, List<PlayedCard> completedTrickToDisplay, boolean interactionPaused) {
        super(context);
        this.context = context;
        this.viewport = new TableViewportPolicy(WindowContent.heightDp(context));
        this.state = state;
        this.copy = copy;
        this.bidding = bidding;
        this.interactionPaused = interactionPaused;
        this.completedTrickToDisplay = List.copyOf(completedTrickToDisplay);
        this.playableCards = new HashSet<>(playableCards);
        this.onCardSelected = onCardSelected;
        this.bidOptions = List.copyOf(bidOptions);
        this.selectedBid = selectedBid;
        this.onBidSelected = onBidSelected;
        this.onBidSubmitted = onBidSubmitted;

        setOrientation(VERTICAL);
        setGravity(Gravity.BOTTOM);
        setPadding(dp(8), dp(viewport.outerVerticalPaddingDp()), dp(8), dp(viewport.outerVerticalPaddingDp()));
        setBackgroundColor(Color.TRANSPARENT);
        setClipChildren(false);
        setContentDescription(copy.tableAccessibility(state.players().size()));

        View board = buildRoundTable();
        LayoutParams boardParams = new LayoutParams(dp(Math.min(availableContentWidthDp(), 760)),
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        boardParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(board, boardParams);
        if (!bidding && viewport.showPlayHint()) {
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
        LayoutParams handParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp((HandLayoutMetrics.cardHeightDp() + viewport.handClearanceDp(bidding))));
        handParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(buildHand(), handParams);
    }

    private View buildRoundTable() {
        int tableWidthDp = Math.min(availableContentWidthDp(), 760);
        TrickCardMetrics.Layout trickLayout = bidding
                ? null : TrickCardMetrics.forTable(state.players().size(), tableWidthDp,
                        getResources().getConfiguration().fontScale, viewport);
        int centerWidthDp = bidding
                ? Math.max(132, Math.min(bidOptions.size(), BidGridMetrics.columnCount(bidOptions.size(), tableWidthDp)) * 52)
                : TableLayoutMetrics.centerPlayWidthDp(tableWidthDp, viewport.isCompact());
        GameBoardLayout board = new GameBoardLayout(context, state.players().size(), centerWidthDp,
                viewport, !bidding, bidding ? TableGeometry.CenterAlignment.TOWARD_HUMAN
                        : TableGeometry.CenterAlignment.BALANCED);
        board.setBackgroundColor(Color.TRANSPARENT);
        board.setContentDescription(copy.boardSurface());
        board.setTag("game-board");
        board.setClipChildren(false);

        for (GameSession.PlayerSnapshot player : state.players()) {
            board.addView(buildSeat(player));
        }
        View center = bidding ? buildBiddingCenter() : buildCurrentTrick(trickLayout);
        board.addView(center);
        return board;
    }

    private View buildBiddingCenter() {
        LinearLayout center = new LinearLayout(context);
        center.setOrientation(VERTICAL);
        center.setGravity(Gravity.CENTER);
        int columns = BidGridMetrics.columnCount(bidOptions.size(), availableContentWidthDp());

        center.setContentDescription(copy.bidSelector());

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
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(BidGridMetrics.CHOICE_ROW_DP)));
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
        boolean active = !interactionPaused && player.seat() == activeSeat;
        boolean dealer = player.seat() == state.dealerSeat();
        LinearLayout seat = new LinearLayout(context);
        seat.setOrientation(VERTICAL);
        seat.setGravity(Gravity.CENTER);
        seat.setPadding(dp(6), dp(viewport.seatVerticalPaddingDp()), dp(6),
                dp(viewport.seatVerticalPaddingDp()));
        seat.setBackground(WhistUiPalette.seatFrame(context, active));
        seat.setElevation(dp(active ? 1 : 0));

        FrameLayout actionSlot = new FrameLayout(context);
        FrameLayout.LayoutParams actionBadgeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        if (active) {
            TextView action = text(copy.seatTurnBadge(bidding), 9, INK, true);
            action.setGravity(Gravity.CENTER);
            int badgePadding = viewport.turnBadgeVerticalPaddingDp();
            action.setPadding(dp(6), dp(badgePadding), dp(6), dp(badgePadding));
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
        TextView name = text(copy.seatName(player), 13,
                active ? INK : Color.rgb(229, 239, 230), true);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (viewport.isCompact()) {
            LinearLayout heading = new LinearLayout(context);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            heading.addView(name, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (active) {
                TextView badge = (TextView) actionSlot.getChildAt(0);
                actionSlot.removeView(badge);
                badge.setTextSize(8);
                badge.setPadding(0, 0, 0, 0);
                badge.setCompoundDrawablesRelative(null, null, null, null);
                badge.setMaxLines(2);
                heading.addView(badge, new LayoutParams(dp(27), ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            seat.addView(heading, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(viewport.seatHeadingHeightDp())));
        } else {
            seat.addView(actionSlot, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(viewport.turnBadgeSlotHeightDp())));
            seat.addView(name, matchWrap());
        }
        addSeatFact(seat, copy.bidLabel() + ":", player.bid() == null ? "—" : player.bid().toString(), active);
        addSeatFact(seat, copy.seatWonLabel() + ":", Integer.toString(player.tricksWon()), active);
        seat.setTag("table-seat:" + player.seat());
        seat.setContentDescription(copy.seatAccessibility(player, dealer, active, bidding));
        seat.setFocusable(true);
        seat.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return seat;
    }

    private View buildCurrentTrick(TrickCardMetrics.Layout layout) {
        List<PlayedCard> plays = state.currentTrick().isEmpty() && !completedTrickToDisplay.isEmpty()
                ? completedTrickToDisplay : state.currentTrick();
        FrameLayout cards = new FrameLayout(context);
        cards.setContentDescription(copy.currentTrick());
        cards.setTag("current-trick");
        cards.setMinimumHeight(dp(layout.centerHeightDp()));
        cards.setClipChildren(false);
        cards.setClipToPadding(false);

        PlayedCard[] bySeat = new PlayedCard[state.players().size()];
        for (PlayedCard play : plays) bySeat[play.seat()] = play;
        for (int rowIndex = 0; rowIndex < layout.rows(); rowIndex++) {
            FrameLayout row = new FrameLayout(context);
            FrameLayout.LayoutParams rowParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(layout.cellHeightDp()),
                    Gravity.TOP | Gravity.LEFT);
            rowParams.topMargin = dp(rowIndex * layout.rowStrideDp());
            cards.addView(row, rowParams);
        }
        for (int seat = 0; seat < bySeat.length; seat++) {
            PlayedCard play = bySeat[seat];
            if (play == null) continue;
            TrickCardMetrics.Slot slot = layout.slotForSeat(seat);
            GameSession.PlayerSnapshot player = state.players().get(seat);
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(VERTICAL);
            item.setGravity(Gravity.CENTER);
            int verticalPadding = dp(layout.itemVerticalPaddingDp() / 2);
            item.setPadding(0, verticalPadding, 0, verticalPadding);

            TextView card = text(play.card().shortLabel(), layout.cardFaceTextSizeSp(),
                    play.card().suit().red() ? CARD_RED : CARD_DARK, true);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(2), dp(4), dp(2), dp(4));
            card.setBackground(rounded(WHITE, Color.rgb(220, 227, 238), 9));
            card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.addView(card, new LayoutParams(dp(layout.cardWidthDp()), dp(layout.cardHeightDp())));

            TextView who = text(copy.trickPlayer(player), layout.playerLabelTextSizeSp(),
                    Color.rgb(212, 223, 209), false);
            who.setGravity(Gravity.CENTER);
            who.setSingleLine(true);
            who.setEllipsize(android.text.TextUtils.TruncateAt.END);
            who.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            if (layout.playerLabelHeightDp() > 0) {
                item.addView(who, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(layout.playerLabelHeightDp())));
            }

            item.setContentDescription(copy.trickCardAccessibility(player, play.card()));
            item.setTag("trick-card:" + seat);
            item.setFocusable(true);
            item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            FrameLayout.LayoutParams itemParams = new FrameLayout.LayoutParams(
                    dp(layout.cardWidthDp()), dp(layout.cellHeightDp()), Gravity.TOP | Gravity.LEFT);
            itemParams.leftMargin = dp(slot.leftDp());
            rowFor(cards, slot.row()).addView(item, itemParams);
        }
        return cards;
    }

    private FrameLayout rowFor(FrameLayout cards, int index) {
        return (FrameLayout) cards.getChildAt(index);
    }

    private View buildHand() {
        int count = state.players().get(0).hand().size();
        CenteredHandView hand = new CenteredHandView(context, dp(HandLayoutMetrics.maxViewportWidthDp()),
                width -> buildFan(count, width));
        hand.setContentDescription(copy.handAccessibility(count));
        hand.setTag("player-hand");
        return hand;
    }

    private View buildFan(int cardCount, int widthPx) {
        float fontScale = getResources().getConfiguration().fontScale;
        FrameLayout fan = new FrameLayout(context);
        fan.setClipChildren(false);
        fan.setClipToPadding(false);
        TextView measurement = text("10", HandLayoutMetrics.cardLabelSizeSp(), CARD_DARK, true);
        float widestIndex = measurement.getPaint().measureText("10");
        for (String symbol : List.of("♠", "♥", "♣", "♦")) widestIndex = Math.max(widestIndex, measurement.getPaint().measureText(symbol));
        int indexWidthDp = (int) Math.ceil(widestIndex / getResources().getDisplayMetrics().density) + 4;
        if (cardCount == 0) return fan;
        HandLayoutMetrics.Fan layout = HandLayoutMetrics.fan(cardCount, widthPx,
                getResources().getDisplayMetrics().density, fontScale, dp(indexWidthDp));
        fan.setMinimumWidth(layout.width());
        int cardHeightDp = HandLayoutMetrics.cardHeightDp();
        int liftClearanceDp = viewport.handClearanceDp(bidding);
        fan.setTag("player-hand-fan");
        for (int index = 0; index < cardCount; index++) {
            Card card = state.players().get(0).hand().get(index);
            boolean playable = !bidding && onCardSelected != null && playableCards.contains(card);
            boolean humanTurn = state.phase() != ro.whistlab.core.session.GamePhase.PLAYING
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
            FrameLayout face = cardFace(card, playable, indexWidthDp);
            FrameLayout.LayoutParams faceParams = new FrameLayout.LayoutParams(layout.cardWidth(),
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
                    index == cardCount - 1 ? layout.cardWidth() : layout.advance(),
                    dp((HandLayoutMetrics.cardHeightDp() + viewport.handClearanceDp(bidding))));
            slotParams.leftMargin = layout.left(index);
            fan.addView(slot, slotParams);
        }
        return fan;
    }

    private FrameLayout cardFace(Card card, boolean playable, int indexWidthDp) {
        FrameLayout face = new FrameLayout(context);
        face.setBackground(new CardFaceBackground(card, getResources().getDisplayMetrics().density, playable ? GOLD : SOFT_BORDER));
        face.setFocusable(false);
        face.setClickable(false);

        TextView label = text(card.rankLabel() + "\n" + card.suit().symbol(), HandLayoutMetrics.cardLabelSizeSp(),
                card.suit().red() ? CARD_RED : CARD_DARK, true);
        label.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        label.setIncludeFontPadding(false);
        label.setPadding(dp(2), dp(5), dp(2), 0);
        label.setTag("card-index");
        face.addView(label, new FrameLayout.LayoutParams(
                dp(indexWidthDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        return face;
    }

    private void addSeatFact(LinearLayout seat, String label, String value, boolean active) {
        LinearLayout row = new LinearLayout(context);
        int color = active ? INK : Color.rgb(229, 239, 230);
        TextView caption = text(label, 11, color, false);
        TextView amount = text(value, 11, color, true);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        amount.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        amount.setGravity(Gravity.END);
        row.addView(caption, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(amount, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        seat.addView(row, matchWrap());
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setIncludeFontPadding(!viewport.isCompact());
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

    private int availableContentWidthDp() {
        return Math.max(1, WindowContent.widthDp(context) - 40);
    }

}
