package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.cardtable.ui.animation.TrickCollectionAnimator;
import ro.whistlab.app.animation.TrickCollectionRequest;
import ro.whistlab.app.hand.CenteredHandView;
import ro.whistlab.app.hand.HandLayoutMetrics;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.seat.PlayerSeatView;
import ro.whistlab.app.seat.PlayerSeatMetrics;
import ro.whistlab.app.theme.WhistUiPalette;
import ro.whistlab.app.window.WindowContent;
import ro.whistlab.app.table.content.WhistTableContent;
import ro.cardtable.ui.hand.HandFan;
import ro.cardtable.ui.seat.PlayerSeatFrame;
import ro.cardtable.ui.table.PlayedCardsView;

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
    private static final int INK = WhistUiPalette.ACTIVE_SEAT_TEXT;
    private static final int GOLD = Color.rgb(220, 181, 105);
    private static final int PLAY_HINT_SLOT_HEIGHT_DP = 32;

    private final Context context;
    private final TableViewportPolicy viewport;
    private final GameSession.Snapshot state;
    private final WhistUiText copy;
    private final ViewGroup tableBoard;
    private final boolean bidding;
    private final WhistTableContent.Turn turn;
    private final boolean collecting;
    private final List<PlayedCard> completedTrickToDisplay;
    private final Set<Card> playableCards;
    private final Consumer<Card> onCardSelected;
    private final List<Integer> bidOptions;
    private final List<Button> bidChoiceButtons = new ArrayList<>();
    private final IntConsumer onBidSelected;
    private final Runnable onBidSubmitted;
    private int selectedBid;
    private Button bidSubmitButton;
    private boolean collectionAttached;
    private TrickCollectionAnimator collection;

    public void cancelCollection() {
        if (collection != null) { collection.cancel(); collection = null; }
    }

    public void collectCompletedTrick(long remainingMillis, Runnable resized) {
        if (collectionAttached || !collecting || remainingMillis <= 0) return;
        var request = TrickCollectionRequest.from(state, completedTrickToDisplay);
        if (!request.isPresent()) return;
        collectionAttached = true;
        collection = TrickCollectionAnimator.attach(tableBoard, request.get(), remainingMillis, resized);
    }

    public static GameTablePanel bidding(Context context, GameSession.Snapshot state,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted, WhistUiText copy) {
        return bidding(context, state, bidOptions, selectedBid, onBidSelected, onBidSubmitted,
                WhistTableContent.turn(state, false, false), copy);
    }

    public static GameTablePanel bidding(Context context, GameSession.Snapshot state,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted, WhistTableContent.Turn turn, WhistUiText copy) {
        return new GameTablePanel(context, state, true, Collections.emptyList(), null,
                null, bidOptions, selectedBid, onBidSelected, onBidSubmitted, copy,
                Collections.emptyList(), turn, false);
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
                Collections.emptyList(), 0, null, null, copy, completedTrickToDisplay,
                WhistTableContent.turn(state, false, false), false);
    }

    public static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint,
            List<PlayedCard> completedTrickToDisplay, boolean interactionPaused, WhistUiText copy) {
        return playing(context, state, legalCards, onCardSelected, hint, completedTrickToDisplay,
                WhistTableContent.turn(state, interactionPaused, false), interactionPaused, copy);
    }

    public static GameTablePanel playing(Context context, GameSession.Snapshot state,
            List<Card> legalCards, Consumer<Card> onCardSelected, String hint,
            List<PlayedCard> completedTrickToDisplay, WhistTableContent.Turn turn,
            boolean collecting, WhistUiText copy) {
        return new GameTablePanel(context, state, false, legalCards, onCardSelected, hint,
                Collections.emptyList(), 0, null, null, copy, completedTrickToDisplay,
                turn, collecting);
    }

    private GameTablePanel(Context context, GameSession.Snapshot state, boolean bidding,
            List<Card> playableCards, Consumer<Card> onCardSelected, String hint,
            List<Integer> bidOptions, int selectedBid, IntConsumer onBidSelected,
            Runnable onBidSubmitted,
            WhistUiText copy, List<PlayedCard> completedTrickToDisplay,
            WhistTableContent.Turn turn, boolean collecting) {
        super(context);
        this.context = context;
        this.viewport = new TableViewportPolicy(WindowContent.heightDp(context),
                getResources().getConfiguration().fontScale);
        this.state = state;
        this.copy = copy;
        this.bidding = bidding;
        this.turn = java.util.Objects.requireNonNull(turn);
        this.collecting = collecting;
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

        tableBoard = buildRoundTable();
        LayoutParams boardParams = new LayoutParams(dp(Math.min(availableContentWidthDp(), 760)),
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        boardParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(tableBoard, boardParams);
        if (viewport.showPlayHint()) {
            FrameLayout hintSlot = new FrameLayout(context);
            TextView help = text(hint == null ? "" : hint, 11, Color.rgb(222, 237, 225), false);
            help.setGravity(Gravity.CENTER);
            help.setMaxLines(2);
            help.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (bidding || hint == null || hint.isBlank()) help.setVisibility(View.INVISIBLE);
            else help.setContentDescription(hint);
            hintSlot.addView(help, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(PLAY_HINT_SLOT_HEIGHT_DP));
            params.topMargin = dp(4);
            addView(hintSlot, params);
        }
        LayoutParams handParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                TableLayoutMetrics.privateHandHeightPx(getResources().getDisplayMetrics().density));
        handParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(buildHand(), handParams);
    }

    private ViewGroup buildRoundTable() {
        int tableWidthDp = Math.min(availableContentWidthDp(), 760);
        float fontScale = getResources().getConfiguration().fontScale;
        TrickCardMetrics.Layout trickLayout = bidding
                ? null : TrickCardMetrics.forTable(state.players().size(), tableWidthDp, fontScale, viewport);
        var bidLayout = bidding ? BidGridMetrics.actionLayout(bidOptions.size(), tableWidthDp,
                fontScale) : null;
        int reservedCenterHeightDp = TableLayoutMetrics.centerReservation(state.players().size(),
                state.handSize(), tableWidthDp, fontScale, viewport).reservedHeightDp();
        int centerWidthDp = bidding ? bidLayout.centerWidthDp()
                : trickLayout.safeCenterWidthDp();
        float density = getResources().getDisplayMetrics().density;
        int boardWidthDp = Math.round(dp(tableWidthDp) / density);
        int seatWidthPx = dp(TableLayoutMetrics.seatWidthDp(boardWidthDp, viewport.isCompact()));
        int stableBadgeHeightPx = PlayerSeatMetrics.stableBadgeHeightPx(
                PlayerSeatView.measureActionHeightPx(context, copy.seatTurnBadge(true), seatWidthPx),
                PlayerSeatView.measureActionHeightPx(context, copy.seatTurnBadge(false), seatWidthPx));
        GameBoardLayout board = new GameBoardLayout(context, state.players().size(), centerWidthDp,
                viewport, !bidding, reservedCenterHeightDp, TableLayoutMetrics.humanHandClearanceDp(),
                bidding ? TableGeometry.CenterAlignment.TOWARD_HUMAN
                        : TableGeometry.CenterAlignment.BALANCED);
        board.setBackgroundColor(Color.TRANSPARENT);
        board.setContentDescription(copy.boardSurface());
        board.setTag("game-board");
        board.setClipChildren(false);

        for (GameSession.PlayerSnapshot player : state.players()) {
            board.addView(buildSeat(player, stableBadgeHeightPx));
        }
        View center = bidding ? buildBiddingCenter(tableWidthDp) : buildCurrentTrick(trickLayout);
        board.addView(center);
        return board;
    }

    private View buildBiddingCenter(int tableWidthDp) {
        LinearLayout center = new LinearLayout(context);
        center.setOrientation(VERTICAL);
        center.setGravity(Gravity.CENTER);
        int columns = BidGridMetrics.columnCount(bidOptions.size(), tableWidthDp);

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
                choice.setEnabled(turn.manual());
                choice.setAlpha(turn.manual() ? 1f : 0.5f);
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
        center.addView(buildBidSubmission(), new LayoutParams(dp(BidGridMetrics.SUBMIT_WIDTH_DP),
                dp(BidGridMetrics.SUBMIT_TARGET_DP)));
        return center;
    }

    private View buildBidSubmission() {
        Button submit = new Button(context);
        bidSubmitButton = submit;
        submit.setAllCaps(false);
        submit.setText(copy.placeBid());
        submit.setTextSize(BidGridMetrics.SUBMIT_TEXT_SP);
        submit.setTextColor(Color.rgb(245, 248, 240));
        submit.setContentDescription(copy.placeBid());
        submit.setTag("btn_submit_bid");
        updateBidSubmission();
        submit.setMinimumWidth(0);
        submit.setMinHeight(dp(BidGridMetrics.SUBMIT_TARGET_DP));
        submit.setStateListAnimator(null);
        var font = submit.getPaint().getFontMetricsInt();
        int textWidth = (int) Math.ceil(android.text.Layout.getDesiredWidth(submit.getText(), submit.getPaint()));
        var inset = BidGridMetrics.submitInsets(getResources().getDisplayMetrics().density,
                textWidth, font.bottom - font.top);
        submit.setBackground(new InsetDrawable(
                rounded(Color.TRANSPARENT, Color.rgb(194, 216, 199), 12),
                inset.horizontalPx(), inset.verticalPx(), inset.horizontalPx(), inset.verticalPx()));
        submit.setPadding(dp(6), 0, dp(6), 0);
        submit.setOnClickListener(ignored -> {
            if (onBidSubmitted != null) onBidSubmitted.run();
        });
        return submit;
    }

    private void selectBid(int bid) {
        if (!turn.manual()) return;
        selectedBid = bid;
        updateBidSubmission();
        for (Button choice : bidChoiceButtons) {
            boolean selected = Integer.parseInt(String.valueOf(choice.getTag()).substring("bid-choice:".length())) == bid;
            choice.setSelected(selected);
            styleBidChoice(choice, selected);
        }
        if (onBidSelected != null) onBidSelected.accept(bid);
    }

    private void updateBidSubmission() {
        boolean available = turn.manual() && bidOptions.contains(selectedBid);
        bidSubmitButton.setEnabled(available);
        bidSubmitButton.setAlpha(available ? 1f : 0.5f);
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

    private View buildSeat(GameSession.PlayerSnapshot player, int stableBadgeHeightPx) {
        var content = WhistTableContent.seat(player, turn.seat(), state.dealerSeat(), bidding, copy);
        LinearLayout frame = new PlayerSeatFrame(context, viewport.isCompact())
                .buildInlineFacts(content, viewport.seatVerticalPaddingDp());
        return new PlayerSeatView(context, frame, copy.seatTurnBadge(bidding), content.active(),
                player.seat(), viewport.isCompact(), stableBadgeHeightPx);
    }

    private View buildCurrentTrick(TrickCardMetrics.Layout layout) {
        List<PlayedCard> plays = state.currentTrick().isEmpty() && !completedTrickToDisplay.isEmpty()
                ? completedTrickToDisplay : state.currentTrick();
        return new PlayedCardsView(context, viewport.isCompact()).build(
                WhistTableContent.plays(state, plays, copy), layout.shared(), copy.currentTrick());
    }

    private View buildHand() {
        int count = state.players().get(0).hand().size();
        CenteredHandView hand = new CenteredHandView(context, dp(HandLayoutMetrics.maxViewportWidthDp()),
                new HandFan(context,
                        WhistTableContent.hand(state, playableCards, turn, bidding, copy),
                        HandLayoutMetrics.LIFT_CLEARANCE_DP, viewport.isCompact(),
                        id -> onCardSelected.accept(WhistTableContent.selected(state.players().get(0).hand(), id)))::build);
        hand.setContentDescription(copy.handAccessibility(count));
        hand.setTag("player-hand");
        return hand;
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
