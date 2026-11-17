package ro.cardtable.ui.table.android;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import ro.cardtable.ui.cards.CardFaceDrawable;
import ro.cardtable.ui.cards.CardFaceMetrics;
import ro.cardtable.ui.cards.CardFaceView;
import ro.cardtable.ui.table.PlayedCardContent;
import ro.cardtable.ui.table.PlayedCardMetrics;
import ro.cardtable.ui.table.PlayedCardStyle;
import android.widget.LinearLayout.LayoutParams;
import static android.widget.LinearLayout.VERTICAL;

/** Seat-aligned public cards; the consumer owns reveal, scoring and collection timing. */
public final class PlayedCardsView {
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
        var metrics = CardFaceMetrics.atHeight(layout.cardHeightDp());
        int indexWidthPx = CardFaceView.indexBandWidthPx(context, metrics);
        for (int seat = 0; seat < bySeat.length; seat++) {
            PlayedCardContent play = bySeat[seat];
            if (play == null) continue;
            PlayedCardMetrics.Slot slot = layout.slotForSeat(seat);
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(VERTICAL);
            item.setGravity(Gravity.CENTER);
            int verticalPadding = dp(layout.itemVerticalPaddingDp() / 2);
            item.setPadding(0, verticalPadding, 0, verticalPadding);

            var face = play.face();
            var background = new CardFaceDrawable(face, context.getResources().getDisplayMetrics().density,
                    PlayedCardStyle.OUTLINE);
            FrameLayout card = CardFaceView.build(context, face, background, metrics, indexWidthPx);
            card.setTag("trick-face:" + seat);
            card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            item.addView(card, new LayoutParams(dp(layout.cardWidthDp()), dp(layout.cardHeightDp())));

            TextView who = playerLabel(play.playerLabel(), layout.playerLabelTextSizeSp());
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

    private TextView playerLabel(String value, int size) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setIncludeFontPadding(!compact);
        view.setTextColor(PlayedCardStyle.PLAYER);
        return view;
    }
    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
