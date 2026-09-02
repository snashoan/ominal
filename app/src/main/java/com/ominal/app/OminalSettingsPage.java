package com.ominal.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ominal.R;

import java.util.List;

/** Full-page settings navigation built from the same row model as focused pickers. */
final class OminalSettingsPage {
    private OminalSettingsPage() {
    }

    static View create(@NonNull Context context,
                       @NonNull OminalInteractionSheet.Theme theme,
                       @NonNull List<OminalInteractionSheet.Section> sections,
                       @NonNull Runnable onBack,
                       @NonNull OminalInteractionSheet.Listener listener) {
        return create(context, theme, "Settings", "", sections, onBack, listener);
    }

    static View create(@NonNull Context context,
                       @NonNull OminalInteractionSheet.Theme theme,
                       @NonNull String title,
                       @NonNull String subtitle,
                       @NonNull List<OminalInteractionSheet.Section> sections,
                       @NonNull Runnable onBack,
                       @NonNull OminalInteractionSheet.Listener listener) {
        LinearLayout page = new LinearLayout(context);
        page.setTag("settings-page");
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(theme.surface);

        addToolbar(context, page, theme, title, onBack);

        ScrollView scroller = new ScrollView(context);
        scroller.setFillViewport(true);
        scroller.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroller.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(context, 8), 0, dp(context, 32));
        addSubtitle(context, content, theme, subtitle);
        for (OminalInteractionSheet.Section section : sections)
            addSection(context, content, theme, section, listener);
        scroller.addView(content, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroller, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        return page;
    }

    static View createForm(@NonNull Context context,
                           @NonNull OminalInteractionSheet.Theme theme,
                           @NonNull String title,
                           @NonNull String subtitle,
                           @NonNull View form,
                           @NonNull Runnable onBack,
                           @NonNull Runnable onSave,
                           @NonNull Runnable onClear) {
        LinearLayout page = new LinearLayout(context);
        page.setTag("settings-form-page");
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(theme.surface);
        addToolbar(context, page, theme, title, onBack);

        ScrollView scroller = new ScrollView(context);
        scroller.setFillViewport(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 24));
        addSubtitle(context, body, theme, subtitle);
        body.addView(form, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        scroller.addView(body, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroller, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        actions.setPadding(dp(context, 20), dp(context, 10), dp(context, 20), dp(context, 14));
        actions.setBackgroundColor(theme.surface);

        Button clear = commandButton(context, theme, "Clear", false);
        clear.setTextColor(Color.rgb(255, 69, 58));
        clear.setOnClickListener(view -> onClear.run());
        actions.addView(clear, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 48)));

        Button save = commandButton(context, theme, "Save", true);
        save.setOnClickListener(view -> onSave.run());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
            dp(context, 116), dp(context, 48));
        saveParams.setMargins(dp(context, 12), 0, 0, 0);
        actions.addView(save, saveParams);
        page.addView(divider(context, theme.border), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)));
        page.addView(actions, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return page;
    }

    private static void addToolbar(Context context, LinearLayout page,
                                   OminalInteractionSheet.Theme theme,
                                   String titleText, Runnable onBack) {

        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(context, 12), dp(context, 8), dp(context, 20), dp(context, 8));
        toolbar.setMinimumHeight(dp(context, 60));

        ImageButton back = new ImageButton(context);
        back.setTag("settings-back");
        back.setImageResource(R.drawable.ic_lucide_arrow_left);
        back.setImageTintList(ColorStateList.valueOf(theme.text));
        back.setBackground(roundRipple(context, theme.surfaceRaised, theme.border, theme.text));
        back.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        back.setContentDescription("Back to chat");
        back.setOnClickListener(view -> onBack.run());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(theme.text);
        title.setTextSize(21);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(context, 10), 0, 0, 0);
        toolbar.addView(title, titleParams);
        page.addView(toolbar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(divider(context, theme.border), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)));
    }

    private static void addSubtitle(Context context, LinearLayout content,
                                    OminalInteractionSheet.Theme theme, String subtitle) {
        if (TextUtils.isEmpty(subtitle)) return;
        TextView view = new TextView(context);
        view.setText(subtitle);
        view.setTextColor(theme.muted);
        view.setTextSize(14);
        view.setLineSpacing(0, 1.12f);
        view.setIncludeFontPadding(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 12));
        content.addView(view, params);
    }

    private static void addSection(Context context, LinearLayout content,
                                   OminalInteractionSheet.Theme theme,
                                   OminalInteractionSheet.Section section,
                                   OminalInteractionSheet.Listener listener) {
        if (!TextUtils.isEmpty(section.label)) {
            TextView label = new TextView(context);
            label.setText(section.label);
            label.setTextColor(theme.muted);
            label.setTextSize(13);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setIncludeFontPadding(false);
            label.setPadding(dp(context, 24), dp(context, 22), dp(context, 24),
                dp(context, 8));
            content.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        for (int index = 0; index < section.rows.size(); index++) {
            OminalInteractionSheet.Row value = section.rows.get(index);
            content.addView(createRow(context, theme, value, listener),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            if (index < section.rows.size() - 1) {
                View divider = divider(context, theme.border);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1));
                dividerParams.setMargins(dp(context, 24), 0, dp(context, 20), 0);
                content.addView(divider, dividerParams);
            }
        }
    }

    private static View createRow(Context context, OminalInteractionSheet.Theme theme,
                                  OminalInteractionSheet.Row value,
                                  OminalInteractionSheet.Listener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setTag("settings-row:" + value.id);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 24), dp(context, 12), dp(context, 18), dp(context, 12));
        row.setMinimumHeight(dp(context, value.detail.isEmpty() ? 58 : 70));
        row.setEnabled(value.enabled);
        row.setAlpha(value.enabled ? 1f : 0.45f);
        row.setBackground(ripple(theme.text));
        row.setContentDescription(value.title
            + (value.detail.isEmpty() ? "" : ", " + value.detail)
            + (value.trailing.isEmpty() ? "" : ", " + value.trailing));

        if (value.iconRes != 0) {
            ImageView icon = new ImageView(context);
            icon.setImageResource(value.iconRes);
            icon.setImageTintList(ColorStateList.valueOf(theme.text));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dp(context, 22), dp(context, 22));
            iconParams.setMargins(0, 0, dp(context, 18), 0);
            row.addView(icon, iconParams);
        }

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(value.title);
        title.setTextColor(value.danger ? Color.rgb(255, 69, 58) : theme.text);
        title.setTextSize(16);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setIncludeFontPadding(false);
        labels.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (!value.detail.isEmpty()) {
            TextView detail = new TextView(context);
            detail.setText(value.detail);
            detail.setTextColor(theme.muted);
            detail.setTextSize(13);
            detail.setMaxLines(2);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            detail.setIncludeFontPadding(false);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            detailParams.setMargins(0, dp(context, 4), 0, 0);
            labels.addView(detail, detailParams);
        }
        row.addView(labels, new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (!value.trailing.isEmpty()) {
            TextView trailing = new TextView(context);
            trailing.setText(value.trailing);
            trailing.setTextColor(value.selected ? theme.accent : theme.muted);
            trailing.setTextSize(13);
            trailing.setSingleLine(true);
            trailing.setEllipsize(TextUtils.TruncateAt.END);
            trailing.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            trailing.setIncludeFontPadding(false);
            trailing.setMaxWidth(dp(context, 144));
            LinearLayout.LayoutParams trailingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            trailingParams.setMargins(dp(context, 12), 0, 0, 0);
            row.addView(trailing, trailingParams);
        }

        if (value.enabled) {
            ImageView chevron = new ImageView(context);
            chevron.setImageResource(R.drawable.ic_lucide_chevron_right);
            chevron.setImageTintList(ColorStateList.valueOf(theme.muted));
            chevron.setAlpha(0.72f);
            LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(
                dp(context, 20), dp(context, 20));
            chevronParams.setMargins(dp(context, 8), 0, 0, 0);
            row.addView(chevron, chevronParams);
        }

        row.setOnClickListener(view -> {
            if (!value.enabled) return;
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            listener.onSelected(value.id);
        });
        return row;
    }

    private static View divider(Context context, int color) {
        View divider = new View(context);
        divider.setBackgroundColor(color);
        return divider;
    }

    private static RippleDrawable ripple(int color) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(32,
            Color.red(color), Color.green(color), Color.blue(color))),
            new ColorDrawable(Color.TRANSPARENT), null);
    }

    private static RippleDrawable roundRipple(Context context, int fill, int stroke, int ink) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(context, 22));
        shape.setStroke(dp(context, 1), stroke);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(34,
            Color.red(ink), Color.green(ink), Color.blue(ink))), shape, null);
    }

    private static Button commandButton(Context context, OminalInteractionSheet.Theme theme,
                                        String text, boolean primary) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? theme.onAccent : theme.text);
        button.setIncludeFontPadding(false);
        button.setPadding(dp(context, 18), 0, dp(context, 18), 0);
        button.setBackground(roundRipple(context,
            primary ? theme.accent : Color.TRANSPARENT,
            primary ? theme.accent : theme.border,
            primary ? theme.onAccent : theme.text));
        return button;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
