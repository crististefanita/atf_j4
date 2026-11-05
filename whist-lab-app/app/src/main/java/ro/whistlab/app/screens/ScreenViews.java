package ro.whistlab.app.screens;

import ro.whistlab.app.bidding.DropdownBackground;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.theme.WhistUiPalette;
import ro.whistlab.app.window.WindowContent;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Shared widget construction; screen classes own composition and the activity owns lifetime. */
public final class ScreenViews {
    static final int PINE = Color.rgb(29, 54, 43);
    static final int INK = Color.rgb(41, 54, 46);
    static final int MUTED = Color.rgb(105, 117, 106);
    static final int ACCENT = Color.rgb(48, 111, 78);
    static final int SURFACE = Color.rgb(255, 254, 248);
    static final int BACKGROUND = Color.rgb(246, 245, 237);
    private final Activity activity;
    private final java.util.function.Supplier<WhistUiText> language;
    public ScreenViews(Activity activity, java.util.function.Supplier<WhistUiText> language) {
        this.activity = java.util.Objects.requireNonNull(activity);
        this.language = java.util.Objects.requireNonNull(language);
    }
    Activity activity() { return activity; }
    public LinearLayout page(String eyebrow, String title) {
        return page(eyebrow, title, 22);
    }

    public LinearLayout page(String eyebrow, String title, int horizontalPadding) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(horizontalPadding), dp(16), dp(horizontalPadding), dp(14));
        content.setBackgroundColor(BACKGROUND);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        activity.setContentView(scroll);
        WindowContent.protect(scroll);
        if (eyebrow != null && !eyebrow.isBlank()) addText(content, eyebrow, 12, ACCENT, true, 0, 8);
        if (title != null && !title.isBlank()) addText(content, title, 25, PINE, true, 0, 8);
        return content;
    }

    public LinearLayout gamePage() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(2), dp(12), dp(2));
        content.setBackgroundColor(WhistUiPalette.TABLE_FELT);
        activity.setContentView(content);
        WindowContent.protect(content);
        return content;
    }

    public void applyStandardWindowChrome() {
        activity.getWindow().setStatusBarColor(PINE);
        activity.getWindow().setNavigationBarColor(BACKGROUND);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    public void applyTableWindowChrome() {
        activity.getWindow().setStatusBarColor(WhistUiPalette.TABLE_FELT);
        activity.getWindow().setNavigationBarColor(WhistUiPalette.TABLE_FELT);
        activity.getWindow().getDecorView().setSystemUiVisibility(0);
    }

    public void addRule(LinearLayout page, String heading, String body) {
        addText(page, heading, 16, INK, true, 18, 4);
        addText(page, body, 15, MUTED, false, 0, 4);
    }

    public void addText(LinearLayout parent, String value, int size, int color,
            boolean bold, int top, int bottom) {
        TextView view = text(value, size, color, bold);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(top), 0, dp(bottom));
        parent.addView(view, params);
    }

    public TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    public void addButton(LinearLayout parent, String label, String description, boolean enabled,
            Runnable action, boolean primary) {
        Button button = actionButton(label, description, enabled, action, primary, 16);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, dp(3));
        parent.addView(button, params);
    }

    public Button actionButton(String label, String description, boolean enabled,
            Runnable action, boolean primary, int textSize) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(textSize);
        button.setTextColor(primary ? Color.WHITE : PINE);
        button.setContentDescription(label);
        button.setTag(description);
        button.setEnabled(enabled);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setBackground(buttonBackground(primary));
        button.setOnClickListener(ignored -> action.run());
        return button;
    }

    public Spinner spinner(List<String> values, String description) {
        Spinner spinner = new Spinner(activity);
        spinner.setContentDescription(language.get().spinnerLabel(description));
        spinner.setTag(description);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, new ArrayList<>(values)) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                TextView selected = (TextView) super.getView(position, recycled, parent);
                selected.setTextSize(14);
                selected.setTextColor(INK);
                selected.setSingleLine(true);
                selected.setEllipsize(android.text.TextUtils.TruncateAt.END);
                return selected;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setGravity(Gravity.CENTER_VERTICAL);
        spinner.setBackground(new DropdownBackground(activity.getResources().getDisplayMetrics().density));
        spinner.setMinimumHeight(dp(48));
        spinner.setPadding(dp(12), dp(4), dp(34), dp(4));
        return spinner;
    }

    public void addSetupSelector(LinearLayout parent, String label, Spinner selector) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label, 14, INK, true), new LinearLayout.LayoutParams(0, dp(48), 1f));
        row.addView(selector, new LinearLayout.LayoutParams(dp(150), dp(48)));
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(8);
        parent.addView(row, params);
    }

    public void addQuietButton(LinearLayout parent, String label, Runnable action) {
        Button button = actionButton(label, label, true, action, false, 14);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setElevation(0);
        button.setStateListAnimator(null);
        parent.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
    }

    public GradientDrawable buttonBackground(boolean primary) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? ACCENT : SURFACE);
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1), primary ? ACCENT : Color.rgb(211, 220, 210));
        return shape;
    }

    public GradientDrawable roundedBackground(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    public LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
