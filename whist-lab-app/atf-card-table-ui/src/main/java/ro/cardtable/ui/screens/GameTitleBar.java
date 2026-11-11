package ro.cardtable.ui.screens;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.function.Consumer;

/** Owns the first header line and moves the same trailing control to a second line when needed. */
public final class GameTitleBar extends ViewGroup {
    public static final String AUTO_PLAY_TAG = "auto-game-header-toggle";
    public static final int TOUCH_TARGET_DP = 48;
    private static final int UNBOUNDED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    public record AutoPlay(String label, String startDescription, String stopDescription,
            boolean checked, boolean enabled, int color, Consumer<Boolean> changed) { }

    private final View leading;
    private final TextView title;
    private final CheckBox toggle;
    private final int touchTarget;
    private final int gap;
    private GameTitleLayout placement;
    private int titleLineHeight;

    public GameTitleBar(Context context, View leading, TextView title, AutoPlay autoPlay) {
        super(context);
        this.leading = leading;
        this.title = title;
        float density = getResources().getDisplayMetrics().density;
        touchTarget = Math.round(TOUCH_TARGET_DP * density);
        gap = Math.round(8 * density);
        toggle = autoPlay == null ? null : makeToggle(autoPlay);
        addView(leading);
        addView(title);
        if (toggle != null) addView(toggle);
    }

    private CheckBox makeToggle(AutoPlay control) {
        var checkbox = new CheckBox(getContext());
        checkbox.setTag(AUTO_PLAY_TAG);
        checkbox.setText(control.label());
        checkbox.setTextSize(13);
        checkbox.setSingleLine(false);
        checkbox.setTextColor(control.color());
        checkbox.setButtonTintList(ColorStateList.valueOf(control.color()));
        checkbox.setPadding(0, 0, 0, 0);
        checkbox.setMinHeight(touchTarget);
        checkbox.setMinimumWidth(touchTarget);
        checkbox.setFocusable(true);
        checkbox.setEnabled(control.enabled());
        checkbox.setChecked(control.checked());
        checkbox.setContentDescription(control.checked() ? control.stopDescription() : control.startDescription());
        checkbox.setOnCheckedChangeListener((button, checked) -> {
            button.setContentDescription(checked ? control.stopDescription() : control.startDescription());
            control.changed().accept(checked);
        });
        return checkbox;
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int leadingSize = Math.min(width, touchTarget);
        leading.measure(exact(leadingSize), exact(touchTarget));
        title.measure(UNBOUNDED, UNBOUNDED);
        int toggleWidth = 0;
        if (toggle != null) {
            toggle.measure(UNBOUNDED, UNBOUNDED);
            toggleWidth = toggle.getMeasuredWidth();
        }
        placement = GameTitleLayout.measure(width, leadingSize, gap, title.getMeasuredWidth(), toggleWidth);
        title.measure(atMost(placement.titleWidth()), UNBOUNDED);
        titleLineHeight = Math.max(leading.getMeasuredHeight(), title.getMeasuredHeight());
        int height = titleLineHeight;
        if (toggle != null) {
            toggle.measure(atMost(width), UNBOUNDED);
            if (placement.trailingOnTitleLine()) {
                height = Math.max(height, toggle.getMeasuredHeight());
                titleLineHeight = height;
            } else {
                height += toggle.getMeasuredHeight();
            }
        }
        setMeasuredDimension(width, resolveSize(height, heightSpec));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        place(leading, 0, centered(leading));
        place(title, placement.titleLeft(), centered(title));
        if (toggle != null) {
            int x = getWidth() - toggle.getMeasuredWidth();
            place(toggle, x, placement.trailingOnTitleLine() ? centered(toggle) : titleLineHeight);
        }
    }

    @Override protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private int centered(View child) { return (titleLineHeight - child.getMeasuredHeight()) / 2; }

    private void place(View child, int x, int y) {
        child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }

    private static int exact(int size) { return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY); }
    private static int atMost(int size) { return MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST); }
}
