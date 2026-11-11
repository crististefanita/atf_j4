package ro.cardtable.ui.hand;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import ro.cardtable.ui.cards.CardFaceDrawable;
import ro.cardtable.ui.cards.CardHighlight;
import ro.cardtable.ui.cards.CardHighlightView;

/** Renders a snapshot-bound hand without owning legality or executing a game command. */
public final class HandFan {
    private static final int GOLD = Color.rgb(220, 181, 105);
    private static final int CARD_RED = Color.rgb(173, 57, 58);
    private static final int CARD_DARK = Color.rgb(42, 53, 47);
    private static final int SOFT_BORDER = Color.rgb(211, 220, 210);
    private final Context context;
    private final List<HandCard> cards;
    private final int clearanceDp;
    private final boolean compact;
    private final Consumer<String> onCardSelected;
    private final Predicate<HandCard> highlight;

    public HandFan(Context context, List<HandCard> cards, int clearanceDp, boolean compact, Consumer<String> onCardSelected) {
        this(context, cards, clearanceDp, compact, onCardSelected, card -> false);
    }

    public HandFan(Context context, List<HandCard> cards, int clearanceDp, boolean compact,
            Consumer<String> onCardSelected, Predicate<HandCard> highlight) {
        this.context = context; this.cards = List.copyOf(cards); this.clearanceDp = clearanceDp;
        this.compact = compact; this.onCardSelected = java.util.Objects.requireNonNull(onCardSelected);
        this.highlight = java.util.Objects.requireNonNull(highlight);
        if (cards.stream().map(HandCard::id).distinct().count() != cards.size()) throw new IllegalArgumentException("Duplicate visible card identity");
    }
    public View build(int widthPx) {
        int cardCount = cards.size();
        float fontScale = context.getResources().getConfiguration().fontScale;
        FrameLayout fan = new FrameLayout(context);
        fan.setClipChildren(false);
        fan.setClipToPadding(false);
        TextView measurement = text("10", HandLayoutMetrics.cardLabelSizeSp(), CARD_DARK, true);
        float widestIndex = measurement.getPaint().measureText("10");
        for (String symbol : List.of("â™ ", "â™¥", "â™£", "â™¦")) widestIndex = Math.max(widestIndex, measurement.getPaint().measureText(symbol));
        int indexWidthPx = HandLayoutMetrics.indexBandWidthPx(widestIndex, dp(1));
        if (cardCount == 0) return fan;
        HandLayoutMetrics.Fan layout = HandLayoutMetrics.fan(cardCount, widthPx,
                context.getResources().getDisplayMetrics().density, fontScale, indexWidthPx);
        fan.setMinimumWidth(layout.width());
        int cardHeightDp = HandLayoutMetrics.cardHeightDp();
        int liftClearanceDp = clearanceDp;
        fan.setTag("player-hand-fan");
        for (int index = 0; index < cardCount; index++) {
            HandCard card = cards.get(index);
            boolean playable = card.playable();
            FrameLayout slot = new FrameLayout(context);
            slot.setClipChildren(false);
            slot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            slot.setFocusable(true);
            slot.setTag("hand-card:" + card.id());
            slot.setContentDescription(card.description());
            slot.setEnabled(true);
            slot.setClickable(playable);
            slot.setTranslationZ(dp(HandLayoutMetrics.NEIGHBOR_Z_DP));
            FrameLayout face = cardFace(card, playable, indexWidthPx);
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
                            .withEndAction(() -> onCardSelected.accept(card.id()))
                            .start();
                });
            }
            FrameLayout.LayoutParams slotParams = new FrameLayout.LayoutParams(
                    index == cardCount - 1 ? layout.cardWidth() : layout.advance(),
                    dp(HandLayoutMetrics.cardHeightDp()) + dp(clearanceDp));
            slotParams.leftMargin = layout.left(index);
            fan.addView(slot, slotParams);
        }
        return fan;
    }
    private FrameLayout cardFace(HandCard card, boolean playable, int indexWidthPx) {
        FrameLayout face = new FrameLayout(context);
        float density = context.getResources().getDisplayMetrics().density;
        var background = new CardFaceDrawable(card.face(), density, playable ? GOLD : SOFT_BORDER);
        if (playable && highlight.test(card)) CardHighlightView.paint(background, CardHighlight.IN_PLACE, density);
        face.setBackground(background);
        face.setFocusable(false);
        face.setClickable(false);

        TextView label = text(card.face().rankLabel() + "\n" + card.face().suitSymbol(), HandLayoutMetrics.cardLabelSizeSp(),
                card.face().red() ? CARD_RED : CARD_DARK, true);
        label.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        label.setIncludeFontPadding(false);
        label.setPadding(dp(1), dp(5), dp(1), 0);
        label.setTag("card-index");
        face.addView(label, new FrameLayout.LayoutParams(
                indexWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        return face;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setIncludeFontPadding(!compact);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

}
