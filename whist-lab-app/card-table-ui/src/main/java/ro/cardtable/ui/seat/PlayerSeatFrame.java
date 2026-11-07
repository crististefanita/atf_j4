package ro.cardtable.ui.seat;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import static android.widget.LinearLayout.VERTICAL;
import android.widget.TextView;
import java.util.List;
import ro.cardtable.ui.theme.TablePalette;

/** Shared frame; the consumer provides ordered facts and the complete accessible description. */
public final class PlayerSeatFrame {
    private static final int INK = TablePalette.ACTIVE_SEAT_TEXT;
    private final Context context;
    private final boolean compact;
    public PlayerSeatFrame(Context context, boolean compact) { this.context = context; this.compact = compact; }
    public LinearLayout build(SeatContent content, int verticalPaddingDp) {
        boolean active = content.active();
        LinearLayout seat = new LinearLayout(context);
        seat.setOrientation(VERTICAL);
        seat.setGravity(Gravity.CENTER);
        seat.setPadding(dp(6), dp(verticalPaddingDp), dp(6),
                dp(verticalPaddingDp));
        seat.setBackground(TablePalette.seatFrame(context, active));
        seat.setElevation(dp(active ? 1 : 0));

        TextView name = text(content.name(), 13,
                active ? INK : TablePalette.IDLE_SEAT_TEXT, true);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setTag("seat-name:" + content.seat());
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seat.addView(name, matchWrap());
        for (SeatContent.Fact fact : content.facts()) addSeatFact(seat, fact.label(), fact.value(), active);
        seat.setTag("table-seat:" + content.seat());
        seat.setContentDescription(content.description());
        seat.setFocusable(true);
        seat.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return seat;
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
        view.setIncludeFontPadding(!compact);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    private LayoutParams matchWrap() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
