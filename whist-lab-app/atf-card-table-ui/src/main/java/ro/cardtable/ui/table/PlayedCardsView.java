package ro.cardtable.ui.table;
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
import android.widget.TextView;
import java.util.List;
import android.widget.LinearLayout.LayoutParams;
import static android.widget.LinearLayout.VERTICAL;

/** Seat-aligned public cards; the consumer owns reveal, scoring and collection timing. */
public final class PlayedCardsView {
    private static final int WHITE = Color.rgb(255, 254, 248);
    private static final int CARD_RED = Color.rgb(173, 57, 58);
    private static final int CARD_DARK = Color.rgb(42, 53, 47);
    private final Context context;
    private final boolean compact;
    public PlayedCardsView(Context context, boolean compact) { this.context = context; this.compact = compact; }
    public View build(List<PlayedCardContent> plays, PlayedCardMetrics.Layout layout, String description) {
        FrameLayout cards = new FrameLayout(context);
        cards.setContentDescription(description);
        cards.setTag("current-trick");
        cards.setMinimumHeight(dp(layout.centerHeightDp()));
        cards.setClipChildren(false);
        cards.setClipToPadding(false);

        PlayedCardContent[] bySeat = new PlayedCardContent[layout.slots().size()];
        for (PlayedCardContent play : plays) bySeat[play.seat()] = play;
        for (int rowIndex = 0; rowIndex < layout.rows(); rowIndex++) {
            FrameLayout row = new FrameLayout(context);
            FrameLayout.LayoutParams rowParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(layout.cellHeightDp()),
                    Gravity.TOP | Gravity.LEFT);
            rowParams.topMargin = dp(rowIndex * layout.rowStrideDp());
            cards.addView(row, rowParams);
        }
        for (int seat = 0; seat < bySeat.length; seat++) {
            PlayedCardContent play = bySeat[seat];
            if (play == null) continue;
            PlayedCardMetrics.Slot slot = layout.slotForSeat(seat);
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(VERTICAL);
            item.setGravity(Gravity.CENTER);
            int verticalPadding = dp(layout.itemVerticalPaddingDp() / 2);
            item.setPadding(0, verticalPadding, 0, verticalPadding);

            TextView card = text(play.faceLabel(), layout.cardFaceTextSizeSp(),
                    play.red() ? CARD_RED : CARD_DARK, true);
            card.setTag("trick-face:" + seat);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(2), dp(4), dp(2), dp(4));
            card.setBackground(rounded(WHITE, Color.rgb(220, 227, 238), 9));
            card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.addView(card, new LayoutParams(dp(layout.cardWidthDp()), dp(layout.cardHeightDp())));

            TextView who = text(play.playerLabel(), layout.playerLabelTextSizeSp(),
                    Color.rgb(212, 223, 209), false);
            who.setGravity(Gravity.CENTER);
            who.setSingleLine(true);
            who.setEllipsize(android.text.TextUtils.TruncateAt.END);
            who.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            if (layout.playerLabelHeightDp() > 0) {
                item.addView(who, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(layout.playerLabelHeightDp())));
            }

            item.setContentDescription(play.description());
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

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setIncludeFontPadding(!compact);
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
    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
