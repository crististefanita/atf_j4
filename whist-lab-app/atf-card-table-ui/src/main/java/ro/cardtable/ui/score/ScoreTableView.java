package ro.cardtable.ui.score;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.stream.Collectors;
import ro.cardtable.ui.table.ScoreGridStyle;
import ro.cardtable.ui.score.ScoreTypography;

/** Neutral score widget: the adapter supplies all column roles and confirmed values. */
public class ScoreTableView extends LinearLayout {
    public ScoreTableView(Context context, ScoreTableContent content) {
        super(context);
        setOrientation(VERTICAL);
        var shape = new GradientDrawable();
        shape.setColor(ScoreGridStyle.PAPER); shape.setCornerRadius(dp(12)); shape.setStroke(dp(1), ScoreGridStyle.RULE);
        setBackground(shape);
        addRow(content.columns(), content.columns().stream().map(c -> new ScoreTableContent.Cell(c.label())).collect(Collectors.toList()), true, false, 0);
        int index = 0;
        for (var row : content.rows()) {
            var divider = new View(context); divider.setBackgroundColor(ScoreGridStyle.RULE);
            addView(divider, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            var view = addRow(content.columns(), row.cells(), false, row.highlighted(), index++);
            view.setTag(row.id());
        }
    }
    private LinearLayout addRow(List<ScoreTableContent.Column> columns, List<ScoreTableContent.Cell> values,
            boolean header, boolean highlighted, int index) {
        var row = new LinearLayout(getContext()); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(8), 0, dp(8), 0);
        row.setBackgroundColor(header ? ScoreGridStyle.HEADER : highlighted ? ScoreGridStyle.HUMAN_ROW
                : index % 2 == 1 ? ScoreGridStyle.STRIPE : ScoreGridStyle.PAPER);
        row.setContentDescription(String.join(" · ", values.stream().map(ScoreTableContent.Cell::description).collect(Collectors.toList())));
        for (int column = 0; column < columns.size(); column++) {
            var data = values.get(column); var spec = columns.get(column);
            var cell = new TextView(getContext()); cell.setText(data.text()); cell.setContentDescription(data.description());
            int size = (header ? ScoreTypography.COLUMN : ScoreTypography.VALUE).sp();
            cell.setTextSize(size); cell.setTextColor(header ? ScoreGridStyle.MUTED : ScoreGridStyle.INK);
            cell.setTypeface(Typeface.DEFAULT, data.emphasized() ? Typeface.BOLD : Typeface.NORMAL);
            cell.setGravity(spec.numeric() ? Gravity.CENTER : Gravity.CENTER_VERTICAL | Gravity.START);
            cell.setSingleLine(true);
            if (Build.VERSION.SDK_INT >= 26) cell.setAutoSizeTextTypeUniformWithConfiguration(10, size, 1, TypedValue.COMPLEX_UNIT_SP);
            cell.setEllipsize(TextUtils.TruncateAt.END);
            if (data.underlined()) cell.setPaintFlags(cell.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            row.addView(cell, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, spec.weight()));
        }
        addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(header ? ScoreGridStyle.HEADER_DP : ScoreGridStyle.ROW_DP)));
        return row;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
