package ro.cardtable.ui.hand;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ro.cardtable.ui.cards.CardFace;
import ro.cardtable.ui.cards.CardHighlightView;

/** Measures the actual Android views with and without a highlight in the same viewport. */
public final class HandHighlightMeasurement {
    private static final int GOLD_FACE = 0xfffff6d7;
    private static final int REGULAR_FACE = 0xfffffef8;

    private HandHighlightMeasurement() { }

    public static JSONArray verify(Context target) throws JSONException {
        var result = new JSONArray();
        for (float scale : new float[]{1f, 1.5f, 2f}) {
            var config = new Configuration(target.getResources().getConfiguration());
            config.fontScale = scale;
            var context = target.createConfigurationContext(config);
            for (int widthDp : new int[]{280, 393}) result.put(verifyFan(context, scale, widthDp));
        }
        verifyRaisedCard(target);
        return result;
    }

    private static JSONObject verifyFan(Context context, float scale, int widthDp) throws JSONException {
        int viewport = dp(context, widthDp);
        var cards = cards();
        var regular = measured(new HandFan(context, cards, 28, true, id -> { }), viewport);
        var highlighted = measured(new HandFan(context, cards, 28, true, id -> { },
                card -> !"3".equals(card.id())), viewport);
        require(regular.getChildCount() == 8 && highlighted.getChildCount() == 8, "All own cards are visible");
        require(regular.getWidth() == highlighted.getWidth() && regular.getHeight() == highlighted.getHeight(),
                "Applying marks preserves the fan footprint");
        for (int index = 0; index < cards.size(); index++) {
            var card = cards.get(index);
            var plainSlot = (FrameLayout) regular.getChildAt(index);
            var markedSlot = (FrameLayout) highlighted.getChildAt(index);
            var plainFace = plainSlot.getChildAt(0);
            var markedFace = markedSlot.getChildAt(0);
            String identity = "card " + index + ", " + widthDp + " dp, font " + scale;
            require(sameBounds(plainSlot, markedSlot) && sameBounds(plainFace, markedFace), identity + " moved");
            require(plainSlot.getTag().equals(markedSlot.getTag())
                    && plainSlot.getContentDescription().equals(markedSlot.getContentDescription())
                    && markedSlot.isClickable() == card.playable()
                    && plainSlot.getTranslationZ() == markedSlot.getTranslationZ(), identity + " changed its action");
            require(markedFace.getTranslationY() == 0 && markedFace.getElevation() == 0, identity + " was raised");
            boolean expectedMark = index == 1 || index == 4;
            require(fill(plainFace) == REGULAR_FACE && fill(markedFace) == (expectedMark ? GOLD_FACE : REGULAR_FACE),
                    identity + " has the wrong fill");
            var label = markedFace.findViewWithTag("card-index");
            require(label.getWidth() > 0 && label.getWidth() <= markedSlot.getWidth(), identity + " index is covered");
        }
        return new JSONObject().put("widthDp", widthDp).put("fontScale", scale)
                .put("cards", cards.size()).put("highlighted", 2).put("positionsPreserved", true);
    }

    private static void verifyRaisedCard(Context context) {
        var view = new TextView(context);
        view.setTag("public-card");
        view.setContentDescription("Visible ace");
        view.layout(3, 5, 39, 53);
        CardHighlightView.raise(view, 48);
        require(view.getLeft() == 3 && view.getTop() == 5 && view.getRight() == 39 && view.getBottom() == 53,
                "Raised mark must preserve layout bounds");
        require(fill(view) == GOLD_FACE && view.getTranslationY() == -dp(context, 4)
                && view.getElevation() == dp(context, 8), "Raised face changed the reveal style");
        require("public-card".equals(view.getTag()) && "Visible ace".contentEquals(view.getContentDescription()),
                "Raised face changed the public identity");
    }

    private static List<HandCard> cards() {
        var ranks = List.of("7", "8", "9", "10", "J", "Q", "K", "A");
        var cards = new ArrayList<HandCard>();
        for (int index = 0; index < ranks.size(); index++) {
            boolean playable = index == 1 || index == 3 || index == 4;
            cards.add(new HandCard(Integer.toString(index), new CardFace(ranks.get(index), "♠", false),
                    ranks.get(index) + " spades", playable));
        }
        return cards;
    }

    private static FrameLayout measured(HandFan fan, int viewport) {
        var view = (FrameLayout) fan.build(viewport);
        view.measure(View.MeasureSpec.makeMeasureSpec(view.getMinimumWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        return view;
    }

    private static boolean sameBounds(View first, View second) {
        return first.getLeft() == second.getLeft() && first.getTop() == second.getTop()
                && first.getRight() == second.getRight() && first.getBottom() == second.getBottom();
    }

    private static int fill(View view) {
        return ((GradientDrawable) view.getBackground()).getColor().getDefaultColor();
    }

    private static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static void require(boolean valid, String diagnostic) {
        if (!valid) throw new IllegalStateException(diagnostic);
    }
}
