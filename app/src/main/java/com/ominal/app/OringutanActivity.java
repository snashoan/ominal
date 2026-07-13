package com.ominal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.ominal.R;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.ominal.shared.shell.command.result.ResultData;
import com.ominal.shared.shell.command.runner.app.AppShell;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;
import com.ominal.shared.runtime.settings.preferences.OminalAppSharedPreferences;
import com.ominal.shared.runtime.shell.command.environment.OminalShellEnvironment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;

/**
 * Prototype chatbot front-end for driving a coding agent inside the Ominal execution environment.
 */
public final class OringutanActivity extends AppCompatActivity {

    private static final String LOG_TAG = "OringutanActivity";

    private static final String PREFS_NAME = "ominal_state";
    private static final String PREF_SIGNED_IN = "signed_in";
    private static final String PREF_ACTIVE_SKIN = "active_skin";
    private static final String PREF_ACTIVE_CHAT_ID = "active_chat_id";
    private static final String CHAT_ROOT_NAME = ".ominal/chats";
    private static final String UI_CONFIG_FILE_NAME = ".ominal/ui.properties";
    private static final String UI_RC_FILE_NAME = ".ominalrc";
    private static final String ATTACHMENTS_DIR_NAME = "attachments";
    private static final String DISPLAY_DIR_NAME = "display";
    private static final String DISPLAY_URL = "http://127.0.0.1:6080/vnc_lite.html?autoconnect=true&reconnect=true&path=websockify&resize=scale&view_only=false&show_dot=false&quality=6&compression=6";
    private static final String DISPLAY_START_COMMAND = "command -v ominal-display-start >/dev/null 2>&1 && ominal-display-start || printf 'Install $PREFIX/bin/ominal-display-start first.\\n'";
    private static final int DISPLAY_HEALTH_RETRIES = 30;
    private static final int DISPLAY_HEALTH_RETRY_DELAY_MS = 300;
    private static final String CODEX_LOGIN_TERMINAL_NAME = "ominal-codex-login";
    private static final String NODE_VERSION = "24.15.0";
    private static final String CODEX_VERSION = "0.144.1";
    private static final String PROOT_ASSET = "runtime/archives/proot-android-aarch64.tgz";
    private static final String ROOTFS_ASSET = "runtime/archives/ubuntu-base-24.04.4-arm64-nohardlinks.tgz";
    private static final String PROOT_SHA256 = "9629eb30cdf86e95c6ba681f8ab89c6fdaa9eca093d5577163513c99af5ca281";
    private static final String ROOTFS_SHA256 = "8ae01fcddd133998b050e90119bee3a772b7a28bb50d700f8acf3d95ddb27d7e";
    private static final String NODE_SHA256 = "73afc234d558c24919875f51c2d1ea002a2ada4ea6f83601a383869fefa64eed";
    private static final String CODEX_CORE_SHA256 = "5490b3973605d5f6d9d11680e01513c66732d4bb268f8114055b73c64f91c098";
    private static final String CODEX_ARM64_SHA256 = "25c66d4451c4f57df6b427173ed7d3d7e29c129d61dc58498dbdb946787b7655";
    private static final String NODE_URL = "https://nodejs.org/dist/v" + NODE_VERSION
        + "/node-v" + NODE_VERSION + "-linux-arm64.tar.gz";
    private static final String CODEX_CORE_URL = "https://registry.npmjs.org/@openai/codex/-/codex-"
        + CODEX_VERSION + ".tgz";
    private static final String CODEX_ARM64_URL = "https://registry.npmjs.org/@openai/codex/-/codex-"
        + CODEX_VERSION + "-linux-arm64.tgz";
    private static final long MIN_RUNTIME_FREE_BYTES = 2L * 1024L * 1024L * 1024L;
    private static final String DISPLAY_USER_INPUT_MARKER = "OMINAL_NEEDS_USER_INPUT";
    private static final String DISPLAY_OPEN_MARKER = "OMINAL_OPEN_DISPLAY";
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    private static final String GPTMOBILE_PACKAGE = "dev.chungjungsoo.gptmobile";
    private static final String[] CHAT_SHELL_PACKAGES = new String[]{
        CHATGPT_PACKAGE,
        GPTMOBILE_PACKAGE
    };
    private static final String CHATGPT_STORE_URI = "market://details?id=" + CHATGPT_PACKAGE;
    private static final String CHATGPT_WEB_URI = "https://play.google.com/store/apps/details?id=" + CHATGPT_PACKAGE;
    private static final String HISTORY_FILE_NAME = "history.jsonl";
    private static final String META_FILE_NAME = "meta.json";
    private static final int REQUEST_ATTACH_FILE = 1001;
    private static final int MODE_CHAT = 0;
    private static final int MODE_TERMINAL = 1;
    private static final int MODE_DISPLAY = 2;

    private static final int COLOR_INK = Color.rgb(25, 28, 27);
    private static final int COLOR_MUTED = Color.rgb(87, 96, 93);
    private static final int COLOR_CANVAS = Color.rgb(8, 8, 9);
    private static final int COLOR_PANEL = Color.rgb(16, 17, 18);
    private static final int COLOR_ACCENT = Color.rgb(42, 42, 44);
    private static final int COLOR_ACCENT_DARK = Color.rgb(20, 20, 21);
    private static final int COLOR_BORDER = Color.rgb(43, 44, 46);
    private static final int COLOR_GLASS = Color.argb(232, 21, 22, 24);
    private static final int COLOR_INPUT_GLASS = Color.rgb(30, 31, 33);

    private static final BrandSkin[] BRAND_SKINS = new BrandSkin[]{
        new BrandSkin("codex", "Codex", "Codex", "Ask, build, ship.",
            COLOR_CANVAS, COLOR_PANEL, Color.rgb(244, 245, 247),
            Color.rgb(150, 153, 158), Color.rgb(46, 47, 49), Color.rgb(28, 28, 30),
            Color.rgb(38, 39, 41), Color.rgb(6, 6, 7), Color.rgb(244, 245, 247)),
        new BrandSkin("local", "Local agent", "Local", "Run a local executable agent.",
            COLOR_CANVAS, COLOR_PANEL, Color.rgb(244, 245, 247),
            Color.rgb(150, 153, 158), Color.rgb(52, 53, 55), Color.rgb(28, 28, 30),
            Color.rgb(38, 39, 41), Color.rgb(6, 6, 7), Color.rgb(244, 245, 247)),
        new BrandSkin("ssh", "SSH agent", "SSH", "Use a remote shell-backed agent.",
            COLOR_CANVAS, COLOR_PANEL, Color.rgb(244, 245, 247),
            Color.rgb(150, 153, 158), Color.rgb(52, 53, 55), Color.rgb(28, 28, 30),
            Color.rgb(38, 39, 41), Color.rgb(6, 6, 7), Color.rgb(244, 245, 247)),
        new BrandSkin("custom", "Custom", "Custom", "Bring your own adapter.",
            COLOR_CANVAS, COLOR_PANEL, Color.rgb(244, 245, 247),
            Color.rgb(150, 153, 158), Color.rgb(52, 53, 55), Color.rgb(28, 28, 30),
            Color.rgb(38, 39, 41), Color.rgb(6, 6, 7), Color.rgb(244, 245, 247))
    };

    private static final String CODEX_ADAPTER_SCRIPT =
        "PREFIX=\"${PREFIX:-/data/data/com.ominal/files/usr}\"\n" +
        "HOME=\"${HOME:-/data/data/com.ominal/files/home}\"\n" +
        "PATH=\"$PREFIX/bin:/system/bin:${PATH:-}\"\n" +
        "export PREFIX HOME PATH\n" +
        "prompt=\"$1\"\n" +
        "workdir=\"${OMINAL_WORKDIR:-$PWD}\"\n" +
        "mkdir -p \"$workdir\" 2>/dev/null || true\n" +
        "cd \"$workdir\" 2>/dev/null || cd \"$HOME\"\n" +
        "export OMINAL_WORKDIR=\"$workdir\"\n" +
        "if [ -n \"$ORINGUTAN_AGENT_COMMAND\" ]; then\n" +
        "  printf '%s\\n' \"$prompt\" | sh -lc \"$ORINGUTAN_AGENT_COMMAND\"\n" +
        "elif command -v ominal-codex >/dev/null 2>&1; then\n" +
        "  ominal-codex \"$prompt\" </dev/null\n" +
        "else\n" +
        "  printf 'Ominal provider commands are not installed yet. Restart Ominal or run ominal-codex-setup in the terminal.\\n'\n" +
        "fi";

    private final ArrayList<ChatSession> mSessions = new ArrayList<>();
    private final SimpleDateFormat mClockFormat = new SimpleDateFormat("HH:mm", Locale.US);

    private DrawerLayout mDrawerLayout;
    private LinearLayout mChatDrawerList;
    private View mChatDrawer;
    private EditText mChatSearchInput;
    private LinearLayout mMessagesView;
    private LinearLayout mModeBar;
    private FrameLayout mRootFrame;
    private View mHeaderView;
    private View mComposerView;
    private FrameLayout mContentFrame;
    private ScrollView mScrollView;
    private EditText mPromptInput;
    private Button mAttachButton;
    private Button mSendButton;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mStatusView;
    private WebView mDisplayWebView;
    private View mDisplayPane;
    private View mDisplayHomeOverlay;
    private FrameLayout mDisplayWarmHost;
    private TextView mDisplayAvailabilityView;
    private Button mChatModeButton;
    private Button mTerminalModeButton;
    private Button mDisplayModeButton;
    private Button mHeaderDisplayButton;
    private Button mDisplayCloseButton;
    private Button mSwapButton;
    private Button mProviderButton;
    private Button mTerminalToolButton;
    private Button mDisplayToolButton;

    private SharedPreferences mPrefs;
    private ChatSession mActiveSession;
    private BrandSkin mSkin = BRAND_SKINS[0];
    private UiSpec mUi = UiSpec.defaults(BRAND_SKINS[0]);
    private boolean mBootstrapReady;
    private boolean mRuntimeReady;
    private boolean mRuntimeSetupInFlight;
    private String mRuntimeSetupDetail = "";
    private boolean mPromptRunning;
    private boolean mDisplayStartInFlight;
    private boolean mReloadDisplayWhenReady;
    private boolean mDisplayReady;
    private boolean mDisplayUrlLoaded;
    private boolean mDisplayHomeVisible = true;
    private boolean mPendingCodexDeviceLogin;
    private boolean mCodexAccountDialogVisible;
    private boolean mCodexAuthRefreshInFlight;
    private int mDisplayRetryCount;
    private long mDisplayLastStartedAt;
    private String mDisplayStartupDetail = "";
    private String mChatSearchQuery = "";
    private boolean mSplitReversed;
    private float mSplitRatio = 0.52f;
    private int mMode = MODE_CHAT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mSkin = findSkin(mPrefs.getString(PREF_ACTIVE_SKIN, BRAND_SKINS[0].id));
        mUi = loadUiSpec();
        applySystemBars();
        if (!mPrefs.getBoolean(PREF_SIGNED_IN, false)) {
            setContentView(createLoginView());
            return;
        }

        startWorkspace();
    }

    private void startWorkspace() {
        setContentView(createContentView());

        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(this, true);
        if (preferences == null) {
            setInputEnabled(false);
            setStatus("Ominal preferences unavailable");
            addTransientSystemMessage("Ominal preferences could not be loaded. Check package identity and app data.");
            return;
        }

        setStatus("Preparing executable area");
        setInputEnabled(false);
        OminalInstaller.setupBootstrapIfNeeded(this, () -> {
            mBootstrapReady = true;
            refreshRuntimeDns();
            ensureDefaultUiProperties();
            ensureProviderCommands();
            mUi = loadUiSpec();
            loadOrCreateSessions();
            setStatus("Preparing Linux runtime");
            ensureRuntimeReady(() -> {
                mRuntimeReady = true;
                setInputEnabled(true);
                setStatus("Ready");
                ensureDisplayServerStarted(false);
                if (mRootFrame != null) mRootFrame.postDelayed(this::prewarmDisplaySurface, 1200);
                if (mPendingCodexDeviceLogin) {
                    mPendingCodexDeviceLogin = false;
                    if (mRootFrame != null) mRootFrame.postDelayed(this::startCodexDeviceLogin, 260);
                    else startCodexDeviceLogin();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBootstrapReady) {
            refreshRuntimeDns();
            refreshCodexAuthStatus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ATTACH_FILE && resultCode == Activity.RESULT_OK && data != null)
            handleAttachmentResult(data);
    }

    private View createLoginView() {
        BrandSkin skin = skin();
        UiSpec ui = ui();
        int panelWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(480));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(ui.app.fill);

        View topSpace = new View(this);
        root.addView(topSpace, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        View mark = new BrandMarkView(this, skin, false);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        markParams.setMargins(0, 0, 0, dp(22));
        root.addView(mark, markParams);

        TextView title = new TextView(this);
        title.setText(skin.name);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(ui.ink);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(32);
        root.addView(title, new LinearLayout.LayoutParams(panelWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText(skin.tagline);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(ui.muted);
        subtitle.setTextSize(15);
        subtitle.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(panelWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, dp(10), 0, dp(22));
        root.addView(subtitle, subtitleParams);

        View switcher = createProviderDropdown(true);
        LinearLayout.LayoutParams switcherParams = new LinearLayout.LayoutParams(panelWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        switcherParams.setMargins(0, 0, 0, dp(26));
        root.addView(switcher, switcherParams);

        Button signIn = createAccentButton("Sign in with ChatGPT");
        signIn.setOnClickListener(v -> {
            mPendingCodexDeviceLogin = true;
            completeLogin();
        });
        LinearLayout.LayoutParams signInParams = new LinearLayout.LayoutParams(panelWidth, dp(50));
        signInParams.setMargins(0, 0, 0, dp(10));
        root.addView(signIn, signInParams);

        Button continueWithoutSignIn = createSecondaryButton("Continue without signing in");
        continueWithoutSignIn.setOnClickListener(v -> completeLogin());
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(panelWidth, dp(46));
        continueParams.setMargins(0, 0, 0, dp(18));
        root.addView(continueWithoutSignIn, continueParams);

        View bottomSpace = new View(this);
        root.addView(bottomSpace, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        return root;
    }

    private void completeLogin() {
        mPrefs.edit()
            .putBoolean(PREF_SIGNED_IN, true)
            .putString(PREF_ACTIVE_SKIN, skin().id)
            .apply();
        startWorkspace();
    }

    private void showCodexAccountDialog() {
        if (mCodexAccountDialogVisible) return;
        mCodexAccountDialogVisible = true;

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Connect Codex")
            .setMessage("Sign in in your browser, then return to Ominal.")
            .setPositiveButton("Sign in with ChatGPT", (ignored, which) -> startCodexDeviceLogin())
            .setNeutralButton("Check status", (ignored, which) -> refreshCodexAuthStatus())
            .setNegativeButton("Cancel", null)
            .create();
        dialog.setOnDismissListener(ignored -> mCodexAccountDialogVisible = false);
        dialog.show();
    }

    private void startCodexDeviceLogin() {
        if (!mBootstrapReady || !mRuntimeReady || mActiveSession == null) {
            mPendingCodexDeviceLogin = true;
            return;
        }

        ensureProviderCommands();
        refreshRuntimeDns();
        String commandLine = "export PREFIX=" + shellQuote(OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH) + "; "
            + "export HOME=" + shellQuote(OminalConstants.OMINAL_HOME_DIR_PATH) + "; "
            + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\"; "
            + "codex login --device-auth; "
            + "printf '\\nReturn to Ominal after sign-in.\\n'; exec \"$PREFIX/bin/bash\" -i";

        Intent executeIntent = new Intent(OMINAL_SERVICE.ACTION_SERVICE_EXECUTE);
        executeIntent.setClass(this, OminalService.class);
        executeIntent.setData(Uri.fromFile(new File(OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH, "sh")));
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_ARGUMENTS, new String[]{"-lc", commandLine});
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_WORKDIR, mActiveSession.workspacePath);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_RUNNER, ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_NAME, CODEX_LOGIN_TERMINAL_NAME);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE, ShellCreateMode.ALWAYS.getMode());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_COMMAND_LABEL, "Connect Codex");
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION,
            Integer.toString(OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY));
        try {
            startService(executeIntent);
            setStatus("Waiting for sign-in");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to start Codex device login", e);
            addTransientSystemMessage("Could not start Codex sign-in.");
        }
    }

    private void refreshCodexAuthStatus() {
        if (!mBootstrapReady || !mRuntimeReady || mActiveSession == null || mCodexAuthRefreshInFlight) return;
        refreshRuntimeDns();
        mCodexAuthRefreshInFlight = true;

        new Thread(() -> {
            String statusCommand = "PREFIX=" + shellQuote(OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH) + "; "
                + "export PREFIX HOME=" + shellQuote(OminalConstants.OMINAL_HOME_DIR_PATH) + "; "
                + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\"; "
                + "codex login status";
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
                new String[]{"-lc", statusCommand},
                null,
                mActiveSession.workspacePath,
                ExecutionCommand.Runner.APP_SHELL.getName(),
                false);
            command.commandLabel = "Codex account status";
            AppShell.execute(this, command, null, new OminalShellEnvironment(), null, true);
            String output = formatCommandOutput(command).toLowerCase(Locale.ROOT);
            boolean signedIn = !requiresCodexLogin(output)
                && command.resultData.exitCode != null && command.resultData.exitCode == 0;
            runOnUiThread(() -> {
                mCodexAuthRefreshInFlight = false;
                setStatus(signedIn ? "Codex connected" : "Connect Codex");
            });
        }).start();
    }

    private boolean requiresCodexLogin(String output) {
        if (output == null) return true;
        String normalized = output.toLowerCase(Locale.ROOT);
        return normalized.contains("not authenticated")
            || normalized.contains("not signed in")
            || normalized.contains("login required")
            || normalized.contains("sign in once")
            || normalized.contains("auth missing")
            || normalized.contains("no auth")
            || normalized.contains("unauthenticated");
    }

    private View createProviderDropdown(boolean centered) {
        BrandSkin skin = skin();
        UiSpec ui = ui();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setBackgroundColor(ui.app.fill);

        TextView label = new TextView(this);
        label.setText("Agent");
        label.setTextColor(ui.muted);
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            centered ? dp(70) : dp(64), dp(44));
        row.addView(label, labelParams);

        mProviderButton = createSecondaryButton(skin.name + "  v");
        mProviderButton.setGravity(Gravity.CENTER_VERTICAL);
        mProviderButton.setTextSize(15);
        mProviderButton.setOnClickListener(v -> showProviderPicker());
        row.addView(mProviderButton, new LinearLayout.LayoutParams(0, dp(44), 1));

        return row;
    }

    private void showProviderPicker() {
        String[] labels = new String[BRAND_SKINS.length + 3];
        for (int i = 0; i < BRAND_SKINS.length; i++) labels[i] = BRAND_SKINS[i].name;
        labels[BRAND_SKINS.length] = "Connect Codex";
        labels[BRAND_SKINS.length + 1] = "Open ChatGPT app";
        labels[BRAND_SKINS.length + 2] = "Share draft to ChatGPT";

        new AlertDialog.Builder(this)
            .setTitle("Agent")
            .setSingleChoiceItems(labels, indexOfSkin(skin()), (dialog, which) -> {
                dialog.dismiss();
                if (which >= 0 && which < BRAND_SKINS.length) {
                    selectSkin(BRAND_SKINS[which]);
                } else if (which == BRAND_SKINS.length) {
                    showCodexAccountDialog();
                } else if (which == BRAND_SKINS.length + 1) {
                    openChatGptApp();
                } else if (which == BRAND_SKINS.length + 2) {
                    shareDraftToChatGpt();
                }
            })
            .show();
    }

    private void showWorkspaceMenu() {
        String[] labels = new String[]{
            "Agent",
            "Terminal",
            "Display",
            "Reload UI config",
            "Copy workspace command",
            "Open ChatGPT app",
            "Share draft to ChatGPT"
        };

        new AlertDialog.Builder(this)
            .setItems(labels, (dialog, which) -> {
                dialog.dismiss();
                if (which == 0) {
                    showProviderPicker();
                } else if (which == 1) {
                    switchMode(mMode == MODE_TERMINAL ? MODE_CHAT : MODE_TERMINAL);
                } else if (which == 2) {
                    switchMode(mMode == MODE_DISPLAY ? MODE_CHAT : MODE_DISPLAY);
                } else if (which == 3) {
                    reloadUiConfig();
                } else if (which == 4) {
                    if (mActiveSession != null) {
                        copyToClipboard("Ominal workspace", "cd " + shellQuote(mActiveSession.workspacePath));
                        setStatus("Workspace command copied");
                    }
                } else if (which == 5) {
                    openChatGptApp();
                } else if (which == 6) {
                    shareDraftToChatGpt();
                }
            })
            .show();
    }

    private void openChatGptApp() {
        if (startInstalledChatShell()) return;

        Toast.makeText(this, "No chat shell app installed", Toast.LENGTH_SHORT).show();
        Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(CHATGPT_STORE_URI));
        if (!startExternalActivity(storeIntent))
            startExternalActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(CHATGPT_WEB_URI)));
    }

    private void shareDraftToChatGpt() {
        String text = buildChatGptBridgeText();
        if (TextUtils.isEmpty(text)) {
            openChatGptApp();
            return;
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        for (String packageName : CHAT_SHELL_PACKAGES) {
            sendIntent.setPackage(packageName);
            if (sendIntent.resolveActivity(getPackageManager()) != null && startExternalActivity(sendIntent)) return;
        }

        copyToClipboard("Ominal ChatGPT draft", text);
        Toast.makeText(this, "Draft copied; opening chat app", Toast.LENGTH_SHORT).show();
        openChatGptApp();
    }

    private boolean startInstalledChatShell() {
        for (String packageName : CHAT_SHELL_PACKAGES) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null && startExternalActivity(launchIntent))
                return true;
        }
        return false;
    }

    private String buildChatGptBridgeText() {
        if (mPromptInput != null) {
            String draft = mPromptInput.getText().toString().trim();
            if (!draft.isEmpty()) return draft;
        }

        if (mActiveSession == null) return "";
        StringBuilder builder = new StringBuilder();
        builder.append("Ominal chat: ").append(mActiveSession.title).append('\n')
            .append("Workspace: ").append(mActiveSession.workspacePath);
        if (!mActiveSession.messages.isEmpty()) {
            ChatMessage last = mActiveSession.messages.get(mActiveSession.messages.size() - 1);
            builder.append("\n\nLatest message:\n").append(last.text);
        }
        return builder.toString();
    }

    private boolean startExternalActivity(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to start external activity", e);
            return false;
        }
    }

    private void selectSkin(BrandSkin nextSkin) {
        if (nextSkin == null || nextSkin == mSkin) return;
        mSkin = nextSkin;
        mUi = loadUiSpec();
        applySystemBars();
        mPrefs.edit().putString(PREF_ACTIVE_SKIN, nextSkin.id).apply();
        if (mContentFrame == null) {
            setContentView(createLoginView());
            return;
        }

        setContentView(createContentView());
        if (mActiveSession != null) {
            renderChatDrawer();
            renderHeader();
            renderMode();
        }
        setInputEnabled(mBootstrapReady);
    }

    private void reloadUiConfig() {
        mUi = loadUiSpec();
        applySystemBars();
        if (mContentFrame == null) {
            setContentView(createLoginView());
            return;
        }

        setContentView(createContentView());
        if (mActiveSession != null) {
            renderChatDrawer();
            renderHeader();
            renderMode();
        }
        setInputEnabled(mBootstrapReady);
        setStatus("UI config reloaded");
    }

    private UiSpec loadUiSpec() {
        Properties properties = new Properties();
        loadUiProperties(properties, uiConfigFile());
        loadUiProperties(properties, uiRcFile());
        return UiSpec.fromProperties(skin(), properties);
    }

    private void loadUiProperties(Properties properties, File file) {
        if (file == null || !file.isFile()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                readUiConfigLine(properties, line);
            }
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read UI config: " + file.getAbsolutePath(), e);
        }
    }

    private void readUiConfigLine(Properties properties, String line) {
        if (line == null) return;
        String text = line.trim();
        if (text.isEmpty() || text.startsWith("#")) return;
        if (text.startsWith("export ")) text = text.substring("export ".length()).trim();
        int equals = text.indexOf('=');
        if (equals <= 0) return;

        String key = text.substring(0, equals).trim();
        String value = text.substring(equals + 1).trim();
        if (key.isEmpty()) return;
        properties.setProperty(key, stripUiConfigQuotes(value));
    }

    private String stripUiConfigQuotes(String value) {
        if (value == null || value.length() < 2) return value;
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\''))
            return value.substring(1, value.length() - 1);
        return value;
    }

    private File uiConfigFile() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH, UI_CONFIG_FILE_NAME);
    }

    private File uiRcFile() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH, UI_RC_FILE_NAME);
    }

    private UiSpec ui() {
        return mUi != null ? mUi : UiSpec.defaults(skin());
    }

    private void ensureDefaultUiProperties() {
        File file = uiConfigFile();
        if (file.isFile()) return;
        try {
            writeFile(file, defaultUiPropertiesTemplate());
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to write default UI config", e);
        }
    }

    private void ensureProviderCommands() {
        try {
            File binDir = new File(OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH);
            ensureDirectory(binDir.getAbsolutePath());
            extractRuntimeTool("runtime/ominal-proot-run.sh", new File(binDir, "ominal-proot-run"));
            extractRuntimeTool("runtime/ominal-codex-setup.sh", new File(binDir, "ominal-codex-setup"));
            extractRuntimeTool("runtime/ominal-proot-codex.sh", new File(binDir, "ominal-proot-codex"));
            extractRuntimeTool("runtime/ominal-proot-install-local-codex.sh",
                new File(binDir, "ominal-proot-install-local-codex"));
            extractRuntimeTool("runtime/ominal-runtime-install-proot.sh",
                new File(binDir, "ominal-runtime-install-proot"));
            extractRuntimeTool("runtime/ominal-runtime-install-ubuntu-base.sh",
                new File(binDir, "ominal-runtime-install-ubuntu-base"));
            extractRuntimeTool("runtime/ominal-install-display-packages.sh",
                new File(binDir, "ominal-install-display-packages"));
            extractRuntimeTool("runtime/ominal-runtime-bootstrap.sh",
                new File(binDir, "ominal-runtime-bootstrap"));
            extractRuntimeTool("runtime/ominal-display-start.sh", new File(binDir, "ominal-display-start"));
            writeExecutableFile(new File(binDir, "ominal-codex"),
                "#!/data/data/com.ominal/files/usr/bin/sh\n"
                    + "PREFIX=\"${PREFIX:-/data/data/com.ominal/files/usr}\"\n"
                    + "exec \"$PREFIX/bin/ominal-proot-codex\" exec --skip-git-repo-check -- \"$@\"\n");
            writeExecutableFile(new File(binDir, "codex"),
                "#!/data/data/com.ominal/files/usr/bin/sh\n"
                    + "PREFIX=\"${PREFIX:-/data/data/com.ominal/files/usr}\"\n"
                    + "exec \"$PREFIX/bin/ominal-proot-codex\" \"$@\"\n");

            File legacyNativeProvider = new File(binDir, "codex.real");
            if (legacyNativeProvider.exists() && !legacyNativeProvider.delete())
                Logger.logWarn(LOG_TAG, "Could not remove retired native Codex provider");
            File legacyArm64Provider = new File(binDir, "codex-aarch64");
            if (legacyArm64Provider.exists() && !legacyArm64Provider.delete())
                Logger.logWarn(LOG_TAG, "Could not remove retired arm64 Codex provider");
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to install Ominal PRoot commands", e);
            addTransientSystemMessage("Codex provider setup commands could not be installed.");
        }
    }

    private void extractRuntimeTool(String assetPath, File target) throws IOException {
        try (InputStream input = getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        }
        if (!target.setReadable(true, true) || !target.setExecutable(true, true))
            throw new IOException("Could not make runtime tool executable: " + target.getName());
    }

    private void ensureRuntimeReady(Runnable whenReady) {
        if (mRuntimeReady) {
            whenReady.run();
            return;
        }
        if (mRuntimeSetupInFlight) return;
        mRuntimeSetupInFlight = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        new Thread(() -> {
            try {
                if (!supportsArm64())
                    throw new IOException("Ominal's Linux runtime currently requires an arm64-v8a device.");

                String bootstrap = OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-runtime-bootstrap";
                if (runRuntimeShell(shellQuote(bootstrap) + " --verify", "Verify Ominal runtime")) {
                    finishRuntimeSetup(whenReady);
                    return;
                }

                File runtimeRoot = new File(OminalConstants.OMINAL_HOME_DIR_PATH, ".ominal/runtime");
                ensureDirectory(runtimeRoot.getAbsolutePath());
                long usableBytes = runtimeRoot.getUsableSpace();
                if (usableBytes > 0 && usableBytes < MIN_RUNTIME_FREE_BYTES)
                    throw new IOException("At least 2 GB of free storage is required to prepare the Linux runtime.");

                File downloads = new File(runtimeRoot, "downloads");
                ensureDirectory(downloads.getAbsolutePath());
                File proot = new File(downloads, "proot-android-aarch64.tar.gz");
                File rootfs = new File(downloads, "ubuntu-base-24.04.4-arm64-nohardlinks.tar.gz");
                File node = new File(downloads, "node-v" + NODE_VERSION + "-linux-arm64.tar.gz");
                File codexCore = new File(downloads, "codex-" + CODEX_VERSION + ".tgz");
                File codexArm64 = new File(downloads, "codex-" + CODEX_VERSION + "-linux-arm64.tgz");

                updateRuntimeStatus("Preparing Linux base");
                copyRuntimeAsset(PROOT_ASSET, proot, PROOT_SHA256);
                copyRuntimeAsset(ROOTFS_ASSET, rootfs, ROOTFS_SHA256);
                downloadRuntimeArtifact(NODE_URL, node, NODE_SHA256, "Downloading Node");
                downloadRuntimeArtifact(CODEX_CORE_URL, codexCore, CODEX_CORE_SHA256, "Downloading Codex");
                downloadRuntimeArtifact(CODEX_ARM64_URL, codexArm64, CODEX_ARM64_SHA256,
                    "Downloading Codex runtime");

                updateRuntimeStatus("Installing Linux workspace");
                String installCommand = shellQuote(bootstrap) + " "
                    + shellQuote(proot.getAbsolutePath()) + " "
                    + shellQuote(rootfs.getAbsolutePath()) + " "
                    + shellQuote(node.getAbsolutePath()) + " "
                    + shellQuote(codexCore.getAbsolutePath()) + " "
                    + shellQuote(codexArm64.getAbsolutePath());
                if (!runRuntimeShell(installCommand, "Install Ominal runtime"))
                    throw new IOException(mRuntimeSetupDetail.isEmpty()
                        ? "The Linux runtime installer did not complete." : mRuntimeSetupDetail);

                deleteQuietly(proot);
                deleteQuietly(rootfs);
                deleteQuietly(node);
                deleteQuietly(codexCore);
                deleteQuietly(codexArm64);
                finishRuntimeSetup(whenReady);
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to prepare Ominal runtime", e);
                runOnUiThread(() -> {
                    mRuntimeSetupInFlight = false;
                    mRuntimeReady = false;
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    setInputEnabled(false);
                    setStatus("Runtime setup failed");
                    String detail = e.getMessage();
                    addTransientSystemMessage(detail == null || detail.trim().isEmpty()
                        ? "Linux runtime setup failed. Restart Ominal to retry."
                        : detail.trim() + " Restart Ominal to retry.");
                });
            }
        }, "ominal-runtime-setup").start();
    }

    private void finishRuntimeSetup(Runnable whenReady) {
        runOnUiThread(() -> {
            mRuntimeSetupInFlight = false;
            mRuntimeReady = true;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            refreshRuntimeDns();
            whenReady.run();
        });
    }

    private boolean supportsArm64() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }

    private boolean runRuntimeShell(String commandLine, String label) {
        ExecutionCommand command = new ExecutionCommand(-1,
            OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
            new String[]{"-lc", commandLine},
            null,
            OminalConstants.OMINAL_HOME_DIR_PATH,
            ExecutionCommand.Runner.APP_SHELL.getName(),
            false);
        command.commandLabel = label;
        AppShell.execute(this, command, null, new OminalShellEnvironment(), null, true);
        mRuntimeSetupDetail = formatCommandOutput(command);
        Integer exitCode = command.resultData.exitCode;
        return !command.isStateFailed() && exitCode != null && exitCode == 0;
    }

    private void copyRuntimeAsset(String assetPath, File target, String expectedSha256) throws IOException {
        if (target.isFile() && hasSha256(target, expectedSha256)) return;
        File partial = new File(target.getAbsolutePath() + ".part");
        deleteQuietly(partial);
        ensureDirectory(target.getParent());
        try (InputStream input = getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(partial)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        }
        if (!hasSha256(partial, expectedSha256)) {
            deleteQuietly(partial);
            throw new IOException("Bundled runtime asset failed verification: " + target.getName());
        }
        replaceFile(partial, target);
    }

    private void downloadRuntimeArtifact(String sourceUrl, File target, String expectedSha256, String label)
        throws IOException {
        if (target.isFile() && hasSha256(target, expectedSha256)) return;
        if (target.exists() && !target.delete())
            throw new IOException("Could not replace invalid runtime download: " + target.getName());

        File partial = new File(target.getAbsolutePath() + ".part");
        ensureDirectory(target.getParent());
        long resumeAt = partial.isFile() ? partial.length() : 0L;
        URL url = new URL(sourceUrl);
        ConnectivityManager connectivityManager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager == null ? null : connectivityManager.getActiveNetwork();
        HttpURLConnection connection = (HttpURLConnection) (activeNetwork == null
            ? url.openConnection() : activeNetwork.openConnection(url));
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(120_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept-Encoding", "identity");
        if (resumeAt > 0) connection.setRequestProperty("Range", "bytes=" + resumeAt + "-");

        try {
            int responseCode = connection.getResponseCode();
            boolean append = resumeAt > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL;
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL)
                throw new IOException("Download failed with HTTP " + responseCode + ": " + target.getName());
            if (!append) resumeAt = 0L;

            long responseBytes = connection.getContentLengthLong();
            long totalBytes = responseBytes > 0 ? resumeAt + responseBytes : -1L;
            long completed = resumeAt;
            int lastProgressBucket = -1;
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(partial, append)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    completed += count;
                    if (totalBytes > 0) {
                        int percent = (int) Math.min(100L, completed * 100L / totalBytes);
                        int bucket = percent / 5;
                        if (bucket != lastProgressBucket) {
                            lastProgressBucket = bucket;
                            updateRuntimeStatus(label + " " + percent + "%");
                        }
                    }
                }
                output.getFD().sync();
            }
        } finally {
            connection.disconnect();
        }

        if (!hasSha256(partial, expectedSha256)) {
            deleteQuietly(partial);
            throw new IOException("Downloaded runtime artifact failed verification: " + target.getName());
        }
        replaceFile(partial, target);
    }

    private boolean hasSha256(File file, String expectedSha256) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder actual = new StringBuilder(64);
        for (byte value : digest.digest()) actual.append(String.format(Locale.US, "%02x", value & 0xff));
        return actual.toString().equals(expectedSha256);
    }

    private void replaceFile(File source, File target) throws IOException {
        if (target.exists() && !target.delete())
            throw new IOException("Could not replace runtime artifact: " + target.getName());
        if (!source.renameTo(target))
            throw new IOException("Could not finalize runtime artifact: " + target.getName());
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete())
            Logger.logWarn(LOG_TAG, "Could not remove runtime artifact " + file.getAbsolutePath());
    }

    private void updateRuntimeStatus(String status) {
        runOnUiThread(() -> setStatus(status));
    }

    private void refreshRuntimeDns() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) return;

            LinkedHashSet<String> ipv4DnsServers = new LinkedHashSet<>();
            LinkedHashSet<String> ipv6DnsServers = new LinkedHashSet<>();
            Network activeNetwork = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null)
                    addRuntimeDnsServers(connectivityManager.getLinkProperties(activeNetwork), ipv4DnsServers, ipv6DnsServers);
            }

            Network[] networks = connectivityManager.getAllNetworks();
            if (networks != null) {
                for (Network network : networks) {
                    if (network != null && !network.equals(activeNetwork))
                        addRuntimeDnsServers(connectivityManager.getLinkProperties(network), ipv4DnsServers, ipv6DnsServers);
                }
            }
            LinkedHashSet<String> dnsServers = new LinkedHashSet<>();
            dnsServers.addAll(ipv4DnsServers);
            dnsServers.addAll(ipv6DnsServers);
            if (dnsServers.isEmpty()) return;

            File rootfs = new File(OminalConstants.OMINAL_HOME_DIR_PATH, ".ominal/runtime/linux/rootfs");
            File runtimeReady = new File(rootfs, ".ominal-rootfs-ready");
            File etcDirectory = new File(rootfs, "etc");
            if (!runtimeReady.isFile() || !etcDirectory.isDirectory()) return;

            StringBuilder resolver = new StringBuilder();
            for (String dnsServer : dnsServers)
                resolver.append("nameserver ").append(dnsServer).append('\n');
            writeFile(new File(etcDirectory, "resolv.conf"), resolver.toString());
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to refresh Ominal runtime DNS", e);
        }
    }

    private static void addRuntimeDnsServers(LinkProperties properties, LinkedHashSet<String> ipv4DnsServers,
        LinkedHashSet<String> ipv6DnsServers) {
        if (properties == null) return;
        for (java.net.InetAddress address : properties.getDnsServers()) {
            String value = address.getHostAddress();
            int scope = value.indexOf('%');
            if (scope >= 0) value = value.substring(0, scope);
            if (value.isEmpty()) continue;
            if (value.indexOf(':') >= 0) ipv6DnsServers.add(value);
            else ipv4DnsServers.add(value);
        }
    }

    private String defaultUiPropertiesTemplate() {
        UiSpec spec = UiSpec.defaults(skin());
        StringBuilder builder = new StringBuilder();
        builder.append("# Ominal UI properties\n");
        builder.append("# Edit like .bashrc: one key=value per line, comments start with #.\n");
        builder.append("# 'export key=value' also works, but the app only reads these UI keys.\n");
        builder.append("# Use More > Reload UI config after saving, or restart Ominal.\n");
        builder.append("# Colors accept #RRGGBB or #AARRGGBB.\n\n");
        appendColor(builder, "color.canvas", spec.canvas);
        appendColor(builder, "color.panel", spec.panel);
        appendColor(builder, "color.panelSoft", spec.panelSoft);
        appendColor(builder, "color.ink", spec.ink);
        appendColor(builder, "color.muted", spec.muted);
        appendColor(builder, "color.accent", spec.accent);
        appendColor(builder, "color.accentDark", spec.accentDark);
        appendColor(builder, "color.border", spec.border);
        appendColor(builder, "color.dark", spec.dark);
        appendColor(builder, "color.onDark", spec.onDark);
        appendColor(builder, "color.onDarkMuted", spec.onDarkMuted);
        builder.append('\n');
        appendSurface(builder, "surface.app", spec.app);
        appendSurface(builder, "surface.header", spec.header);
        appendSurface(builder, "surface.toolbarButton", spec.toolbarButton);
        appendSurface(builder, "surface.toolbarButtonActive", spec.toolbarButtonActive);
        appendSurface(builder, "surface.drawer", spec.drawer);
        appendSurface(builder, "surface.drawerSearch", spec.drawerSearch);
        appendSurface(builder, "surface.drawerRow", spec.drawerRow);
        appendSurface(builder, "surface.drawerRowActive", spec.drawerRowActive);
        appendSurface(builder, "surface.chat", spec.chat);
        appendSurface(builder, "surface.bubble.user", spec.bubbleUser);
        appendSurface(builder, "surface.bubble.agent", spec.bubbleAgent);
        appendSurface(builder, "surface.composer", spec.composer);
        appendSurface(builder, "surface.composerInput", spec.composerInput);
        appendSurface(builder, "surface.composerIcon", spec.composerIcon);
        appendSurface(builder, "surface.composerSend", spec.composerSend);
        appendSurface(builder, "surface.buttonPrimary", spec.buttonPrimary);
        appendSurface(builder, "surface.buttonSecondary", spec.buttonSecondary);
        appendSurface(builder, "surface.modeButton", spec.modeButton);
        appendSurface(builder, "surface.modeButtonActive", spec.modeButtonActive);
        appendSurface(builder, "surface.terminalBlock", spec.terminalBlock);
        appendSurface(builder, "surface.displayHome", spec.displayHome);
        appendSurface(builder, "surface.displayTile", spec.displayTile);
        return builder.toString();
    }

    private void appendColor(StringBuilder builder, String key, int color) {
        builder.append(key).append('=').append(colorHex(color)).append('\n');
    }

    private void appendSurface(StringBuilder builder, String key, SurfaceSpec surface) {
        builder.append(key).append(".fill=").append(colorHex(surface.fill)).append('\n');
        builder.append(key).append(".stroke=").append(colorHex(surface.stroke)).append('\n');
        builder.append(key).append(".text=").append(colorHex(surface.text)).append('\n');
        builder.append(key).append(".radius=").append(surface.radiusDp).append('\n');
        builder.append('\n');
    }

    private String colorHex(int color) {
        if (Color.alpha(color) == 255) {
            return String.format(Locale.US, "#%02X%02X%02X",
                Color.red(color), Color.green(color), Color.blue(color));
        }
        return String.format(Locale.US, "#%02X%02X%02X%02X",
            Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
    }

    private View createContentView() {
        UiSpec ui = ui();
        mRootFrame = new FrameLayout(this);
        mRootFrame.setBackgroundColor(ui.app.fill);

        mDrawerLayout = new DrawerLayout(this);
        mDrawerLayout.setScrimColor(Color.argb(172, 0, 0, 0));
        mDrawerLayout.setDrawerElevation(dp(10));
        mDrawerLayout.setBackgroundColor(ui.app.fill);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.app.fill);

        mHeaderView = createHeader();
        root.addView(mHeaderView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        mChatDrawer = createChatDrawer();

        mModeBar = null;
        mChatModeButton = null;
        mTerminalModeButton = null;
        mDisplayModeButton = null;
        mSwapButton = null;
        mDisplayCloseButton = null;
        mDisplayPane = null;
        mDisplayHomeOverlay = null;
        mDisplayWebView = null;
        mDisplayUrlLoaded = false;
        mDisplayHomeVisible = true;

        mContentFrame = new FrameLayout(this);
        root.addView(mContentFrame, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mComposerView = createComposer();
        root.addView(mComposerView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        renderMode();
        mDrawerLayout.addView(root, new DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT));

        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(
            getDrawerWidth(), DrawerLayout.LayoutParams.MATCH_PARENT);
        drawerParams.gravity = Gravity.START;
        mDrawerLayout.addView(mChatDrawer, drawerParams);

        mDisplayWarmHost = new FrameLayout(this);
        mDisplayWarmHost.setAlpha(0f);
        mDisplayWarmHost.setClickable(false);
        mDisplayWarmHost.setFocusable(false);
        mDisplayWarmHost.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        FrameLayout.LayoutParams warmParams = new FrameLayout.LayoutParams(dp(1), dp(1));
        warmParams.gravity = Gravity.BOTTOM | Gravity.START;
        mRootFrame.addView(mDisplayWarmHost, warmParams);

        mRootFrame.addView(mDrawerLayout, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return mRootFrame;
    }

    private int getDrawerWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.max(dp(288), Math.min(dp(380), screenWidth - dp(48)));
    }

    private void applySystemBars() {
        UiSpec ui = ui();
        getWindow().setStatusBarColor(ui.header.fill);
        getWindow().setNavigationBarColor(Color.BLACK);
    }

    private View createHeader() {
        UiSpec ui = ui();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(14), dp(8), dp(14), dp(6));
        header.setBackgroundColor(ui.header.fill);

        Button chatsButton = createToolbarIconButton("☰");
        chatsButton.setContentDescription("Chat history");
        chatsButton.setOnClickListener(v -> showChatPicker());
        header.addView(chatsButton, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        titleStack.setGravity(Gravity.CENTER_VERTICAL);
        titleStack.setPadding(dp(12), 0, dp(8), 0);

        mTitleView = new TextView(this);
        mTitleView.setText("Ominal");
        mTitleView.setTextColor(ui.header.text);
        mTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        mTitleView.setTextSize(17);
        mTitleView.setSingleLine(true);
        mTitleView.setEllipsize(TextUtils.TruncateAt.END);
        mTitleView.setIncludeFontPadding(false);
        mTitleView.setContentDescription("Current chat");
        titleStack.addView(mTitleView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(22)));

        mSubtitleView = new TextView(this);
        mSubtitleView.setText("Workspace ready");
        mSubtitleView.setTextColor(ui.onDarkMuted);
        mSubtitleView.setSingleLine(true);
        mSubtitleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        mSubtitleView.setTextSize(12);
        mSubtitleView.setIncludeFontPadding(false);
        titleStack.addView(mSubtitleView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(18)));

        header.addView(titleStack, new LinearLayout.LayoutParams(0, dp(40), 1));

        Button newChatButton = createToolbarIconButton("+");
        newChatButton.setContentDescription("New chat");
        newChatButton.setOnClickListener(v -> createAndSelectSession());
        LinearLayout.LayoutParams newChatParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        newChatParams.setMargins(dp(6), 0, 0, 0);
        header.addView(newChatButton, newChatParams);

        mHeaderDisplayButton = createToolbarIconButton("▭");
        mHeaderDisplayButton.setContentDescription("Agent display");
        mHeaderDisplayButton.setOnClickListener(v -> {
            if (mMode == MODE_DISPLAY) {
                switchMode(MODE_CHAT);
            } else {
                mDisplayHomeVisible = true;
                if (mDisplayHomeOverlay != null) {
                    mDisplayHomeOverlay.setAlpha(1f);
                    mDisplayHomeOverlay.setVisibility(View.VISIBLE);
                }
                switchMode(MODE_DISPLAY);
            }
        });
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        displayParams.setMargins(dp(6), 0, 0, 0);
        header.addView(mHeaderDisplayButton, displayParams);

        Button menuButton = createToolbarIconButton("⋮");
        menuButton.setContentDescription("More");
        menuButton.setOnClickListener(v -> showWorkspaceMenu());
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        menuParams.setMargins(dp(6), 0, 0, 0);
        header.addView(menuButton, menuParams);

        return header;
    }

    private View createComposer() {
        UiSpec ui = ui();
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.VERTICAL);
        composer.setPadding(dp(12), dp(8), dp(12), dp(16));
        composer.setBackgroundColor(ui.composer.fill);

        LinearLayout messageRow = new LinearLayout(this);
        messageRow.setOrientation(LinearLayout.HORIZONTAL);
        messageRow.setGravity(Gravity.CENTER_VERTICAL);
        messageRow.setPadding(dp(6), dp(6), dp(6), dp(6));
        messageRow.setMinimumHeight(dp(56));
        messageRow.setBackground(makeSurfaceDrawable(ui.composerInput, true));
        messageRow.setElevation(dp(8));
        LinearLayout.LayoutParams messageRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        mAttachButton = createComposerIconButton("+");
        mAttachButton.setContentDescription("Attach file to this chat workspace");
        mAttachButton.setOnClickListener(v -> pickAttachment());
        LinearLayout.LayoutParams attachParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        attachParams.setMargins(0, 0, dp(4), 0);
        messageRow.addView(mAttachButton, attachParams);

        mPromptInput = new EditText(this);
        mPromptInput.setHint(getString(R.string.oringutan_prompt_hint));
        mPromptInput.setMinLines(1);
        mPromptInput.setMaxLines(4);
        mPromptInput.setMinHeight(dp(42));
        mPromptInput.setMinimumHeight(dp(42));
        mPromptInput.setSingleLine(false);
        mPromptInput.setTextIsSelectable(true);
        mPromptInput.setIncludeFontPadding(false);
        mPromptInput.setTextColor(ui.composerInput.text);
        mPromptInput.setHintTextColor(ui.muted);
        mPromptInput.setTextSize(16);
        mPromptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mPromptInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        mPromptInput.setBackgroundColor(Color.TRANSPARENT);
        mPromptInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        mPromptInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitPrompt();
                return true;
            }
            return false;
        });
        messageRow.addView(mPromptInput, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        mSendButton = createComposerSendButton("↑");
        mSendButton.setContentDescription(getString(R.string.oringutan_send));
        mSendButton.setOnClickListener(v -> submitPrompt());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        sendParams.setMargins(dp(4), 0, 0, 0);
        messageRow.addView(mSendButton, sendParams);
        composer.addView(messageRow, messageRowParams);

        return composer;
    }

    private void loadOrCreateSessions() {
        ensureChatRoot();
        loadSessions();
        if (mSessions.isEmpty()) {
            ChatSession session = createSession("New chat");
            mSessions.add(session);
            appendMessage(session, new ChatMessage("system", "Ready.", nowLabel()), true);
        }

        String activeId = mPrefs.getString(PREF_ACTIVE_CHAT_ID, null);
        ChatSession selected = findSession(activeId);
        if (selected == null) selected = mSessions.get(0);
        setActiveSession(selected);
    }

    private void ensureChatRoot() {
        File root = new File(getChatRootPath());
        if (!root.isDirectory() && !root.mkdirs())
            Logger.logError(LOG_TAG, "Failed to create chat root: " + root.getAbsolutePath());
    }

    private void loadSessions() {
        mSessions.clear();
        File root = new File(getChatRootPath());
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            ChatSession session = loadSession(dir);
            if (session != null) mSessions.add(session);
        }
        mSessions.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
    }

    private ChatSession loadSession(File dir) {
        File meta = new File(dir, META_FILE_NAME);
        if (!meta.isFile()) return null;

        try {
            JSONObject object = new JSONObject(readFile(meta));
            String id = object.optString("id", dir.getName());
            String title = object.optString("title", "Chat " + id);
            long createdAt = object.optLong("createdAt", dir.lastModified());
            ChatSession session = new ChatSession(id, title, createdAt, dir.getAbsolutePath());
            loadHistory(session);
            return session;
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load chat session", e);
            return null;
        }
    }

    private void loadHistory(ChatSession session) {
        File history = new File(session.historyPath);
        if (!history.isFile()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(history))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                JSONObject object = new JSONObject(line);
                session.messages.add(new ChatMessage(object.optString("role", "assistant"),
                    object.optString("text", ""), object.optString("timestamp", "")));
            }
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load chat history", e);
        }
    }

    private ChatSession createSession(String title) {
        String id = Long.toString(System.currentTimeMillis(), 36);
        File root = new File(getChatRootPath(), id);
        File workspace = new File(root, "workspace");
        if (!workspace.isDirectory() && !workspace.mkdirs())
            Logger.logError(LOG_TAG, "Failed to create workspace: " + workspace.getAbsolutePath());

        ChatSession session = new ChatSession(id, title, System.currentTimeMillis(), root.getAbsolutePath());
        saveMeta(session);
        return session;
    }

    private void createAndSelectSession() {
        if (!mBootstrapReady) return;
        ChatSession session = createSession("New chat");
        mSessions.add(0, session);
        appendMessage(session, new ChatMessage("system", "Ready.", nowLabel()), true);
        setActiveSession(session);
    }

    private void showChatPicker() {
        if (mDrawerLayout == null || mChatDrawer == null) return;
        renderChatDrawer();
        if (mDrawerLayout.isDrawerOpen(mChatDrawer)) {
            mDrawerLayout.closeDrawer(mChatDrawer);
            return;
        }

        mDrawerLayout.openDrawer(mChatDrawer);
    }

    private View createChatDrawer() {
        UiSpec ui = ui();
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setPadding(dp(16), dp(18), dp(16), dp(14));
        drawer.setBackgroundColor(ui.drawer.fill);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Chats");
        title.setTextColor(ui.drawer.text);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(22);
        title.setSingleLine(true);
        title.setIncludeFontPadding(false);
        topRow.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1));

        Button newChatButton = createToolbarIconButton("+");
        newChatButton.setContentDescription("New chat");
        newChatButton.setOnClickListener(v -> {
            if (mDrawerLayout != null && mChatDrawer != null) mDrawerLayout.closeDrawer(mChatDrawer);
            createAndSelectSession();
        });
        LinearLayout.LayoutParams newChatParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        newChatParams.setMargins(dp(10), 0, 0, 0);
        topRow.addView(newChatButton, newChatParams);
        drawer.addView(topRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));

        mChatSearchInput = new EditText(this);
        mChatSearchInput.setSingleLine(true);
        mChatSearchInput.setHint("Search chats");
        mChatSearchInput.setText(mChatSearchQuery);
        mChatSearchInput.setTextSize(15);
        mChatSearchInput.setTextColor(ui.drawerSearch.text);
        mChatSearchInput.setHintTextColor(ui.onDarkMuted);
        mChatSearchInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mChatSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mChatSearchInput.setPadding(dp(14), 0, dp(14), 0);
        mChatSearchInput.setBackground(makeSurfaceDrawable(ui.drawerSearch, true));
        mChatSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mChatSearchQuery = s == null ? "" : s.toString();
                renderChatDrawer();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        searchParams.setMargins(0, dp(14), 0, dp(12));
        drawer.addView(mChatSearchInput, searchParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        mChatDrawerList = new LinearLayout(this);
        mChatDrawerList.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(mChatDrawerList, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        drawer.addView(scrollView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        return drawer;
    }

    private void setActiveSession(ChatSession session) {
        mActiveSession = session;
        mPrefs.edit().putString(PREF_ACTIVE_CHAT_ID, session.id).apply();
        renderChatDrawer();
        renderHeader();
        renderMode();
    }

    private void renderChatDrawer() {
        if (mChatDrawerList == null) return;
        UiSpec ui = ui();
        mChatDrawerList.removeAllViews();

        int visibleCount = 0;
        for (ChatSession session : mSessions) {
            if (!matchesChatSearch(session)) continue;
            View row = createChatDrawerRow(session, session == mActiveSession);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(6));
            mChatDrawerList.addView(row, params);
            visibleCount++;
        }

        if (visibleCount == 0) {
            TextView empty = new TextView(this);
            empty.setText("No matching chats");
            empty.setTextColor(ui.onDarkMuted);
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(28), dp(12), dp(28));
            mChatDrawerList.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private boolean matchesChatSearch(ChatSession session) {
        String query = mChatSearchQuery == null ? "" : mChatSearchQuery.trim().toLowerCase(Locale.US);
        if (query.isEmpty()) return true;
        String title = session.title == null ? "" : session.title.toLowerCase(Locale.US);
        String last = latestUserVisibleMessage(session).toLowerCase(Locale.US);
        return title.contains(query) || last.contains(query);
    }

    private View createChatDrawerRow(ChatSession session, boolean active) {
        UiSpec ui = ui();
        SurfaceSpec surface = active ? ui.drawerRowActive : ui.drawerRow;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setMinimumHeight(dp(56));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(session.title);
        row.setBackground(makeSurfaceDrawable(surface, false));
        attachNativeRipple(row);
        row.setOnClickListener(v -> {
            if (mDrawerLayout != null && mChatDrawer != null) mDrawerLayout.closeDrawer(mChatDrawer);
            setActiveSession(session);
        });

        TextView title = new TextView(this);
        title.setText(session.title);
        title.setTextColor(surface.text);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(14);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setIncludeFontPadding(false);
        row.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(22)));

        TextView meta = new TextView(this);
        meta.setText(sessionMeta(session));
        meta.setTextColor(active ? ui.onDarkMuted : ui.muted);
        meta.setTextSize(12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        meta.setIncludeFontPadding(false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(18));
        metaParams.setMargins(0, dp(4), 0, 0);
        row.addView(meta, metaParams);

        return row;
    }

    private String sessionMeta(ChatSession session) {
        int messageCount = visibleMessageCount(session);
        String latest = latestUserVisibleMessage(session);
        String count = messageCount + (messageCount == 1 ? " message" : " messages");
        if (messageCount == 0) return "No messages yet";
        if (latest.isEmpty()) return count;
        return count + " · " + latest;
    }

    private int visibleMessageCount(ChatSession session) {
        int count = 0;
        for (ChatMessage message : session.messages) {
            if ("system".equals(message.role)) continue;
            String text = message.text == null ? "" : message.text.trim();
            if (!text.isEmpty()) count++;
        }
        return count;
    }

    private String latestUserVisibleMessage(ChatSession session) {
        for (int i = session.messages.size() - 1; i >= 0; i--) {
            ChatMessage message = session.messages.get(i);
            if ("system".equals(message.role)) continue;
            String text = message.text == null ? "" : message.text.replace('\n', ' ').trim();
            if (!text.isEmpty()) {
                if (text.length() > 46) text = text.substring(0, 46).trim() + "...";
                return text;
            }
        }
        return "";
    }

    private void renderHeader() {
        if (mActiveSession == null) return;
        if (mTitleView != null) {
            mTitleView.setText(mActiveSession.title);
            mTitleView.setContentDescription(mActiveSession.title);
        }
        if (mSubtitleView != null) {
            int messageCount = visibleMessageCount(mActiveSession);
            mSubtitleView.setText(skin().name + " · " + messageCount
                + (messageCount == 1 ? " message" : " messages"));
        }
    }

    private void switchMode(int mode) {
        if (mMode == mode) return;
        mMode = mode;
        renderMode();
    }

    private void renderMode() {
        if (mContentFrame == null) return;

        if (mChatModeButton != null) styleModeButton(mChatModeButton, mMode == MODE_CHAT);
        if (mTerminalModeButton != null) styleModeButton(mTerminalModeButton, mMode == MODE_TERMINAL);
        if (mDisplayModeButton != null) styleModeButton(mDisplayModeButton, mMode == MODE_DISPLAY);
        if (mHeaderDisplayButton != null) styleHeaderButton(mHeaderDisplayButton, mMode == MODE_DISPLAY);
        if (mSwapButton != null) {
            mSwapButton.setEnabled(mMode != MODE_CHAT);
            styleModeButton(mSwapButton, false);
        }
        updateAppChromeForMode();
        updateComposerTools();

        if (mMode != MODE_DISPLAY) parkDisplayPane();
        mContentFrame.removeAllViews();
        View nextView;
        if (mMode == MODE_DISPLAY) {
            nextView = getOrCreateDisplayPane();
            detachFromParent(nextView);
        } else if (mMode == MODE_TERMINAL) {
            nextView = createToolPaneSurface(createTerminalPane());
        } else {
            nextView = createChatPane();
        }
        mContentFrame.addView(nextView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        animateModeView(nextView);
    }

    private void detachFromParent(View view) {
        if (view == null) return;
        if (view.getParent() instanceof ViewGroup)
            ((ViewGroup) view.getParent()).removeView(view);
    }

    private void parkDisplayPane() {
        if (mDisplayPane == null || mDisplayWarmHost == null) return;
        if (mDisplayPane.getParent() == mDisplayWarmHost) return;
        detachFromParent(mDisplayPane);
        mDisplayWarmHost.addView(mDisplayPane, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void prewarmDisplaySurface() {
        if (!mBootstrapReady || mActiveSession == null || mDisplayWarmHost == null
            || mMode == MODE_DISPLAY) return;

        View pane = getOrCreateDisplayPane();
        if (pane.getParent() != mDisplayWarmHost) {
            detachFromParent(pane);
            mDisplayWarmHost.addView(pane, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        ensureDisplayServerStarted(true);
    }

    private View createToolPaneSurface(View toolPane) {
        if (shouldUseSplitPane()) return createSplitPane(toolPane);
        return toolPane;
    }

    private boolean shouldUseSplitPane() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        if (widthDp <= 0)
            widthDp = (int) (getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        return widthDp >= 700;
    }

    private View createSplitPane(View toolPane) {
        UiSpec ui = ui();
        LinearLayout split = new LinearLayout(this);
        split.setOrientation(LinearLayout.HORIZONTAL);
        split.setBackgroundColor(ui.chat.fill);

        View chatPane = createChatPane();
        View divider = createSplitDivider(split);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT, Math.round(mSplitRatio * 1000));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.MATCH_PARENT, Math.round((1f - mSplitRatio) * 1000));

        if (mSplitReversed) {
            split.addView(toolPane, leftParams);
            split.addView(divider, new LinearLayout.LayoutParams(dp(10), LinearLayout.LayoutParams.MATCH_PARENT));
            split.addView(chatPane, rightParams);
        } else {
            split.addView(chatPane, leftParams);
            split.addView(divider, new LinearLayout.LayoutParams(dp(10), LinearLayout.LayoutParams.MATCH_PARENT));
            split.addView(toolPane, rightParams);
        }

        return split;
    }

    private View createSplitDivider(LinearLayout split) {
        UiSpec ui = ui();
        TextView divider = new TextView(this);
        divider.setText("|");
        divider.setGravity(Gravity.CENTER);
        divider.setTextColor(ui.muted);
        divider.setTextSize(18);
        divider.setTypeface(Typeface.DEFAULT_BOLD);
        divider.setBackgroundColor(ui.border);
        divider.setOnTouchListener((view, event) -> {
            if (event.getAction() != MotionEvent.ACTION_DOWN
                && event.getAction() != MotionEvent.ACTION_MOVE
                && event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }

            int[] location = new int[2];
            split.getLocationOnScreen(location);
            int splitWidth = Math.max(1, split.getWidth());
            float ratio = (event.getRawX() - location[0]) / splitWidth;
            float minRatio = Math.min(0.45f, Math.max(0.25f, (float) dp(260) / splitWidth));
            mSplitRatio = Math.max(minRatio, Math.min(1f - minRatio, ratio));
            renderMode();
            return true;
        });
        return divider;
    }

    private View createChatPane() {
        UiSpec ui = ui();
        mScrollView = new ScrollView(this);
        mScrollView.setFillViewport(true);
        mScrollView.setBackgroundColor(ui.chat.fill);

        mMessagesView = new LinearLayout(this);
        mMessagesView.setOrientation(LinearLayout.VERTICAL);
        mMessagesView.setPadding(dp(14), dp(12), dp(14), dp(12));
        mScrollView.addView(mMessagesView, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        if (mActiveSession != null) {
            for (ChatMessage message : mActiveSession.messages) {
                if (shouldHideSystemReadyMessage(message)) continue;
                addBubble(message.text, "user".equals(message.role), false);
            }
        }

        scrollToBottom();
        return mScrollView;
    }

    private View createTerminalPane() {
        UiSpec ui = ui();
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ui.app.fill);

        LinearLayout pane = new LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);
        pane.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(pane, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        if (mActiveSession == null) return scrollView;

        TextView name = new TextView(this);
        name.setText(mActiveSession.title);
        name.setTextColor(ui.ink);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextSize(22);
        pane.addView(name, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView shellName = terminalMetaText("Per-chat terminal");
        LinearLayout.LayoutParams shellParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        shellParams.setMargins(0, dp(4), 0, dp(10));
        pane.addView(shellName, shellParams);

        TextView workspace = terminalBlock(mActiveSession.workspacePath);
        pane.addView(workspace, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button open = createAccentButton(getString(R.string.oringutan_open_terminal));
        open.setOnClickListener(v -> openTerminalForActiveChat());
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        openParams.setMargins(0, dp(14), 0, dp(10));
        pane.addView(open, openParams);

        Button copyCd = createSecondaryButton("Copy cd");
        copyCd.setOnClickListener(v -> {
            copyToClipboard("Ominal workspace", "cd " + shellQuote(mActiveSession.workspacePath));
            setStatus("Workspace command copied");
        });
        pane.addView(copyCd, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));

        mStatusView = terminalMetaText(mPromptRunning ? "Agent running" : "Ready");
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dp(18), 0, 0);
        pane.addView(mStatusView, statusParams);

        return scrollView;
    }

    private View getOrCreateDisplayPane() {
        if (mDisplayPane != null) {
            if (mDisplayReady) loadDisplayWebView();
            else ensureDisplayServerStarted(true);
            return mDisplayPane;
        }

        UiSpec ui = ui();
        FrameLayout pane = new FrameLayout(this);
        pane.setBackgroundColor(ui.displayHome.fill);

        if (mActiveSession == null) return pane;

        FrameLayout screen = new FrameLayout(this);
        screen.setPadding(0, 0, 0, 0);
        screen.setBackgroundColor(Color.BLACK);
        pane.addView(screen, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mDisplayWebView = new WebView(this);
        mDisplayRetryCount = 0;
        mDisplayWebView.setBackgroundColor(Color.BLACK);
        mDisplayWebView.getSettings().setJavaScriptEnabled(true);
        mDisplayWebView.getSettings().setDomStorageEnabled(true);
        mDisplayWebView.getSettings().setLoadWithOverviewMode(true);
        mDisplayWebView.getSettings().setUseWideViewPort(true);
        mDisplayWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.startsWith("http://127.0.0.1:6080/"))
                    mDisplayUrlLoaded = true;
                hideViewerChrome(view);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (failingUrl != null && failingUrl.startsWith("http://127.0.0.1:6080/"))
                    retryDisplayLoad();
            }
        });
        mDisplayWebView.setWebChromeClient(new WebChromeClient());
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        screen.addView(mDisplayWebView, webParams);

        mDisplayHomeOverlay = createDisplayHomeOverlay();
        pane.addView(mDisplayHomeOverlay, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        pane.addView(createDisplayOverlay(), new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (mDisplayReady) loadDisplayWebView();
        else ensureDisplayServerStarted(true);

        mDisplayPane = pane;
        return mDisplayPane;
    }

    private View createDisplayHomeOverlay() {
        UiSpec ui = ui();
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(dp(20), dp(46), dp(20), dp(24));
        home.setBackgroundColor(ui.displayHome.fill);
        home.setVisibility(mDisplayHomeVisible ? View.VISIBLE : View.GONE);

        TextView screen = new TextView(this);
        screen.setText("Display");
        screen.setTextColor(ui.displayHome.text);
        screen.setTextSize(18);
        screen.setTypeface(Typeface.DEFAULT_BOLD);
        screen.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        screen.setIncludeFontPadding(false);
        home.addView(screen, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));

        View spacer = new View(this);
        home.addView(spacer, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.35f));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        addDisplayHomeRow(grid,
            createDisplayHomeTile("View", "Screen", v -> showLiveDisplay()),
            createDisplayHomeTile(">_", "Shell", v -> launchDisplayApp(displayXtermCommand("Ominal Shell",
                "cd \"${OMINAL_WORKDIR:-$HOME}\" 2>/dev/null || cd \"$HOME\"; export PS1=\"ominal:\\W# \"; exec bash --noprofile --norc -i"))),
            createDisplayHomeTile("Files", "Files", v -> launchDisplayApp(displayXtermCommand("Files",
                "cd \"${OMINAL_WORKDIR:-$HOME}\" 2>/dev/null || cd \"$HOME\"; ls -la; exec bash --noprofile --norc -i"))));
        addDisplayHomeRow(grid,
            createDisplayHomeTile("Web", "Browser", v -> launchDisplayApp(
                "if command -v chromium >/dev/null 2>&1; then chromium --no-sandbox >/dev/null 2>&1 & elif command -v firefox >/dev/null 2>&1; then firefox >/dev/null 2>&1 & else "
                    + displayXtermCommand("Browser", "python3 -m http.server 8080; exec bash --noprofile --norc -i")
                    + " fi")),
            createDisplayHomeTile("Edit", "Editor", v -> launchDisplayApp(
                "if command -v leafpad >/dev/null 2>&1; then leafpad >/dev/null 2>&1 & else "
                    + displayXtermCommand("Editor", "nano; exec bash --noprofile --norc -i")
                    + " fi")),
            createDisplayHomeTile("Setup", "Setup", v -> launchDisplayApp(displayXtermCommand("Setup",
                "printf 'Workspace: %s\\nScreen ready\\n' \"$PWD\"; exec bash --noprofile --norc -i"))));
        home.addView(grid, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View bottomSpacer = new View(this);
        home.addView(bottomSpacer, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.65f));

        return home;
    }

    private void addDisplayHomeRow(LinearLayout grid, View first, View second, View third) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(8), 0, dp(8));
        grid.addView(row, rowParams);
        addDisplayTileToRow(row, first);
        addDisplayTileToRow(row, second);
        addDisplayTileToRow(row, third);
    }

    private void addDisplayTileToRow(LinearLayout row, View tile) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(100), 1);
        params.setMargins(dp(6), 0, dp(6), 0);
        row.addView(tile, params);
    }

    private View createDisplayHomeTile(String icon, String label, View.OnClickListener listener) {
        UiSpec ui = ui();
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(8), dp(6), dp(7));
        tile.setBackground(makeSurfaceDrawable(ui.displayTile, true));
        tile.setOnClickListener(listener);
        tile.setClickable(true);
        attachNativeRipple(tile);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextColor(ui.displayTile.text);
        iconView.setTextSize(icon.length() <= 2 ? 24 : 15);
        iconView.setTypeface(Typeface.DEFAULT_BOLD);
        iconView.setGravity(Gravity.CENTER);
        iconView.setIncludeFontPadding(false);
        tile.addView(iconView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ui.onDarkMuted);
        labelView.setTextSize(11);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setIncludeFontPadding(false);
        tile.addView(labelView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(18)));

        return tile;
    }

    private String displayXtermCommand(String title, String body) {
        return "xterm -geometry 66x34+10+72 -fa Monospace -fs 9 "
            + "-bg '#050506' -fg '#f4f5f7' -cr '#ffffff' -bd '#050506' "
            + "-T " + shellQuote(title) + " -e bash -lc " + shellQuote(body)
            + " >/dev/null 2>&1 &";
    }

    private void showLiveDisplay() {
        if (!mDisplayReady) {
            ensureDisplayServerStarted(true);
            setStatus("Starting display");
            return;
        }
        mDisplayHomeVisible = false;
        if (mDisplayHomeOverlay != null) {
            mDisplayHomeOverlay.animate().cancel();
            mDisplayHomeOverlay.animate()
                .alpha(0f)
                .setDuration(140)
                .withEndAction(() -> {
                    if (!mDisplayHomeVisible && mDisplayHomeOverlay != null)
                        mDisplayHomeOverlay.setVisibility(View.GONE);
                })
                .start();
        }
        loadDisplayWebView();
    }

    private void launchDisplayApp(String appCommand) {
        if (mActiveSession == null) return;
        ensureDisplayServerStarted(false);
        showLiveDisplay();

        String inner = "export DISPLAY=${OMINAL_DISPLAY:-:20}; "
            + "export OMINAL_WORKDIR=" + shellQuote(mActiveSession.workspacePath) + "; "
            + appCommand;
        String commandLine = "ominal-proot-run /bin/bash -c " + shellQuote(inner);

        new Thread(() -> {
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
                new String[]{"-lc", commandLine},
                null,
                mActiveSession.workspacePath,
                ExecutionCommand.Runner.APP_SHELL.getName(),
                false);
            command.commandLabel = "Ominal display app";

            HashMap<String, String> environment = new HashMap<>();
            environment.put("OMINAL_DISPLAY", ":20");
            environment.put("OMINAL_DISPLAY_GEOMETRY", getDisplayGeometry());
            environment.put("OMINAL_WORKDIR", mActiveSession.workspacePath);
            AppShell.execute(this, command, null, new OminalShellEnvironment(), environment, true);
        }).start();
    }

    private View createDisplayOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setPadding(dp(12), dp(12), dp(12), dp(12));

        mDisplayCloseButton = createToolbarIconButton("×");
        mDisplayCloseButton.setContentDescription("Close display");
        mDisplayCloseButton.setOnClickListener(v -> switchMode(MODE_CHAT));
        mDisplayCloseButton.setAlpha(0.94f);
        mDisplayCloseButton.setElevation(dp(12));

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        overlay.addView(mDisplayCloseButton, closeParams);
        animateOverlayView(mDisplayCloseButton);
        return overlay;
    }

    private void loadDisplayWebView() {
        if (mDisplayWebView != null) {
            hideViewerChrome(mDisplayWebView);
            mDisplayUrlLoaded = true;
            mDisplayWebView.loadUrl(displayUrl());
        }
    }

    private String displayUrl() {
        return DISPLAY_URL + "&_ominal=" + System.currentTimeMillis();
    }

    private void retryDisplayLoad() {
        if (mDisplayWebView == null || mDisplayRetryCount >= 4) return;
        mDisplayRetryCount++;
        mDisplayUrlLoaded = false;
        mDisplayWebView.postDelayed(() -> ensureDisplayServerStarted(true), 1200);
    }

    private void hideViewerChrome(WebView view) {
        String script = "(function(){"
            + "var style=document.getElementById('ominal-display-style');"
            + "if(!style){style=document.createElement('style');style.id='ominal-display-style';document.head.appendChild(style);}"
            + "style.textContent='html,body{margin:0!important;width:100%!important;height:100%!important;overflow:hidden!important;background:#000!important;cursor:none!important;}#top_bar,#sendCtrlAltDelButton,#noVNC_control_bar,#noVNC_status_bar,#noVNC_control_bar_handle,#noVNC_connect_dlg{display:none!important;}#screen,#noVNC_screen,#noVNC_container{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;margin:0!important;padding:0!important;background:#000!important;overflow:hidden!important;display:flex!important;align-items:center!important;justify-content:center!important;cursor:none!important;}canvas,#noVNC_canvas{display:block!important;width:auto!important;height:auto!important;max-width:100vw!important;max-height:100vh!important;margin:auto!important;object-fit:contain!important;background:#000!important;image-rendering:auto!important;cursor:none!important;}#noVNC_canvas:focus{outline:none!important;}';"
            + "var top=document.getElementById('top_bar');if(top)top.style.display='none';"
            + "['screen','noVNC_screen','noVNC_container'].forEach(function(id){var el=document.getElementById(id);if(el){el.style.position='fixed';el.style.inset='0';el.style.width='100vw';el.style.height='100vh';el.style.margin='0';el.style.padding='0';el.style.display='flex';el.style.alignItems='center';el.style.justifyContent='center';}});"
            + "Array.prototype.forEach.call(document.getElementsByTagName('canvas'),function(canvas){canvas.style.width='auto';canvas.style.height='auto';canvas.style.maxWidth='100vw';canvas.style.maxHeight='100vh';canvas.style.objectFit='contain';canvas.style.margin='auto';canvas.style.cursor='none';});"
            + "document.body.style.margin='0';document.body.style.background='#000';"
            + "})()";
        view.evaluateJavascript(script, null);
        view.postDelayed(() -> view.evaluateJavascript(script, null), 1000);
        view.postDelayed(() -> view.evaluateJavascript(script, null), 3000);
    }

    private void ensureDisplayServerStarted(boolean reloadWhenReady) {
        if (!mBootstrapReady || mActiveSession == null) return;
        long now = System.currentTimeMillis();
        if (mDisplayStartInFlight) {
            if (reloadWhenReady) mReloadDisplayWhenReady = true;
            return;
        }
        if (reloadWhenReady && mDisplayReady && now - mDisplayLastStartedAt < 8000) {
            loadDisplayWebView();
            return;
        }

        mDisplayStartInFlight = true;
        mReloadDisplayWhenReady = reloadWhenReady;
        mDisplayLastStartedAt = now;

        new Thread(() -> {
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
                new String[]{"-lc", DISPLAY_START_COMMAND},
                null,
                mActiveSession.workspacePath,
                ExecutionCommand.Runner.APP_SHELL.getName(),
                false);
            command.commandLabel = "Codex display start";

            HashMap<String, String> environment = new HashMap<>();
            environment.put("OMINAL_DISPLAY", ":20");
            environment.put("OMINAL_DISPLAY_GEOMETRY", getDisplayGeometry());
            environment.put("OMINAL_WORKDIR", mActiveSession.workspacePath);

            AppShell.execute(this, command, null, new OminalShellEnvironment(), environment, true);
            boolean ready = waitForDisplayEndpoint();
            runOnUiThread(() -> {
                mDisplayReady = ready;
                mDisplayStartInFlight = false;
                boolean shouldReload = mReloadDisplayWhenReady;
                mReloadDisplayWhenReady = false;
                if (ready) {
                    setStatus("Display ready");
                    if (shouldReload) showLiveDisplay();
                } else {
                    setStatus("Display unavailable");
                    mDisplayStartupDetail = "Linux display did not become ready.";
                    if (mDisplayAvailabilityView != null)
                        mDisplayAvailabilityView.setText(mDisplayStartupDetail);
                }
            });
        }).start();
    }

    private boolean waitForDisplayEndpoint() {
        for (int attempt = 0; attempt < DISPLAY_HEALTH_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("http://127.0.0.1:6080/vnc_lite.html").openConnection();
                connection.setConnectTimeout(300);
                connection.setReadTimeout(300);
                connection.setUseCaches(false);
                int status = connection.getResponseCode();
                if (status >= 200 && status < 400) return true;
            } catch (IOException ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            try {
                Thread.sleep(DISPLAY_HEALTH_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void submitPrompt() {
        if (!mBootstrapReady) {
            addTransientSystemMessage("Still preparing the Ominal executable area.");
            return;
        }
        if (mActiveSession == null || mPromptRunning) return;

        String prompt = mPromptInput.getText().toString().trim();
        if (prompt.isEmpty()) return;

        mPromptInput.setText("");
        if ("New chat".equals(mActiveSession.title))
            renameSessionFromPrompt(mActiveSession, prompt);

        ChatMessage userMessage = new ChatMessage("user", prompt, nowLabel());
        appendMessage(mActiveSession, userMessage, true);
        TextView responseBubble = addBubble("Running through Ominal agent adapter...", false, true);
        runPrompt(mActiveSession, prompt, responseBubble);
    }

    private void pickAttachment() {
        if (!mBootstrapReady) {
            addTransientSystemMessage("Still preparing the Ominal executable area.");
            return;
        }
        if (mActiveSession == null) return;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.oringutan_attach_file)),
                REQUEST_ATTACH_FILE);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open attachment picker", e);
            addTransientSystemMessage("File picker unavailable.");
        }
    }

    private void handleAttachmentResult(Intent data) {
        if (mActiveSession == null) return;

        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) uris.add(uri);
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }

        if (uris.isEmpty()) return;
        if (mMode != MODE_CHAT) switchMode(MODE_CHAT);

        for (Uri uri : uris) {
            try {
                String relativePath = copyAttachmentToWorkspace(mActiveSession, uri);
                appendMessage(mActiveSession, new ChatMessage("system", "Attached: " + relativePath, nowLabel()), true);
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to copy attachment", e);
                addTransientSystemMessage("Attachment failed: " + getDisplayName(uri));
            }
        }
    }

    private void runPrompt(ChatSession session, String prompt, TextView responseBubble) {
        refreshRuntimeDns();
        mPromptRunning = true;
        setInputEnabled(false);
        setStatus("Agent running");
        ensureDisplayServerStarted(false);

        new Thread(() -> {
            ensureDirectory(session.workspacePath);
            String agentPrompt = buildAgentPrompt(session, prompt);
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
                new String[]{"-lc", CODEX_ADAPTER_SCRIPT, "ominal-agent", agentPrompt},
                null,
                session.workspacePath,
                ExecutionCommand.Runner.APP_SHELL.getName(),
                false);
            command.commandLabel = "Ominal agent prompt";

            HashMap<String, String> environment = new HashMap<>();
            environment.put("ORINGUTAN_FRONTEND", "chat");
            environment.put("OMINAL_CHAT_ID", session.id);
            environment.put("OMINAL_WORKDIR", session.workspacePath);
            environment.put("OMINAL_DISPLAY", ":20");
            environment.put("OMINAL_DISPLAY_GEOMETRY", getDisplayGeometry());
            environment.put("OMINAL_AGENT_TIMEOUT", "600");
            environment.put("OMINAL_AGENT_PREFLIGHT", "1");
            environment.put("OMINAL_PREFLIGHT_TIMEOUT", "25");

            AppShell.execute(this, command, null, new OminalShellEnvironment(), environment, true);

            String output = formatCommandOutput(command);
            boolean openDisplay = shouldAutoOpenDisplay(output);
            String visibleOutput = stripDisplayMarkers(output);
            runOnUiThread(() -> {
                responseBubble.setText(visibleOutput);
                ChatMessage assistantMessage = new ChatMessage("assistant", visibleOutput, nowLabel());
                session.messages.add(assistantMessage);
                appendHistory(session, assistantMessage);
                mPromptRunning = false;
                setInputEnabled(true);
                if (openDisplay) {
                    setStatus("User input needed");
                    switchMode(MODE_DISPLAY);
                    Toast.makeText(this, "Agent needs input on the display", Toast.LENGTH_SHORT).show();
                } else {
                    setStatus("Ready");
                }
                scrollToBottom();
            });
        }).start();
    }

    private String buildAgentPrompt(ChatSession session, String prompt) {
        File attachmentDir = new File(session.workspacePath, ATTACHMENTS_DIR_NAME);
        File[] files = attachmentDir.listFiles(File::isFile);

        StringBuilder builder = new StringBuilder();
        builder.append("You are running as the Codex agent inside Ominal, an Android coding workspace. ")
            .append("Use the current working directory for this chat's files and outputs. ")
            .append("A graphical display is available on DISPLAY=:20 when a GUI is needed. ")
            .append("Use that display autonomously for OS/browser/app work whenever possible. ")
            .append("If user input, visual confirmation, login, or manual control is required, print ")
            .append(DISPLAY_USER_INPUT_MARKER)
            .append(" on its own line with a short reason; Ominal will open the display for the user.\n\n")
            .append("User request:\n")
            .append(prompt);
        if (files != null && files.length > 0) {
            builder.append("\n\nAttached files are available in this chat workspace under ./")
                .append(ATTACHMENTS_DIR_NAME)
                .append(":\n");

            int count = 0;
            for (File file : files) {
                builder.append("- ")
                    .append(ATTACHMENTS_DIR_NAME)
                    .append("/")
                    .append(file.getName())
                    .append("\n");
                count++;
                if (count >= 40) break;
            }
        }

        return builder.toString();
    }

    private String formatCommandOutput(ExecutionCommand command) {
        String stdout = command.resultData.stdout.toString().trim();
        String stderr = command.resultData.stderr.toString().trim();
        Integer exitCode = command.resultData.exitCode;

        StringBuilder result = new StringBuilder();
        if (!stdout.isEmpty()) {
            result.append(stdout);
        } else if (!stderr.isEmpty()) {
            result.append(stderr);
        }

        if (command.isStateFailed()) {
            if (result.length() > 0) result.append("\n\n");
            result.append("Agent process failed before completion.");
            String errors = ResultData.getErrorsListMinimalString(command.resultData).trim();
            if (!errors.isEmpty()) result.append("\n").append(errors);
        }
        if (exitCode != null && exitCode != 0) {
            if (result.length() > 0) result.append("\n\n");
            result.append("exit code: ").append(exitCode);
            if (!stderr.isEmpty() && stdout.length() > 0)
                result.append("\n\nstderr:\n").append(stderr);
        }
        if (result.length() == 0) result.append("No output.");
        return result.toString();
    }

    private boolean shouldAutoOpenDisplay(String output) {
        if (output == null) return false;
        return output.contains(DISPLAY_USER_INPUT_MARKER) || output.contains(DISPLAY_OPEN_MARKER);
    }

    private String stripDisplayMarkers(String output) {
        if (output == null) return "";
        String cleaned = output
            .replace(DISPLAY_USER_INPUT_MARKER, "")
            .replace(DISPLAY_OPEN_MARKER, "")
            .trim();
        if (cleaned.isEmpty()) return "The agent needs user input on the display.";
        return cleaned;
    }

    private void openTerminalForActiveChat() {
        if (!mBootstrapReady || mActiveSession == null) return;

        ensureDirectory(mActiveSession.workspacePath);
        Intent executeIntent = new Intent(OMINAL_SERVICE.ACTION_SERVICE_EXECUTE);
        executeIntent.setClass(this, OminalService.class);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_WORKDIR, mActiveSession.workspacePath);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_RUNNER, ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_NAME, mActiveSession.terminalName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE, ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_COMMAND_LABEL, mActiveSession.title);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION,
            Integer.toString(OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY));
        startService(executeIntent);
        mContentFrame.postDelayed(() -> startActivity(new Intent(this, OminalActivity.class)), 300);
    }

    private void appendMessage(ChatSession session, ChatMessage message, boolean persist) {
        session.messages.add(message);
        if (persist) appendHistory(session, message);
        if (session == mActiveSession && mMode == MODE_CHAT && !shouldHideSystemReadyMessage(message))
            addBubble(message.text, "user".equals(message.role), true);
    }

    private boolean shouldHideSystemReadyMessage(ChatMessage message) {
        if (message == null || !"system".equals(message.role)) return false;
        String text = message.text == null ? "" : message.text.trim();
        return "Ready.".equals(text);
    }

    private void addTransientSystemMessage(String message) {
        if (mMode != MODE_CHAT) switchMode(MODE_CHAT);
        addBubble(message, false, true);
    }

    private TextView addBubble(String message, boolean fromUser, boolean scrollNow) {
        UiSpec ui = ui();
        SurfaceSpec surface = fromUser ? ui.bubbleUser : ui.bubbleAgent;
        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(15);
        bubble.setLineSpacing(0, 1.08f);
        bubble.setTextColor(surface.text);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        bubble.setBackground(makeSurfaceDrawable(surface, false));
        bubble.setTextIsSelectable(true);
        bubble.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        bubble.setFocusable(true);
        bubble.setFocusableInTouchMode(true);
        bubble.setOnLongClickListener(v -> {
            copyToClipboard("Ominal message", message);
            Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
            return true;
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.setMargins(dp(2), dp(4), dp(2), dp(8));
        bubble.setMaxWidth(Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(660)));
        if (mMessagesView != null) mMessagesView.addView(bubble, params);
        if (scrollNow) scrollToBottom();
        return bubble;
    }

    private void copyToClipboard(String label, String value) {
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, value));
    }

    private void appendHistory(ChatSession session, ChatMessage message) {
        ensureDirectory(session.rootPath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(session.historyPath, true))) {
            JSONObject object = new JSONObject();
            object.put("role", message.role);
            object.put("text", message.text);
            object.put("timestamp", message.timestamp);
            writer.write(object.toString());
            writer.newLine();
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to append chat history", e);
        }
    }

    private void saveMeta(ChatSession session) {
        ensureDirectory(session.rootPath);
        try {
            JSONObject object = new JSONObject();
            object.put("id", session.id);
            object.put("title", session.title);
            object.put("createdAt", session.createdAt);
            writeFile(new File(session.rootPath, META_FILE_NAME), object.toString());
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to save chat metadata", e);
        }
    }

    private void renameSessionFromPrompt(ChatSession session, String prompt) {
        String cleaned = prompt.replace('\n', ' ').trim();
        if (cleaned.length() > 36) cleaned = cleaned.substring(0, 36).trim();
        if (cleaned.isEmpty()) return;
        session.title = cleaned;
        saveMeta(session);
        renderChatDrawer();
        renderHeader();
    }

    private ChatSession findSession(String id) {
        if (id == null) return null;
        for (ChatSession session : mSessions)
            if (id.equals(session.id)) return session;
        return null;
    }

    private String getChatRootPath() {
        return OminalConstants.OMINAL_HOME_DIR_PATH + "/" + CHAT_ROOT_NAME;
    }

    private String copyAttachmentToWorkspace(ChatSession session, Uri uri) throws IOException {
        File attachmentDir = new File(session.workspacePath, ATTACHMENTS_DIR_NAME);
        ensureDirectory(attachmentDir.getAbsolutePath());

        String displayName = sanitizeFilename(getDisplayName(uri));
        File destination = getUniqueAttachmentFile(attachmentDir, displayName);

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            if (inputStream == null)
                throw new IOException("No readable stream for " + uri);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, bytesRead);
        }

        return ATTACHMENTS_DIR_NAME + "/" + destination.getName();
    }

    private File getUniqueAttachmentFile(File attachmentDir, String fileName) {
        File destination = new File(attachmentDir, fileName);
        if (!destination.exists()) return destination;

        int dotIndex = fileName.lastIndexOf('.');
        String base = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        int suffix = 1;
        while (destination.exists()) {
            destination = new File(attachmentDir, base + "-" + suffix + extension);
            suffix++;
        }
        return destination;
    }

    private String getDisplayName(Uri uri) {
        String displayName = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) displayName = cursor.getString(index);
            }
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read attachment display name", e);
        }

        if (TextUtils.isEmpty(displayName)) displayName = uri.getLastPathSegment();
        if (TextUtils.isEmpty(displayName)) displayName = "attachment";
        return displayName;
    }

    private String sanitizeFilename(String fileName) {
        String cleaned = fileName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_").trim();
        while (cleaned.startsWith(".")) cleaned = cleaned.substring(1);
        if (cleaned.length() > 96) cleaned = cleaned.substring(0, 96).trim();
        if (cleaned.isEmpty()) cleaned = "attachment";
        return cleaned;
    }

    private void ensureDirectory(String path) {
        if (path == null) return;
        File dir = new File(path);
        if (!dir.isDirectory() && !dir.mkdirs())
            Logger.logError(LOG_TAG, "Failed to create directory: " + path);
    }

    private String readFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append('\n');
        }
        return builder.toString();
    }

    private void writeFile(File file, String value) throws IOException {
        ensureDirectory(file.getParent());
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeExecutableFile(File file, String value) throws IOException {
        writeFile(file, value);
        if (!file.setReadable(true, true))
            Logger.logWarn(LOG_TAG, "Failed to set readable bit on " + file.getAbsolutePath());
        if (!file.setExecutable(true, true))
            Logger.logWarn(LOG_TAG, "Failed to set executable bit on " + file.getAbsolutePath());
    }

    private void setInputEnabled(boolean enabled) {
        boolean available = enabled && mBootstrapReady && mRuntimeReady && !mPromptRunning;
        if (mPromptInput != null) mPromptInput.setEnabled(available);
        if (mAttachButton != null) mAttachButton.setEnabled(available);
        if (mSendButton != null) mSendButton.setEnabled(available);
        boolean toolsAvailable = mBootstrapReady && mActiveSession != null;
        if (mTerminalToolButton != null) mTerminalToolButton.setEnabled(toolsAvailable);
        if (mDisplayToolButton != null) mDisplayToolButton.setEnabled(toolsAvailable);
        if (mHeaderDisplayButton != null) mHeaderDisplayButton.setEnabled(toolsAvailable);
        updateComposerTools();
    }

    private void updateComposerTools() {
        if (mTerminalToolButton != null) {
            mTerminalToolButton.setText(mMode == MODE_TERMINAL ? "Chat" : "Shell");
            styleModeButton(mTerminalToolButton, mMode == MODE_TERMINAL);
        }
        if (mDisplayToolButton != null) {
            mDisplayToolButton.setText(mMode == MODE_DISPLAY ? "Chat" : "View");
            styleModeButton(mDisplayToolButton, mMode == MODE_DISPLAY);
        }
        if (mHeaderDisplayButton != null) styleHeaderButton(mHeaderDisplayButton, mMode == MODE_DISPLAY);
    }

    private void setStatus(String status) {
        if (mStatusView != null) mStatusView.setText(status);
    }

    private void scrollToBottom() {
        if (mScrollView == null) return;
        mScrollView.post(() -> mScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void animateModeView(View view) {
        view.setAlpha(0f);
        view.setTranslationY(mMode == MODE_DISPLAY ? dp(18) : dp(10));
        if (mMode == MODE_DISPLAY) {
            view.setScaleX(0.985f);
            view.setScaleY(0.985f);
        }
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(mMode == MODE_DISPLAY ? 240 : 180)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
            .start();
    }

    private void updateAppChromeForMode() {
        boolean visible = mMode != MODE_DISPLAY;
        animateChromeVisibility(mHeaderView, visible, -dp(8));
        animateChromeVisibility(mComposerView, visible, dp(14));
    }

    private void animateChromeVisibility(View view, boolean visible, int hiddenOffset) {
        if (view == null) return;
        view.animate().cancel();
        if (visible) {
            boolean wasHidden = view.getVisibility() != View.VISIBLE || view.getAlpha() < 1f;
            view.setVisibility(View.VISIBLE);
            if (wasHidden) {
                view.setAlpha(0f);
                view.setTranslationY(hiddenOffset);
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(190)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.7f))
                    .start();
            } else {
                view.setAlpha(1f);
                view.setTranslationY(0f);
            }
            return;
        }

        if (view.getVisibility() != View.VISIBLE) return;
        view.animate()
            .alpha(0f)
            .translationY(hiddenOffset)
            .setDuration(140)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                if (mMode == MODE_DISPLAY) view.setVisibility(View.GONE);
                view.setAlpha(1f);
                view.setTranslationY(0f);
            })
            .start();
    }

    private void animateOverlayView(View view) {
        view.setAlpha(0f);
        view.setTranslationY(-dp(10));
        view.animate()
            .alpha(0.96f)
            .translationY(0f)
            .setDuration(260)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f))
            .start();
    }

    private String getDisplayGeometry() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = 540;
        int height = Math.round(width * (metrics.heightPixels / (float) Math.max(1, metrics.widthPixels)));
        height = Math.max(900, Math.min(1280, height));
        return width + "x" + height + "x24";
    }

    private Button createToolbarButton(String label) {
        UiSpec ui = ui();
        Button button = new Button(this);
        button.setText(label);
        normalizeButton(button);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(ui.toolbarButton.text);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(makeSurfaceDrawable(ui.toolbarButton, true));
        attachNativeRipple(button);
        return button;
    }

    private Button createToolbarIconButton(String label) {
        Button button = createToolbarButton(label);
        button.setTextSize(21);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(0, 0, 0, dp(1));
        return button;
    }

    private void styleHeaderButton(Button button, boolean active) {
        UiSpec ui = ui();
        SurfaceSpec surface = active ? ui.toolbarButtonActive : ui.toolbarButton;
        button.setTextColor(surface.text);
        button.setBackground(makeSurfaceDrawable(surface, true));
        attachNativeRipple(button);
    }

    private Button createComposerIconButton(String label) {
        UiSpec ui = ui();
        Button button = createSecondaryButton(label);
        button.setTextSize(28);
        button.setTypeface(Typeface.DEFAULT);
        button.setPadding(0, 0, 0, dp(3));
        button.setTextColor(ui.composerIcon.text);
        button.setBackground(makeSurfaceDrawable(ui.composerIcon, true));
        attachNativeRipple(button);
        return button;
    }

    private Button createComposerSendButton(String label) {
        UiSpec ui = ui();
        Button button = new Button(this);
        button.setText(label);
        normalizeButton(button);
        button.setAllCaps(false);
        button.setTextColor(ui.composerSend.text);
        button.setTextSize(24);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(0, 0, 0, dp(2));
        button.setBackground(makeSurfaceDrawable(ui.composerSend, true));
        attachNativeRipple(button);
        return button;
    }

    private Button createModeButton(String label, boolean active) {
        Button button = new Button(this);
        button.setText(label);
        normalizeButton(button);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(dp(34));
        button.setMinimumHeight(dp(34));
        button.setPadding(dp(10), 0, dp(10), 0);
        styleModeButton(button, active);
        return button;
    }

    private void styleModeButton(Button button, boolean active) {
        UiSpec ui = ui();
        SurfaceSpec surface = active ? ui.modeButtonActive : ui.modeButton;
        button.setTextColor(surface.text);
        button.setBackground(makeSurfaceDrawable(surface, true));
        attachNativeRipple(button);
    }

    private Button createAccentButton(String label) {
        UiSpec ui = ui();
        Button button = new Button(this);
        button.setText(label);
        normalizeButton(button);
        button.setAllCaps(false);
        button.setTextColor(ui.buttonPrimary.text);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(makeSurfaceDrawable(ui.buttonPrimary, true));
        attachNativeRipple(button);
        return button;
    }

    private Button createSecondaryButton(String label) {
        UiSpec ui = ui();
        Button button = new Button(this);
        button.setText(label);
        normalizeButton(button);
        button.setAllCaps(false);
        button.setTextColor(ui.buttonSecondary.text);
        button.setTextSize(15);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(makeSurfaceDrawable(ui.buttonSecondary, true));
        attachNativeRipple(button);
        return button;
    }

    private void normalizeButton(Button button) {
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setIncludeFontPadding(false);
        button.setStateListAnimator(null);
    }

    private TextView terminalMetaText(String text) {
        UiSpec ui = ui();
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ui.muted);
        view.setTextSize(13);
        view.setTextIsSelectable(true);
        return view;
    }

    private TextView terminalBlock(String text) {
        UiSpec ui = ui();
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ui.terminalBlock.text);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(13);
        view.setTextIsSelectable(true);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setBackground(makeSurfaceDrawable(ui.terminalBlock, false));
        return view;
    }

    private Drawable makeSurfaceDrawable(SurfaceSpec surface, boolean polished) {
        return makeNativeSurfaceDrawable(surface.fill, surface.stroke, dp(surface.radiusDp), polished);
    }

    private Drawable makeNativeSurfaceDrawable(int color, int strokeColor, int radius, boolean polished) {
        return makeRoundedDrawable(color, strokeColor, radius);
    }

    private void attachNativeRipple(View view) {
        if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Drawable content = view.getBackground();
        if (content == null || content instanceof RippleDrawable) return;
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(44, 255, 255, 255)), content, null));
    }

    private GradientDrawable makeRoundedDrawable(int color, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private BrandSkin skin() {
        return mSkin != null ? mSkin : BRAND_SKINS[0];
    }

    private BrandSkin findSkin(String id) {
        if (id != null) {
            for (BrandSkin skin : BRAND_SKINS) {
                if (id.equals(skin.id)) return skin;
            }
        }
        return BRAND_SKINS[0];
    }

    private int indexOfSkin(BrandSkin selected) {
        for (int i = 0; i < BRAND_SKINS.length; i++) {
            if (BRAND_SKINS[i] == selected || BRAND_SKINS[i].id.equals(selected.id)) return i;
        }
        return 0;
    }

    private String nowLabel() {
        return mClockFormat.format(new Date());
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class BrandMarkView extends View {
        private final BrandSkin skin;
        private final boolean compact;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        BrandMarkView(Context context, BrandSkin skin, boolean compact) {
            super(context);
            this.skin = skin;
            this.compact = compact;
            setContentDescription(skin.name + " symbol");
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            if (size <= 0) return;
            float s = size / 100f;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(compact ? skin.accent : skin.dark);
            rect.set(cx - 45f * s, cy - 45f * s, cx + 45f * s, cy + 45f * s);
            canvas.drawRoundRect(rect, 24f * s, 24f * s, paint);

            if ("local".equals(skin.id)) {
                drawLocal(canvas, cx, cy, s);
            } else if ("ssh".equals(skin.id)) {
                drawSsh(canvas, cx, cy, s);
            } else if ("custom".equals(skin.id)) {
                drawCustom(canvas, cx, cy, s);
            } else {
                drawCodex(canvas, cx, cy, s);
            }
        }

        private void drawCodex(Canvas canvas, float cx, float cy, float s) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(7f * s);
            paint.setColor(skin.onDark);

            for (int i = 0; i < 6; i++) {
                canvas.save();
                canvas.rotate(i * 60f, cx, cy);
                rect.set(cx - 8f * s, cy - 33f * s, cx + 8f * s, cy + 12f * s);
                canvas.drawRoundRect(rect, 9f * s, 9f * s, paint);
                canvas.restore();
            }

            paint.setStrokeWidth(4f * s);
            paint.setColor(skin.accent);
            canvas.drawCircle(cx, cy, 19f * s, paint);
        }

        private void drawLocal(Canvas canvas, float cx, float cy, float s) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(5f * s);
            paint.setColor(skin.onDark);
            rect.set(cx - 30f * s, cy - 22f * s, cx + 30f * s, cy + 23f * s);
            canvas.drawRoundRect(rect, 8f * s, 8f * s, paint);
            paint.setStrokeWidth(5f * s);
            canvas.drawLine(cx - 20f * s, cy - 4f * s, cx - 8f * s, cy + 5f * s, paint);
            canvas.drawLine(cx - 20f * s, cy + 14f * s, cx - 8f * s, cy + 5f * s, paint);
            canvas.drawLine(cx + 2f * s, cy + 14f * s, cx + 22f * s, cy + 14f * s, paint);
        }

        private void drawSsh(Canvas canvas, float cx, float cy, float s) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(6f * s);
            paint.setColor(skin.onDark);
            canvas.drawLine(cx - 17f * s, cy - 2f * s, cx + 17f * s, cy + 2f * s, paint);
            canvas.drawCircle(cx - 25f * s, cy - 6f * s, 13f * s, paint);
            canvas.drawCircle(cx + 25f * s, cy + 6f * s, 13f * s, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(skin.accent);
            canvas.drawCircle(cx - 25f * s, cy - 6f * s, 5f * s, paint);
            canvas.drawCircle(cx + 25f * s, cy + 6f * s, 5f * s, paint);
        }

        private void drawCustom(Canvas canvas, float cx, float cy, float s) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(6f * s);
            paint.setColor(skin.onDark);
            canvas.drawCircle(cx, cy, 27f * s, paint);
            paint.setColor(skin.accent);
            paint.setStrokeWidth(5f * s);
            canvas.drawLine(cx - 17f * s, cy, cx + 17f * s, cy, paint);
            canvas.drawLine(cx, cy - 17f * s, cx, cy + 17f * s, paint);
        }
    }

    private static final class UiSpec {
        final int canvas;
        final int panel;
        final int panelSoft;
        final int ink;
        final int muted;
        final int accent;
        final int accentDark;
        final int border;
        final int dark;
        final int onDark;
        final int onDarkMuted;
        final SurfaceSpec app;
        final SurfaceSpec header;
        final SurfaceSpec toolbarButton;
        final SurfaceSpec toolbarButtonActive;
        final SurfaceSpec drawer;
        final SurfaceSpec drawerSearch;
        final SurfaceSpec drawerRow;
        final SurfaceSpec drawerRowActive;
        final SurfaceSpec chat;
        final SurfaceSpec bubbleUser;
        final SurfaceSpec bubbleAgent;
        final SurfaceSpec composer;
        final SurfaceSpec composerInput;
        final SurfaceSpec composerIcon;
        final SurfaceSpec composerSend;
        final SurfaceSpec buttonPrimary;
        final SurfaceSpec buttonSecondary;
        final SurfaceSpec modeButton;
        final SurfaceSpec modeButtonActive;
        final SurfaceSpec terminalBlock;
        final SurfaceSpec displayHome;
        final SurfaceSpec displayTile;

        private UiSpec(int canvas, int panel, int panelSoft, int ink, int muted, int accent,
                       int accentDark, int border, int dark, int onDark, int onDarkMuted,
                       SurfaceSpec app, SurfaceSpec header, SurfaceSpec toolbarButton,
                       SurfaceSpec toolbarButtonActive, SurfaceSpec drawer, SurfaceSpec drawerSearch,
                       SurfaceSpec drawerRow, SurfaceSpec drawerRowActive, SurfaceSpec chat,
                       SurfaceSpec bubbleUser, SurfaceSpec bubbleAgent, SurfaceSpec composer,
                       SurfaceSpec composerInput, SurfaceSpec composerIcon, SurfaceSpec composerSend,
                       SurfaceSpec buttonPrimary, SurfaceSpec buttonSecondary,
                       SurfaceSpec modeButton, SurfaceSpec modeButtonActive,
                       SurfaceSpec terminalBlock, SurfaceSpec displayHome, SurfaceSpec displayTile) {
            this.canvas = canvas;
            this.panel = panel;
            this.panelSoft = panelSoft;
            this.ink = ink;
            this.muted = muted;
            this.accent = accent;
            this.accentDark = accentDark;
            this.border = border;
            this.dark = dark;
            this.onDark = onDark;
            this.onDarkMuted = onDarkMuted;
            this.app = app;
            this.header = header;
            this.toolbarButton = toolbarButton;
            this.toolbarButtonActive = toolbarButtonActive;
            this.drawer = drawer;
            this.drawerSearch = drawerSearch;
            this.drawerRow = drawerRow;
            this.drawerRowActive = drawerRowActive;
            this.chat = chat;
            this.bubbleUser = bubbleUser;
            this.bubbleAgent = bubbleAgent;
            this.composer = composer;
            this.composerInput = composerInput;
            this.composerIcon = composerIcon;
            this.composerSend = composerSend;
            this.buttonPrimary = buttonPrimary;
            this.buttonSecondary = buttonSecondary;
            this.modeButton = modeButton;
            this.modeButtonActive = modeButtonActive;
            this.terminalBlock = terminalBlock;
            this.displayHome = displayHome;
            this.displayTile = displayTile;
        }

        static UiSpec defaults(BrandSkin skin) {
            return fromProperties(skin, new Properties());
        }

        static UiSpec fromProperties(BrandSkin skin, Properties properties) {
            int canvas = readColor(properties, "color.canvas", skin.canvas);
            int panel = readColor(properties, "color.panel", skin.panel);
            int panelSoft = readColor(properties, "color.panelSoft", mix(panel, canvas, 3, 1));
            int ink = readColor(properties, "color.ink", skin.ink);
            int muted = readColor(properties, "color.muted", skin.muted);
            int accent = readColor(properties, "color.accent", skin.accent);
            int accentDark = readColor(properties, "color.accentDark", skin.accentDark);
            int border = readColor(properties, "color.border", skin.border);
            int dark = readColor(properties, "color.dark", skin.dark);
            int onDark = readColor(properties, "color.onDark", skin.onDark);
            int onDarkMuted = readColor(properties, "color.onDarkMuted", mix(onDark, dark, 3, 1));

            SurfaceSpec app = SurfaceSpec.fromProperties(properties, "surface.app",
                new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0));
            SurfaceSpec header = SurfaceSpec.fromProperties(properties, "surface.header",
                new SurfaceSpec(dark, border, onDark, 0));
            SurfaceSpec toolbarButton = SurfaceSpec.fromProperties(properties, "surface.toolbarButton",
                new SurfaceSpec(accentDark, border, onDark, 14));
            SurfaceSpec toolbarButtonActive = SurfaceSpec.fromProperties(properties, "surface.toolbarButtonActive",
                new SurfaceSpec(accent, onDarkMuted, onDark, 14));
            SurfaceSpec drawer = SurfaceSpec.fromProperties(properties, "surface.drawer",
                new SurfaceSpec(dark, Color.TRANSPARENT, onDark, 0));
            SurfaceSpec drawerSearch = SurfaceSpec.fromProperties(properties, "surface.drawerSearch",
                new SurfaceSpec(Color.rgb(18, 19, 21), Color.rgb(48, 49, 52), onDark, 22));
            SurfaceSpec drawerRow = SurfaceSpec.fromProperties(properties, "surface.drawerRow",
                new SurfaceSpec(dark, Color.TRANSPARENT, ink, 8));
            SurfaceSpec drawerRowActive = SurfaceSpec.fromProperties(properties, "surface.drawerRowActive",
                new SurfaceSpec(accent, onDarkMuted, onDark, 8));
            SurfaceSpec chat = SurfaceSpec.fromProperties(properties, "surface.chat",
                new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0));
            SurfaceSpec bubbleUser = SurfaceSpec.fromProperties(properties, "surface.bubble.user",
                new SurfaceSpec(accent, accent, onDark, 14));
            SurfaceSpec bubbleAgent = SurfaceSpec.fromProperties(properties, "surface.bubble.agent",
                new SurfaceSpec(panel, border, ink, 14));
            SurfaceSpec composer = SurfaceSpec.fromProperties(properties, "surface.composer",
                new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0));
            SurfaceSpec composerInput = SurfaceSpec.fromProperties(properties, "surface.composerInput",
                new SurfaceSpec(COLOR_INPUT_GLASS, Color.rgb(58, 59, 63), ink, 32));
            SurfaceSpec composerIcon = SurfaceSpec.fromProperties(properties, "surface.composerIcon",
                new SurfaceSpec(Color.rgb(12, 13, 14), Color.rgb(42, 43, 46), ink, 21));
            SurfaceSpec composerSend = SurfaceSpec.fromProperties(properties, "surface.composerSend",
                new SurfaceSpec(onDark, Color.WHITE, dark, 22));
            SurfaceSpec buttonPrimary = SurfaceSpec.fromProperties(properties, "surface.buttonPrimary",
                new SurfaceSpec(accent, onDarkMuted, onDark, 14));
            SurfaceSpec buttonSecondary = SurfaceSpec.fromProperties(properties, "surface.buttonSecondary",
                new SurfaceSpec(panel, border, ink, 14));
            SurfaceSpec modeButton = SurfaceSpec.fromProperties(properties, "surface.modeButton",
                new SurfaceSpec(panel, border, ink, 12));
            SurfaceSpec modeButtonActive = SurfaceSpec.fromProperties(properties, "surface.modeButtonActive",
                new SurfaceSpec(accent, onDarkMuted, onDark, 12));
            SurfaceSpec terminalBlock = SurfaceSpec.fromProperties(properties, "surface.terminalBlock",
                new SurfaceSpec(panelSoft, border, ink, 12));
            SurfaceSpec displayHome = SurfaceSpec.fromProperties(properties, "surface.displayHome",
                new SurfaceSpec(Color.rgb(5, 5, 6), Color.TRANSPARENT, onDark, 0));
            SurfaceSpec displayTile = SurfaceSpec.fromProperties(properties, "surface.displayTile",
                new SurfaceSpec(Color.rgb(26, 27, 30), Color.rgb(45, 47, 52), onDark, 18));

            return new UiSpec(canvas, panel, panelSoft, ink, muted, accent, accentDark, border,
                dark, onDark, onDarkMuted, app, header, toolbarButton, toolbarButtonActive,
                drawer, drawerSearch, drawerRow, drawerRowActive, chat, bubbleUser, bubbleAgent,
                composer, composerInput, composerIcon, composerSend, buttonPrimary, buttonSecondary,
                modeButton, modeButtonActive, terminalBlock, displayHome, displayTile);
        }

        private static int readColor(Properties properties, String key, int fallback) {
            String value = properties.getProperty(key);
            if (value == null) return fallback;
            String text = value.trim();
            if (text.isEmpty()) return fallback;
            try {
                if (text.startsWith("0x") || text.startsWith("0X")) {
                    String hex = text.substring(2);
                    long parsed = Long.parseLong(hex, 16);
                    if (hex.length() <= 6) parsed |= 0xff000000L;
                    return (int) parsed;
                }
                return Color.parseColor(text);
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }

        private static int readInt(Properties properties, String key, int fallback) {
            String value = properties.getProperty(key);
            if (value == null) return fallback;
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static int mix(int a, int b, int aWeight, int bWeight) {
            int total = aWeight + bWeight;
            return Color.rgb(
                (Color.red(a) * aWeight + Color.red(b) * bWeight) / total,
                (Color.green(a) * aWeight + Color.green(b) * bWeight) / total,
                (Color.blue(a) * aWeight + Color.blue(b) * bWeight) / total);
        }
    }

    private static final class SurfaceSpec {
        final int fill;
        final int stroke;
        final int text;
        final int radiusDp;

        SurfaceSpec(int fill, int stroke, int text, int radiusDp) {
            this.fill = fill;
            this.stroke = stroke;
            this.text = text;
            this.radiusDp = radiusDp;
        }

        static SurfaceSpec fromProperties(Properties properties, String key, SurfaceSpec fallback) {
            int fill = UiSpec.readColor(properties, key + ".fill", fallback.fill);
            fill = UiSpec.readColor(properties, key + ".background", fill);
            int stroke = UiSpec.readColor(properties, key + ".stroke", fallback.stroke);
            stroke = UiSpec.readColor(properties, key + ".border", stroke);
            int text = UiSpec.readColor(properties, key + ".text", fallback.text);
            int radiusDp = UiSpec.readInt(properties, key + ".radius", fallback.radiusDp);
            return new SurfaceSpec(fill, stroke, text, Math.max(0, radiusDp));
        }
    }

    private static final class BrandSkin {
        final String id;
        final String name;
        final String mark;
        final String tagline;
        final int canvas;
        final int panel;
        final int panelSoft;
        final int ink;
        final int muted;
        final int accent;
        final int accentDark;
        final int border;
        final int dark;
        final int onDark;
        final int onDarkMuted;

        BrandSkin(String id, String name, String mark, String tagline, int canvas, int panel, int ink,
                  int muted, int accent, int accentDark, int border, int dark, int onDark) {
            this.id = id;
            this.name = name;
            this.mark = mark;
            this.tagline = tagline;
            this.canvas = canvas;
            this.panel = panel;
            this.panelSoft = mix(panel, canvas, 3, 1);
            this.ink = ink;
            this.muted = muted;
            this.accent = accent;
            this.accentDark = accentDark;
            this.border = border;
            this.dark = dark;
            this.onDark = onDark;
            this.onDarkMuted = mix(onDark, dark, 3, 1);
        }

        private static int mix(int a, int b, int aWeight, int bWeight) {
            int total = aWeight + bWeight;
            return Color.rgb(
                (Color.red(a) * aWeight + Color.red(b) * bWeight) / total,
                (Color.green(a) * aWeight + Color.green(b) * bWeight) / total,
                (Color.blue(a) * aWeight + Color.blue(b) * bWeight) / total);
        }
    }

    private static final class ChatSession {
        final String id;
        final long createdAt;
        final String rootPath;
        final String workspacePath;
        final String historyPath;
        final ArrayList<ChatMessage> messages = new ArrayList<>();
        String title;

        ChatSession(String id, String title, long createdAt, String rootPath) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
            this.rootPath = rootPath;
            this.workspacePath = rootPath + "/workspace";
            this.historyPath = rootPath + "/" + HISTORY_FILE_NAME;
        }

        String terminalName() {
            return "ominal-" + id;
        }
    }

    private static final class ChatMessage {
        final String role;
        final String text;
        final String timestamp;

        ChatMessage(String role, String text, String timestamp) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
        }
    }
}
