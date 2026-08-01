package com.ominal.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Native bottom-sheet primitives shared by runtime, model, and command surfaces. */
final class OminalInteractionSheet {
    interface Listener {
        void onSelected(@NonNull String id);
    }

    static final class Theme {
        final int surface;
        final int surfaceRaised;
        final int text;
        final int muted;
        final int border;
        final int accent;
        final int onAccent;

        Theme(int surface, int surfaceRaised, int text, int muted, int border,
              int accent, int onAccent) {
            this.surface = surface;
            this.surfaceRaised = surfaceRaised;
            this.text = text;
            this.muted = muted;
            this.border = border;
            this.accent = accent;
            this.onAccent = onAccent;
        }
    }

    static final class Row {
        @NonNull final String id;
        @NonNull final String title;
        @NonNull final String detail;
        @NonNull final String trailing;
        final boolean selected;
        final boolean enabled;
        final boolean danger;

        Row(@NonNull String id, @NonNull String title) {
            this(id, title, "", "", false, true, false);
        }

        Row(@NonNull String id, @NonNull String title, @Nullable String detail,
            @Nullable String trailing, boolean selected, boolean enabled, boolean danger) {
            this.id = id;
            this.title = title;
            this.detail = detail == null ? "" : detail;
            this.trailing = trailing == null ? "" : trailing;
            this.selected = selected;
            this.enabled = enabled;
            this.danger = danger;
        }
    }

    static final class Section {
        @NonNull final String label;
        @NonNull final List<Row> rows;

        Section(@Nullable String label, @NonNull List<Row> rows) {
            this.label = label == null ? "" : label;
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        }
    }

    private OminalInteractionSheet() {
    }

    static BottomSheetDialog show(@NonNull Activity activity, @NonNull Theme theme,
                                  @NonNull String title, @Nullable String subtitle,
                                  @NonNull List<Section> sections,
                                  @NonNull Listener listener) {
        BottomSheetDialog dialog = createDialog(activity, theme);
        LinearLayout content = createContent(activity, theme, title, subtitle);
        for (Section section : sections) {
            addSection(activity, content, theme, section, dialog, listener);
        }
        addBottomSpace(activity, content);
        dialog.setContentView(content);
        prepareOnShow(dialog, theme);
        dialog.show();
        animateContent(content);
        return dialog;
    }

    static BottomSheetDialog showChoices(@NonNull Activity activity, @NonNull Theme theme,
                                         @NonNull String title, @Nullable String subtitle,
                                         @NonNull List<Row> choices,
                                         @NonNull Listener listener) {
        BottomSheetDialog dialog = createDialog(activity, theme);
        LinearLayout content = createContent(activity, theme, title, subtitle);

        if (choices.size() > 7) {
            EditText search = createSearch(activity, theme);
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48));
            searchParams.setMargins(dp(activity, 20), dp(activity, 4), dp(activity, 20),
                dp(activity, 8));
            content.addView(search, searchParams);

            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            LinearLayout list = new LinearLayout(activity);
            list.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
            int listHeight = Math.min(dp(activity, 480),
                Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.55f));
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(dp(activity, 280), listHeight));
            content.addView(scroll, scrollParams);

            Runnable initialRender = () -> renderFilteredChoices(activity, list, theme,
                choices, search.getText().toString(), dialog, listener);
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderFilteredChoices(activity, list, theme, choices,
                        s == null ? "" : s.toString(), dialog, listener);
                }

                @Override public void afterTextChanged(Editable s) {
                }
            });
            initialRender.run();
        } else {
            addSection(activity, content, theme, new Section("", choices), dialog, listener);
        }

        addBottomSpace(activity, content);
        dialog.setContentView(content);
        prepareOnShow(dialog, theme);
        dialog.show();
        animateContent(content);
        return dialog;
    }

    private static BottomSheetDialog createDialog(Activity activity, Theme theme) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setDismissWithAnimation(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            dialog.getWindow().setDimAmount(0.62f);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        return dialog;
    }

    private static LinearLayout createContent(Context context, Theme theme, String title,
                                              @Nullable String subtitle) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(context, 10), 0, 0);
        content.setBackground(sheetBackground(context, theme.surface));

        View handle = new View(context);
        handle.setBackground(roundRect(context, Color.argb(92, Color.red(theme.muted),
            Color.green(theme.muted), Color.blue(theme.muted)), Color.TRANSPARENT, 2));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
            dp(context, 42), dp(context, 4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, dp(context, 14));
        content.addView(handle, handleParams);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(theme.text);
        titleView.setTextSize(19);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setIncludeFontPadding(false);
        titleView.setPadding(dp(context, 20), 0, dp(context, 20), 0);
        content.addView(titleView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(theme.muted);
            subtitleView.setTextSize(13.5f);
            subtitleView.setLineSpacing(0, 1.08f);
            subtitleView.setIncludeFontPadding(false);
            subtitleView.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), 0);
            content.addView(subtitleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return content;
    }

    private static void addSection(Context context, LinearLayout content, Theme theme,
                                   Section section, BottomSheetDialog dialog,
                                   Listener listener) {
        if (!section.label.isEmpty()) {
            TextView label = new TextView(context);
            label.setText(section.label);
            label.setTextColor(theme.muted);
            label.setTextSize(12);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setIncludeFontPadding(false);
            label.setPadding(dp(context, 20), dp(context, 20), dp(context, 20),
                dp(context, 7));
            content.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            View spacer = new View(context);
            content.addView(spacer, new LinearLayout.LayoutParams(1, dp(context, 12)));
        }

        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        for (int index = 0; index < section.rows.size(); index++) {
            Row row = section.rows.get(index);
            rows.addView(createRow(context, theme, row, dialog, listener),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        content.addView(rows, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private static View createRow(Context context, Theme theme, Row value,
                                  BottomSheetDialog dialog, Listener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 10), dp(context, 8), dp(context, 12), dp(context, 8));
        row.setMinimumHeight(dp(context, value.detail.isEmpty() ? 50 : 58));
        row.setEnabled(value.enabled);
        row.setAlpha(value.enabled ? 1f : 0.45f);
        row.setBackground(ripple(context, theme,
            value.selected ? theme.surfaceRaised : Color.TRANSPARENT, 14));
        row.setContentDescription(value.title
            + (value.detail.isEmpty() ? "" : ", " + value.detail)
            + (value.selected ? ", selected" : ""));

        View selected = new View(context);
        selected.setBackground(roundRect(context,
            value.selected ? theme.accent : Color.TRANSPARENT,
            Color.TRANSPARENT, 2));
        LinearLayout.LayoutParams selectedParams = new LinearLayout.LayoutParams(
            dp(context, 3), dp(context, 26));
        selectedParams.setMargins(0, 0, dp(context, 13), 0);
        row.addView(selected, selectedParams);

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(value.title);
        title.setTextColor(value.danger ? Color.rgb(255, 69, 58) : theme.text);
        title.setTextSize(14.5f);
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
            detail.setTextSize(11.5f);
            detail.setSingleLine(true);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            detail.setIncludeFontPadding(false);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            detailParams.setMargins(0, dp(context, 4), 0, 0);
            labels.addView(detail, detailParams);
        }
        row.addView(labels, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (!value.trailing.isEmpty()) {
            TextView trailing = new TextView(context);
            trailing.setText(value.trailing);
            trailing.setTextColor(value.selected ? theme.text : theme.muted);
            trailing.setTextSize(12.5f);
            trailing.setSingleLine(true);
            trailing.setEllipsize(TextUtils.TruncateAt.END);
            trailing.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            trailing.setIncludeFontPadding(false);
            LinearLayout.LayoutParams trailingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            trailingParams.setMargins(dp(context, 12), 0, 0, 0);
            row.addView(trailing, trailingParams);
        }

        row.setOnClickListener(view -> {
            if (!value.enabled) return;
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
            dialog.dismiss();
            listener.onSelected(value.id);
        });
        return row;
    }

    private static EditText createSearch(Context context, Theme theme) {
        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("Search");
        search.setTextColor(theme.text);
        search.setHintTextColor(theme.muted);
        search.setTextSize(14.5f);
        search.setIncludeFontPadding(false);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        search.setBackground(roundRect(context, theme.surfaceRaised, theme.border, 12));
        return search;
    }

    private static void renderFilteredChoices(Context context, LinearLayout list, Theme theme,
                                              List<Row> choices, String query,
                                              BottomSheetDialog dialog, Listener listener) {
        list.removeAllViews();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Row row : choices) {
            String searchable = (row.title + " " + row.detail + " " + row.trailing)
                .toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && !searchable.contains(normalized)) continue;
            list.addView(createRow(context, theme, row, dialog, listener),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            count++;
        }
        if (count == 0) {
            TextView empty = new TextView(context);
            empty.setText("No matches");
            empty.setTextColor(theme.muted);
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(context, 20), dp(context, 40), dp(context, 20),
                dp(context, 40));
            list.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private static void addBottomSpace(Context context, LinearLayout content) {
        View bottom = new View(context);
        content.addView(bottom, new LinearLayout.LayoutParams(1, dp(context, 24)));
    }

    private static void prepareOnShow(BottomSheetDialog dialog, Theme theme) {
        dialog.setOnShowListener(ignored -> {
            FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup.LayoutParams rawParams = bottomSheet.getLayoutParams();
            rawParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            bottomSheet.setLayoutParams(rawParams);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    private static void animateContent(View content) {
        content.setAlpha(0f);
        content.setTranslationY(dp(content.getContext(), 18));
        content.animate().alpha(1f).translationY(0f).setDuration(260)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f)).start();
    }

    private static Drawable sheetBackground(Context context, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        float radius = dp(context, 20);
        background.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        return background;
    }

    private static Drawable ripple(Context context, Theme theme, int color, int radiusDp) {
        Drawable content = roundRect(context, color, Color.TRANSPARENT, radiusDp);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return content;
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(38,
            Color.red(theme.text), Color.green(theme.text), Color.blue(theme.text))),
            content, null);
    }

    private static GradientDrawable roundRect(Context context, int color, int stroke,
                                              int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (Color.alpha(stroke) > 0) drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
