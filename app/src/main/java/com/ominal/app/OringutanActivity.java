package com.ominal.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.ominal.BuildConfig;
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
import com.ominal.x11.LorieView;
import com.ominal.x11.OminalNativeDisplay;

import io.noties.markwon.Markwon;

import org.json.JSONArray;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Prototype chatbot front-end for driving a coding agent inside the Ominal execution environment.
 */
public final class OringutanActivity extends AppCompatActivity
    implements OminalAgentRuntime.Observer {

    private static final String LOG_TAG = "OringutanActivity";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;
    private static final String EXTRA_DEBUG_SHOW_PAIRING = "com.ominal.debug.SHOW_PAIRING";

    private static final String PREFS_NAME = "ominal_state";
    private static final String PREF_RUNNER_PAIRING_COMPLETE = "runner_pairing_complete";
    private static final String PREF_LAST_TERMINAL_HARNESS = "last_terminal_harness";
    private static final String PREF_CODEX_REAUTH_REQUIRED = "codex_reauth_required";
    private static final String PREF_ACTIVE_CHAT_ID = "active_chat_id";
    private static final String PREF_COMPOSER_DRAFT = "composer_draft";
    private static final String PREF_LOLO_MODE_ENABLED = "lolo_mode_enabled";
    private static final String PREF_LIGHT_APPEARANCE = "light_appearance";
    private static final String STATE_MODE = "mode";
    private static final String DISPLAY_CLOSE_REQUEST_FILE_NAME =
        "ominal-display-close.request";
    private static final String LAUNCHER_LIGHT_COMPONENT = "com.ominal.LauncherLightV3";
    private static final String LAUNCHER_DARK_COMPONENT = "com.ominal.LauncherDarkV3";
    private static final String CHAT_ROOT_NAME = ".ominal/chats";
    private static final String UI_CONFIG_FILE_NAME = ".ominal/ui.properties";
    private static final String UI_RC_FILE_NAME = ".ominalrc";
    private static final String UI_CONFIG_VERSION = "ominal-closed-v6";
    private static final String ATTACHMENTS_DIR_NAME = "attachments";
    private static final String MEDIA_DIR_NAME = "media";
    private static final String AGENT_RUNTIME_DIR_NAME = ".ominal";
    private static final String AGENT_RUNTIME_CONTRACT_NAME = "runtime.json";
    private static final String AGENT_EVENT_LOG_NAME = "events.jsonl";
    private static final String DISPLAY_DIR_NAME = "display";
    private static final String DISPLAY_ACTIVITY_FILE_NAME = "ominal-display-activity.json";
    private static final String DISPLAY_URL = "http://127.0.0.1:6080/ominal.html";
    private static final String PRIVACY_POLICY_URL =
        "https://snashoan.github.io/ominal/privacy/";
    private static final String DISPLAY_START_COMMAND = "command -v ominal-display-start >/dev/null 2>&1 && ominal-display-start || printf 'Install $PREFIX/bin/ominal-display-start first.\\n'";
    private static final int DISPLAY_HEALTH_RETRIES = 30;
    private static final int DISPLAY_HEALTH_RETRY_DELAY_MS = 300;
    private static final int NATIVE_DISPLAY_HEALTH_RETRIES = 240;
    private static final String NODE_VERSION = "24.18.0";
    private static final String CODEX_VERSION = "0.144.6";
    private static final String ROOTFS_ASSET = "runtime/archives/ubuntu-base-24.04.4-arm64-nohardlinks.tgz";
    private static final String ROOTFS_SHA256 = "8ae01fcddd133998b050e90119bee3a772b7a28bb50d700f8acf3d95ddb27d7e";
    private static final String NODE_SHA256 = "6b4484c2190274175df9aa8f28e2d758a819cb1c1fe6ab481e2f95b463ab8508";
    private static final String CODEX_CORE_SHA256 = "779eab25aa8473583b3d1d6f9316a0ab8d0643fdfd0bfef80ce76cc8cf85e401";
    private static final String CODEX_ARM64_SHA256 = "19f0b01b33f273df94191670b2e0e5d0f624b0354e765bfdea5763920b713800";
    private static final String NODE_URL = "https://nodejs.org/dist/v" + NODE_VERSION
        + "/node-v" + NODE_VERSION + "-linux-arm64.tar.gz";
    private static final String CODEX_CORE_URL = "https://registry.npmjs.org/@openai/codex/-/codex-"
        + CODEX_VERSION + ".tgz";
    private static final String CODEX_ARM64_URL = "https://registry.npmjs.org/@openai/codex/-/codex-"
        + CODEX_VERSION + "-linux-arm64.tgz";
    private static final long MIN_RUNTIME_FREE_BYTES = 4L * 1024L * 1024L * 1024L;
    private static final String DISPLAY_USER_INPUT_MARKER = "OMINAL_NEEDS_USER_INPUT";
    private static final String DISPLAY_OPEN_MARKER = "OMINAL_OPEN_DISPLAY";
    private static final String OMINAL_MOTD =
        "\u001b[1;97mΩ_\u001b[0m  \u001b[1mMONOLITH\u001b[0m\n"
            + "\u001b[2mThe last interface to your computer.\u001b[0m\n\n"
            + "\u001b[1mWorkspace\u001b[0m\n"
            + "  Files      ~/workspace\n"
            + "  Screen     open from the display button\n"
            + "  Command    long-press Send in a chat\n\n"
            + "\u001b[1mWorking with packages\u001b[0m\n"
            + "  Search     apt search <query>\n"
            + "  Install    apt install <package>\n"
            + "  Upgrade    apt update && apt upgrade\n\n";
    private static final String HISTORY_FILE_NAME = "history.jsonl";
    private static final String META_FILE_NAME = "meta.json";
    private static final int REQUEST_ATTACH_FILE = 1001;
    private static final int REQUEST_IMPORT_TERMUX_CONFIG = 1003;
    private static final int MODE_CHAT = 0;
    private static final int MODE_TERMINAL = 1;
    private static final int MODE_DISPLAY = 2;
    private static final String DISPLAY_STATE_OFF = "off";
    private static final String DISPLAY_STATE_STARTING = "starting";
    private static final String DISPLAY_STATE_READY_IDLE = "ready_idle";
    private static final String DISPLAY_STATE_AGENT_ACTIVE = "agent_active";
    private static final String DISPLAY_STATE_NEEDS_USER = "needs_user";
    private static final String DISPLAY_STATE_ERROR = "error";

    private static final int COLOR_CANVAS = Color.BLACK;
    private static final int COLOR_PANEL = Color.rgb(14, 14, 14);
    private static final int COLOR_ACCENT = Color.WHITE;
    private static final int COLOR_ACCENT_DARK = Color.BLACK;
    private static final int COLOR_BORDER = Color.rgb(40, 40, 40);
    private static final int COLOR_INPUT_GLASS = Color.rgb(18, 18, 18);

    private static final BrandSkin[] BRAND_SKINS = new BrandSkin[]{
        new BrandSkin("ominal", "Monolith", "Ω_", "",
            COLOR_CANVAS, COLOR_PANEL, Color.rgb(242, 242, 242),
            Color.rgb(148, 148, 148), COLOR_ACCENT, COLOR_ACCENT_DARK,
            COLOR_BORDER, Color.BLACK, Color.rgb(242, 242, 242))
    };

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
    private ImageButton mHarnessControlsButton;
    private TextView mHarnessContextView;
    private LinearLayout mCommandSuggestionsView;
    private LinearLayout mCommandSuggestionsRow;
    private ImageButton mAttachButton;
    private ImageButton mSendButton;
    private TextView mSubtitleView;
    private TextView mStatusView;
    private WebView mDisplayWebView;
    private LorieView mNativeDisplayView;
    private View mDisplayPane;
    private FrameLayout mDisplayWarmHost;
    private TextView mDisplayAvailabilityView;
    private LinearLayout mDisplayAgentStatusView;
    private TextView mDisplayAgentStatusText;
    private WorkPulseView mDisplayAgentPulse;
    private View mDisplayActivityBorder;
    private Button mChatModeButton;
    private Button mTerminalModeButton;
    private Button mDisplayModeButton;
    private ImageButton mHeaderDisplayButton;
    private Button mSwapButton;
    private ImageButton mAccountButton;
    private ImageButton mLoloButton;
    private Button mTerminalToolButton;
    private Button mDisplayToolButton;
    private View mSetupOverlay;
    private View mPairingOverlay;
    private LinearLayout mPairingContent;
    private View mPairingCodexButton;
    private View mPairingClaudeButton;
    private View mPairingAntigravityButton;
    private View mPairingComputerOnlyButton;
    private SetupMarkView mSetupMarkView;
    private TextView mSetupStageView;
    private TextView mSetupTitleView;
    private TextView mSetupDetailView;
    private TextView mSetupPercentView;
    private RoundedSetupProgressView mSetupProgressView;
    private Button mSetupRetryButton;
    private Runnable mSetupRetryAction;
    private AgentTurnView mActiveAgentTurnView;
    private Markwon mMarkwon;

    private SharedPreferences mPrefs;
    private ChatSession mActiveSession;
    private OminalAgentRuntime mAgentRuntime;
    private BrandSkin mSkin = BRAND_SKINS[0];
    private UiSpec mUi = UiSpec.defaults(BRAND_SKINS[0]);
    private boolean mBootstrapReady;
    private boolean mRuntimeReady;
    private boolean mRuntimeSetupInFlight;
    private boolean mHarnessUpdateInFlight;
    private String mRuntimeSetupDetail = "";
    private boolean mPromptRunning;
    private boolean mDisplayStartInFlight;
    private boolean mReloadDisplayWhenReady;
    private boolean mDisplayReady;
    private boolean mDisplayUrlLoaded;
    private boolean mAgentUsingDisplay;
    private boolean mDisplayNeedsUser;
    private String mDisplayLifecycleState = DISPLAY_STATE_OFF;
    private boolean mPendingCodexTerminalLaunch;
    private boolean mCodexAccountDialogVisible;
    private Dialog mCodexAccountDialog;
    private boolean mCodexAuthRefreshInFlight;
    private boolean mCodexSignedIn;
    private boolean mCodexSessionExpired;
    private final LinkedHashSet<String> mHarnessDiscoveryInFlight = new LinkedHashSet<>();
    private final HashMap<String, String> mHarnessDiscoveryErrors = new HashMap<>();
    private ConnectivityManager.NetworkCallback mRuntimeNetworkCallback;
    private long mHandledAgentRevision = -1;
    private int mDisplayRetryCount;
    private long mDisplayLastStartedAt;
    private String mDisplayStartupDetail = "";
    private String mChatSearchQuery = "";
    private boolean mSplitReversed;
    private FileObserver mDisplayControlObserver;
    private FileObserver mDisplayActivityObserver;
    private FileObserver mAgentEventObserver;
    private String mDisplayActivitySessionId = "";
    private String mObservedAgentSessionId = "";
    private int mObservedAgentEventCount;
    private boolean mLauncherSyncPending;
    private float mSplitRatio = 0.52f;
    private int mMode = MODE_CHAT;
    private int mDisplayNavigationInsetLeft;
    private int mDisplayNavigationInsetTop;
    private int mDisplayNavigationInsetRight;
    private int mDisplayNavigationInsetBottom;
    private int mDisplaySystemInsetBottom;
    private int mDisplayImeInsetBottom;
    private boolean mDisplayInsetsReady;
    private int mChatInsetLeft;
    private int mChatInsetTop;
    private int mChatInsetRight;
    private int mChatInsetBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        getDelegate().setLocalNightMode(mPrefs.getBoolean(PREF_LIGHT_APPEARANCE, false)
            ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            int restoredMode = savedInstanceState.getInt(STATE_MODE, MODE_CHAT);
            if (restoredMode == MODE_CHAT || restoredMode == MODE_TERMINAL
                || restoredMode == MODE_DISPLAY) {
                mMode = restoredMode;
            }
        }
        mMarkwon = Markwon.create(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        configureBackNavigation();
        if (!usesNativeDisplay()
            && (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            WebView.setWebContentsDebuggingEnabled(true);

        mCodexSessionExpired = mPrefs.getBoolean(PREF_CODEX_REAUTH_REQUIRED, false);
        mSkin = BRAND_SKINS[0];
        ensureDefaultUiProperties();
        mUi = loadUiSpec();
        applySystemBars();
        registerRuntimeNetworkObserver();
        startWorkspace();
        maybeShowDebugPairing(getIntent());
        requestNotificationPermissionIfNeeded();
    }

    private void configureBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mMode == MODE_DISPLAY) {
                    switchMode(MODE_CHAT);
                    return;
                }
                if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        maybeShowDebugPairing(intent);
    }

    private void maybeShowDebugPairing(Intent intent) {
        if (!BuildConfig.DEBUG || intent == null
            || !intent.getBooleanExtra(EXTRA_DEBUG_SHOW_PAIRING, false)
            || mRootFrame == null) {
            return;
        }
        mRootFrame.postDelayed(this::showRunnerPairingPreview, 500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAgentRuntime != null) mAgentRuntime.addObserver(this);
    }

    @Override
    protected void onStop() {
        if (mAgentRuntime != null) mAgentRuntime.removeObserver(this);
        if (!isChangingConfigurations() && mLauncherSyncPending) {
            syncLauncherIcon(isLightAppearanceEnabled());
            mLauncherSyncPending = false;
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_MODE, mMode);
        super.onSaveInstanceState(outState);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private void startWorkspace() {
        setContentView(createContentView());
        configureDisplayImeInsets();
        showSetupProgress(0, "Preparing your workspace", "Checking this device", -1);

        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(this, true);
        if (preferences == null) {
            setInputEnabled(false);
            setStatus("Something went wrong");
            showSetupFailure("Saved settings could not be opened.", this::recreate);
            return;
        }

        setStatus("Getting things ready");
        setInputEnabled(false);
        showSetupProgress(1, "Installing system core", "Preparing essential command tools", -1);
        OminalInstaller.setupBootstrapIfNeeded(this, () -> {
            mBootstrapReady = true;
            refreshRuntimeDns();
            ensureDefaultUiProperties();
            ensureProviderCommands();
            ensureOminalMotd();
            mUi = loadUiSpec();
            loadOrCreateSessions();
            attachAgentRuntime();
            setStatus("Finishing setup");
            showSetupProgress(2, "Preparing Linux workspace", "Checking system files", -1);
            ensureRuntimeReady(() -> {
                mRuntimeReady = true;
                ensureProviderCommands();
                setInputEnabled(true);
                setStatus("Ready");
                completeSetupProgress();
                startDisplayActivityObserver();
                ensureDisplayServerStarted(false);
                resumePendingTurns();
                if (mRootFrame != null) mRootFrame.postDelayed(this::prewarmDisplaySurface, 1200);
                if (mPendingCodexTerminalLaunch) {
                    mPendingCodexTerminalLaunch = false;
                    if (mRootFrame != null)
                        mRootFrame.postDelayed(this::startCodexTerminal, 260);
                    else startCodexTerminal();
                } else {
                    refreshCodexAuthStatus(true);
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setDisplayFullscreen(mMode == MODE_DISPLAY);
        if (mMode == MODE_DISPLAY && mNativeDisplayView != null)
            mNativeDisplayView.activateInputBridge();
        if (mBootstrapReady) {
            if (mPairingOverlay != null && mPairingOverlay.getVisibility() == View.VISIBLE)
                setPairingBusy(false, "");
            refreshRuntimeDns();
            refreshCodexAuthStatus(false);
            restoreNativeDisplayConnection();
            if (mRuntimeReady && mActiveSession != null)
                refreshHarnessCapabilities(mActiveSession.harnessId);
        }
    }

    @Override
    protected void onPause() {
        releaseNativeDisplayInput();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopDisplayControlObserver();
        stopDisplayActivityObserver();
        stopAgentEventObserver();
        unregisterRuntimeNetworkObserver();
        if (mAgentRuntime != null) mAgentRuntime.removeObserver(this);
        releaseNativeDisplayInput();
        OminalNativeDisplay.release(mNativeDisplayView);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && mMode == MODE_DISPLAY) {
            setDisplayFullscreen(true);
            if (mNativeDisplayView != null) mNativeDisplayView.activateInputBridge();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ATTACH_FILE && resultCode == Activity.RESULT_OK && data != null)
            handleAttachmentResult(data);
        else if (requestCode == REQUEST_IMPORT_TERMUX_CONFIG && resultCode == Activity.RESULT_OK && data != null)
            handleTermuxConfigImport(data);
    }

    private OminalInteractionSheet.Theme interactionSheetTheme() {
        UiSpec ui = ui();
        return new OminalInteractionSheet.Theme(ui.panel, ui.panelSoft, ui.ink, ui.muted,
            ui.border, ui.accent, ui.accentDark);
    }

    private void showAppMenu() {
        String harnessId = mActiveSession == null
            ? OminalHarnessRegistry.DEFAULT_HARNESS_ID : mActiveSession.harnessId;
        OminalAgentHarness harness = OminalHarnessRegistry.activeOrDefault(harnessId);
        boolean codex = OminalHarnessTerminal.CODEX_ID.equals(harnessId);
        String accountState = codex && mCodexSessionExpired ? "Needs attention"
            : codex && mCodexSignedIn ? "Signed in" : "Manage";
        String appearance = isLightAppearanceEnabled() ? "Light" : "Dark";

        ArrayList<OminalInteractionSheet.Section> sections = new ArrayList<>();
        ArrayList<OminalInteractionSheet.Row> account = new ArrayList<>();
        account.add(new OminalInteractionSheet.Row(
            "agent", "Agent", "Choose the harness for this conversation",
            harness.getDisplayName(), false, true, false));
        account.add(new OminalInteractionSheet.Row(
            "account", harness.getDisplayName(), "Sign-in and provider settings",
            accountState, codex && mCodexSignedIn, true, false));
        sections.add(new OminalInteractionSheet.Section("Account", account));

        ArrayList<OminalInteractionSheet.Row> preferences = new ArrayList<>();
        preferences.add(new OminalInteractionSheet.Row("appearance", "Appearance",
            "Switch the interface theme", appearance, false, true, false));
        preferences.add(new OminalInteractionSheet.Row("lolo", "Lolo mode",
            "Experimental access outside the Linux workspace",
            isLoloModeEnabled() ? "On" : "Off", isLoloModeEnabled(), true, false));
        preferences.add(new OminalInteractionSheet.Row("terminal", "Chat terminal",
            "Open the persistent session for this conversation", "Open", false,
            mActiveSession != null, false));
        sections.add(new OminalInteractionSheet.Section("Preferences", preferences));

        ArrayList<OminalInteractionSheet.Row> information = new ArrayList<>();
        information.add(new OminalInteractionSheet.Row("privacy", "Privacy",
            "How app data and connected agents are handled", "Open", false, true, false));
        sections.add(new OminalInteractionSheet.Section("Information", information));

        OminalInteractionSheet.show(this, interactionSheetTheme(), "Settings",
            "Account and device controls.", sections, id -> {
                if ("agent".equals(id)) {
                    if (mRootFrame != null)
                        mRootFrame.postDelayed(this::showRunnerPairingChooser, 160);
                    else showRunnerPairingChooser();
                } else if ("account".equals(id)) {
                    if (mRootFrame != null) mRootFrame.postDelayed(this::showCodexAccountDialog, 160);
                    else showCodexAccountDialog();
                } else if ("appearance".equals(id)) {
                    setLightAppearanceEnabled(!isLightAppearanceEnabled());
                } else if ("lolo".equals(id)) {
                    if (mRootFrame != null) mRootFrame.postDelayed(this::showLoloModeDialog, 160);
                    else showLoloModeDialog();
                } else if ("terminal".equals(id)) {
                    openAgentTerminalForActiveChat();
                } else if ("privacy".equals(id)) {
                    startExternalActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(PRIVACY_POLICY_URL)));
                }
            });
    }

    private void showCodexAccountDialog() {
        if (mCodexAccountDialogVisible) return;
        mCodexAccountDialogVisible = true;
        String harnessId = mActiveSession == null
            ? OminalHarnessRegistry.DEFAULT_HARNESS_ID : mActiveSession.harnessId;
        OminalAgentHarness harness = OminalHarnessRegistry.activeOrDefault(harnessId);
        String harnessName = harness.getDisplayName();
        boolean codex = OminalHarnessTerminal.CODEX_ID.equals(harnessId);
        boolean sessionExpired = codex && mCodexSessionExpired;
        boolean signedIn = codex && mCodexSignedIn;

        String detail = sessionExpired
            ? "Your Codex session needs attention. Open Codex and sign in again in its terminal."
            : signedIn
            ? "Codex reports that this Linux workspace is signed in."
            : harnessName + " handles its account and settings directly.";

        ArrayList<OminalInteractionSheet.Row> rows = new ArrayList<>();
        rows.add(new OminalInteractionSheet.Row("open",
            signedIn && !sessionExpired ? "Check session" : "Open " + harnessName,
            codex ? "Continue in the agent's own sign-in flow" : "Open the agent terminal",
            "Open", false, true, false));

        if (codex && mCodexSignedIn) {
            rows.add(new OminalInteractionSheet.Row("logout", "Log out",
                "Remove the Codex session", "", false, true, true));
        }

        Dialog dialog = OminalInteractionSheet.show(this, interactionSheetTheme(),
            sessionExpired ? "Session expired" : harnessName, detail,
            Collections.singletonList(new OminalInteractionSheet.Section("Account", rows)), id -> {
                if ("logout".equals(id)) {
                    showCodexLogoutConfirmation();
                } else if (codex && signedIn && !sessionExpired) {
                    refreshCodexAuthStatus(false);
                } else {
                    launchHarnessTerminal(harnessId, false);
                }
            });
        mCodexAccountDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            mCodexAccountDialogVisible = false;
            mCodexAccountDialog = null;
        });
    }

    private void startCodexTerminal() {
        if (!mBootstrapReady || !mRuntimeReady || mActiveSession == null) {
            mPendingCodexTerminalLaunch = true;
            setStatus("Finishing setup");
            return;
        }

        ensureProviderCommands();
        refreshRuntimeDns();
        launchHarnessTerminal(OminalHarnessTerminal.CODEX_ID, false);
    }

    private void dismissCodexAccountDialog() {
        if (mCodexAccountDialog != null) mCodexAccountDialog.dismiss();
    }

    private void showCodexLogoutConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Log out of Codex?")
            .setMessage("Chats and workspace files will remain on this device.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log out", (ignored, which) -> performCodexLogout())
            .create();
        dialog.show();
        styleBlackDialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(255, 69, 58));
    }

    private void performCodexLogout() {
        if (!mRuntimeReady) {
            setStatus("Finishing setup");
            return;
        }
        setStatus("Signing out");
        new Thread(() -> {
            boolean signedOut = runCodexLogoutCommand();
            runOnUiThread(() -> {
                if (!signedOut) {
                    setStatus("Sign-out failed");
                    Toast.makeText(this, "Could not log out. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                mCodexSignedIn = false;
                setCodexSessionExpired(false);
                if (mPrefs != null)
                    mPrefs.edit().putBoolean(PREF_RUNNER_PAIRING_COMPLETE, true).apply();
                shutdownAgentRuntime();
                styleAccountButton();
                setStatus("Sign in");
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            });
        }, "ominal-codex-logout").start();
    }

    private boolean runCodexLogoutCommand() {
        ExecutionCommand command = new ExecutionCommand(-1,
            OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
            new String[]{"-lc", "codex logout"},
            null,
            mActiveSession == null ? OminalConstants.OMINAL_HOME_DIR_PATH : mActiveSession.workspacePath,
            ExecutionCommand.Runner.APP_SHELL.getName(),
            false);
        command.commandLabel = "Codex logout";
        AppShell.execute(this, command, null, new OminalShellEnvironment(), null, true);
        return !command.isStateFailed()
            && command.resultData.exitCode != null && command.resultData.exitCode == 0;
    }

    private void refreshCodexAuthStatus(boolean promptWhenSignedOut) {
        if (!mBootstrapReady || !mRuntimeReady || mActiveSession == null || mCodexAuthRefreshInFlight) return;
        if (!OminalHarnessTerminal.CODEX_ID.equals(mActiveSession.harnessId)) {
            styleAccountButton();
            setStatus("Ready");
            return;
        }
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
                boolean needsReauthentication = mCodexSessionExpired;
                mCodexSignedIn = signedIn && !needsReauthentication;
                styleAccountButton();
                setStatus(mCodexSignedIn ? "Signed in" : "Sign in");
                if (mCodexSignedIn) {
                    completeRunnerPairing(true);
                } else if (needsReauthentication && promptWhenSignedOut) {
                    if (mRootFrame != null)
                        mRootFrame.postDelayed(this::showCodexAccountDialog, 240);
                } else if (!signedIn && promptWhenSignedOut) {
                    if (mRootFrame != null) mRootFrame.postDelayed(this::showRunnerPairing, 240);
                }
                scheduleHarnessUpdates();
            });
        }).start();
    }

    private boolean requiresCodexLogin(String output) {
        return OminalCodexAppServer.isAuthenticationError(output);
    }

    private void setCodexSessionExpired(boolean expired) {
        mCodexSessionExpired = expired;
        if (mPrefs != null)
            mPrefs.edit().putBoolean(PREF_CODEX_REAUTH_REQUIRED, expired).apply();
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

    private UiSpec loadUiSpec() {
        Properties properties = new Properties();
        loadUiProperties(properties, uiConfigFile());
        loadUiProperties(properties, uiRcFile());
        return isLightAppearanceEnabled()
            ? UiSpec.light(skin()) : UiSpec.fromProperties(skin(), properties);
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
            extractRuntimeTool("runtime/ominal-proot-shell.sh",
                new File(binDir, "ominal-proot-shell"));
            extractRuntimeTool("runtime/ominal-harness-tui.sh",
                new File(binDir, "ominal-harness-tui"));
            extractRuntimeTool("runtime/ominal-harness-chat.sh",
                new File(binDir, "ominal-harness-chat"));
            extractRuntimeTool("runtime/ominal-harness-update.sh",
                new File(binDir, "ominal-harness-update"));
            extractRuntimeTool("runtime/ominal-harness-discover.py",
                new File(binDir, "ominal-harness-discover"));
            extractRuntimeTool("runtime/ominal-harness-hook.py",
                new File(binDir, "ominal-harness-hook"));
            extractRuntimeTool("runtime/ominal-xdg-open-guest.sh",
                new File(binDir, "ominal-xdg-open-guest"));
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
            extractRuntimeTool("runtime/ominal-screen-guest.sh",
                new File(binDir, "ominal-screen-guest"));
            extractRuntimeTool("runtime/ominal-event-guest.sh",
                new File(binDir, "ominal-event-guest"));
            extractRuntimeTool("runtime/ominal-device-guest.sh",
                new File(binDir, "ominal-device-guest"));
            extractRuntimeTool("runtime/ominal-package-guest.sh",
                new File(binDir, "ominal-package-guest"));
            extractRuntimeTool("runtime/ominal-runtime-bootstrap.sh",
                new File(binDir, "ominal-runtime-bootstrap"));
            extractRuntimeTool("runtime/ominal-display-start.sh", new File(binDir, "ominal-display-start"));
            extractRuntimeTool("runtime/ominal-xfce-session.sh", new File(binDir, "ominal-xfce-session"));
            writeExecutableFile(new File(binDir, "ominal-codex"),
                "#!/data/data/com.ominal/files/usr/bin/sh\n"
                    + "PREFIX=\"${PREFIX:-/data/data/com.ominal/files/usr}\"\n"
                    + "exec \"$PREFIX/bin/ominal-proot-codex\" exec "
                    + "--sandbox danger-full-access --ask-for-approval never "
                    + "--skip-git-repo-check -- \"$@\"\n");
            writeExecutableFile(new File(binDir, "codex"),
                "#!/data/data/com.ominal/files/usr/bin/sh\n"
                    + "PREFIX=\"${PREFIX:-/data/data/com.ominal/files/usr}\"\n"
                    + "exec \"$PREFIX/bin/ominal-proot-codex\" \"$@\"\n");

            File rootfs = new File(OminalConstants.OMINAL_HOME_DIR_PATH,
                ".ominal/runtime/linux/rootfs");
            if (rootfs.isDirectory()) {
                File guestBin = new File(rootfs, "usr/local/bin");
                ensureDirectory(guestBin.getAbsolutePath());
                extractRuntimeTool("runtime/ominal-screen-guest.sh",
                    new File(guestBin, "ominal-screen"));
                extractRuntimeTool("runtime/ominal-event-guest.sh",
                    new File(guestBin, "ominal-event"));
                extractRuntimeTool("runtime/ominal-device-guest.sh",
                    new File(guestBin, "ominal-device"));
                extractRuntimeTool("runtime/ominal-package-guest.sh",
                    new File(guestBin, "ominal-install"));
                extractRuntimeTool("runtime/ominal-harness-hook.py",
                    new File(guestBin, "ominal-harness-hook"));
            }

            File legacyNativeProvider = new File(binDir, "codex.real");
            if (legacyNativeProvider.exists() && !legacyNativeProvider.delete())
                Logger.logWarn(LOG_TAG, "Could not remove retired native Codex provider");
            File legacyArm64Provider = new File(binDir, "codex-aarch64");
            if (legacyArm64Provider.exists() && !legacyArm64Provider.delete())
                Logger.logWarn(LOG_TAG, "Could not remove retired arm64 Codex provider");
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to install Ominal PRoot commands", e);
            addTransientSystemMessage("Ominal couldn't finish setup. Restart the app.");
        }
    }

    private void ensureOminalMotd() {
        File motd = new File(OminalConstants.OMINAL_ETC_PREFIX_DIR_PATH, "motd");
        try {
            ensureDirectory(motd.getParent());
            if (!motd.isFile() || !OMINAL_MOTD.equals(readFile(motd)))
                writeFile(motd, OMINAL_MOTD);
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to install Ominal terminal welcome", e);
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
                    throw new IOException("This phone isn't supported yet.");

                String bootstrap = OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-runtime-bootstrap";
                showSetupProgress(4, "Checking system components",
                    "Applying compatible package updates", -1);
                if (runRuntimeShell(shellQuote(bootstrap) + " --prepare", "Prepare Ominal runtime")) {
                    finishRuntimeSetup(whenReady);
                    return;
                }

                File runtimeRoot = new File(OminalConstants.OMINAL_HOME_DIR_PATH, ".ominal/runtime");
                ensureDirectory(runtimeRoot.getAbsolutePath());
                long usableBytes = runtimeRoot.getUsableSpace();
                if (usableBytes > 0 && usableBytes < MIN_RUNTIME_FREE_BYTES)
                    throw new IOException("Ominal needs at least 4 GB of free space to finish setup.");

                File downloads = new File(runtimeRoot, "downloads");
                ensureDirectory(downloads.getAbsolutePath());
                File preparedRootfs = new File(downloads,
                    "ominal-ubuntu-24.04.4-arm64-prepared-v3.tar.gz");
                File rootfs = new File(downloads, "ubuntu-base-24.04.4-arm64-nohardlinks.tar.gz");
                File node = new File(downloads, "node-v" + NODE_VERSION + "-linux-arm64.tar.gz");
                File codexCore = new File(downloads, "codex-" + CODEX_VERSION + ".tgz");
                File codexArm64 = new File(downloads, "codex-" + CODEX_VERSION + "-linux-arm64.tgz");

                updateRuntimeStatus("Getting things ready");
                if (!"legacy".equals(BuildConfig.OMINAL_RUNTIME_DELIVERY)) {
                    if ("bundled".equals(BuildConfig.OMINAL_RUNTIME_DELIVERY)) {
                        copyRuntimeAsset(BuildConfig.OMINAL_PREPARED_ROOTFS_ASSET, preparedRootfs,
                            BuildConfig.OMINAL_PREPARED_ROOTFS_SHA256);
                    } else {
                        downloadRuntimeArtifact(BuildConfig.OMINAL_PREPARED_ROOTFS_URL, preparedRootfs,
                            BuildConfig.OMINAL_PREPARED_ROOTFS_SHA256, "Downloading Linux workspace");
                    }

                    updateRuntimeStatus("Finishing setup");
                    String preparedInstallCommand = shellQuote(bootstrap) + " --install-prepared "
                        + shellQuote(preparedRootfs.getAbsolutePath());
                    if (!runRuntimeShell(preparedInstallCommand, "Install prepared Ominal runtime"))
                        throw new IOException(mRuntimeSetupDetail.isEmpty()
                            ? "The prepared Linux workspace did not install." : mRuntimeSetupDetail);
                    deleteQuietly(preparedRootfs);
                    finishRuntimeSetup(whenReady);
                    return;
                }

                copyRuntimeAsset(ROOTFS_ASSET, rootfs, ROOTFS_SHA256);
                downloadRuntimeArtifact(NODE_URL, node, NODE_SHA256, "Downloading tools");
                downloadRuntimeArtifact(CODEX_CORE_URL, codexCore, CODEX_CORE_SHA256, "Downloading Codex");
                downloadRuntimeArtifact(CODEX_ARM64_URL, codexArm64, CODEX_ARM64_SHA256,
                    "Downloading Codex");

                updateRuntimeStatus("Finishing setup");
                String installCommand = shellQuote(bootstrap) + " "
                    + shellQuote(rootfs.getAbsolutePath()) + " "
                    + shellQuote(node.getAbsolutePath()) + " "
                    + shellQuote(codexCore.getAbsolutePath()) + " "
                    + shellQuote(codexArm64.getAbsolutePath());
                if (!runRuntimeShell(installCommand, "Install Ominal runtime"))
                    throw new IOException(mRuntimeSetupDetail.isEmpty()
                        ? "The Linux runtime installer did not complete." : mRuntimeSetupDetail);

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
                    setStatus("Setup paused");
                    showSetupFailure(runtimeSetupMessage(e), () -> ensureRuntimeReady(whenReady));
                });
            }
        }, "ominal-runtime-setup").start();
    }

    private String runtimeSetupMessage(Exception error) {
        String detail = error == null ? null : error.getMessage();
        if (detail != null) {
            detail = detail.trim();
            if (detail.startsWith("Ominal needs at least 4 GB")
                || detail.startsWith("This phone isn't supported")) return detail;
        }
        return "Setup couldn't finish. Check your connection and free space, then restart Ominal.";
    }

    private void finishRuntimeSetup(Runnable whenReady) {
        runOnUiThread(() -> {
            showSetupProgress(4, "Checking your workspace", "Verifying installed components", 100);
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
        HashMap<String, String> environment = new HashMap<>();
        String dnsServers = getRuntimeDnsServerList();
        if (!dnsServers.isEmpty()) environment.put("OMINAL_DNS_SERVERS", dnsServers);
        String aptHostAddresses = getRuntimeHostAddresses("ports.ubuntu.com");
        if (!aptHostAddresses.isEmpty()) environment.put("OMINAL_APT_HOSTS", aptHostAddresses);
        AppShell.execute(this, command, null, new OminalShellEnvironment(), environment, true);
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
        String actualSha256 = sha256(partial);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            Logger.logError(LOG_TAG, "Bundled runtime verification failed for " + target.getName()
                + ": expected " + expectedSha256 + ", got " + actualSha256);
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
        Network activeNetwork = connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? connectivityManager.getActiveNetwork() : null;
        HttpURLConnection connection = (HttpURLConnection) (activeNetwork == null
            ? url.openConnection() : activeNetwork.openConnection(url));
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(120_000);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-transform");
        connection.setRequestProperty("Accept-Encoding", "identity");
        if (resumeAt > 0) connection.setRequestProperty("Range", "bytes=" + resumeAt + "-");

        try {
            int responseCode = connection.getResponseCode();
            boolean append = resumeAt > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL;
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL)
                throw new IOException("Download failed with HTTP " + responseCode + ": " + target.getName());
            if (!append) resumeAt = 0L;

            long responseBytes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? connection.getContentLengthLong() : connection.getContentLength();
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

        String actualSha256 = sha256(partial);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            Logger.logError(LOG_TAG, "Runtime download verification failed for " + target.getName()
                + ": expected " + expectedSha256 + ", got " + actualSha256
                + ", bytes " + partial.length());
            deleteQuietly(partial);
            throw new IOException("Downloaded runtime artifact failed verification: " + target.getName());
        }
        replaceFile(partial, target);
    }

    private boolean hasSha256(File file, String expectedSha256) throws IOException {
        if (expectedSha256 == null || !expectedSha256.matches("(?i)[0-9a-f]{64}"))
            throw new IOException("Invalid SHA-256 lock for " + file.getName());
        return sha256(file).equalsIgnoreCase(expectedSha256);
    }

    private String sha256(File file) throws IOException {
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
        return actual.toString();
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
        runOnUiThread(() -> {
            setStatus(status);
            int percent = parseTrailingPercent(status);
            if (status.startsWith("Downloading Codex")) {
                showSetupProgress(3, "Installing Codex", status, percent);
            } else if (status.startsWith("Downloading")) {
                showSetupProgress(2, "Preparing Linux workspace", status, percent);
            } else if (status.startsWith("Finishing")) {
                showSetupProgress(4, "Configuring your workspace", "Installing system packages", -1);
            } else {
                showSetupProgress(2, "Preparing Linux workspace", status, percent);
            }
        });
    }

    private int parseTrailingPercent(String status) {
        if (status == null) return -1;
        int percentIndex = status.lastIndexOf('%');
        if (percentIndex < 1) return -1;
        int start = percentIndex - 1;
        while (start >= 0 && Character.isDigit(status.charAt(start))) start--;
        try {
            return Integer.parseInt(status.substring(start + 1, percentIndex));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void registerRuntimeNetworkObserver() {
        if (mRuntimeNetworkCallback != null) return;
        ConnectivityManager connectivityManager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        mRuntimeNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                refreshRuntimeDns();
            }

            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties properties) {
                refreshRuntimeDns();
            }

            @Override
            public void onLost(Network network) {
                refreshRuntimeDns();
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();
        try {
            connectivityManager.registerNetworkCallback(request, mRuntimeNetworkCallback);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to observe Ominal network changes", e);
            mRuntimeNetworkCallback = null;
        }
    }

    private void unregisterRuntimeNetworkObserver() {
        if (mRuntimeNetworkCallback == null) return;
        ConnectivityManager connectivityManager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(mRuntimeNetworkCallback);
            } catch (IllegalArgumentException ignored) {
                // The system already discarded this callback.
            }
        }
        mRuntimeNetworkCallback = null;
    }

    private void refreshRuntimeDns() {
        try {
            String dnsServerList = getRuntimeDnsServerList();
            if (dnsServerList.isEmpty()) return;

            File rootfs = new File(OminalConstants.OMINAL_HOME_DIR_PATH, ".ominal/runtime/linux/rootfs");
            File runtimeReady = new File(rootfs, ".ominal-rootfs-ready");
            File etcDirectory = new File(rootfs, "etc");
            if (!runtimeReady.isFile() || !etcDirectory.isDirectory()) return;

            StringBuilder resolver = new StringBuilder();
            for (String dnsServer : dnsServerList.split("\\s+"))
                resolver.append("nameserver ").append(dnsServer).append('\n');
            writeFile(new File(etcDirectory, "resolv.conf"), resolver.toString());
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to refresh Ominal runtime DNS", e);
        }
    }

    private String getRuntimeDnsServerList() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return "";

        LinkedHashSet<String> ipv4DnsServers = new LinkedHashSet<>();
        LinkedHashSet<String> ipv6DnsServers = new LinkedHashSet<>();
        Network activeNetwork = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activeNetwork = connectivityManager.getActiveNetwork();
            if (isRuntimeNetwork(connectivityManager, activeNetwork))
                addRuntimeDnsServers(connectivityManager.getLinkProperties(activeNetwork), ipv4DnsServers, ipv6DnsServers);
        }

        Network[] networks = connectivityManager.getAllNetworks();
        if (ipv4DnsServers.isEmpty() && ipv6DnsServers.isEmpty() && networks != null) {
            for (Network network : networks) {
                if (network != null && !network.equals(activeNetwork)
                    && isRuntimeNetwork(connectivityManager, network))
                    addRuntimeDnsServers(connectivityManager.getLinkProperties(network), ipv4DnsServers, ipv6DnsServers);
            }
        }

        StringBuilder dnsServers = new StringBuilder();
        for (String dnsServer : ipv4DnsServers) {
            if (dnsServers.length() > 0) dnsServers.append(' ');
            dnsServers.append(dnsServer);
        }
        for (String dnsServer : ipv6DnsServers) {
            if (dnsServers.length() > 0) dnsServers.append(' ');
            dnsServers.append(dnsServer);
        }
        return dnsServers.toString();
    }

    private static boolean isRuntimeNetwork(ConnectivityManager connectivityManager, Network network) {
        if (connectivityManager == null || network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private String getRuntimeHostAddresses(String hostname) {
        LinkedHashSet<String> addresses = new LinkedHashSet<>();
        try {
            for (java.net.InetAddress address : java.net.InetAddress.getAllByName(hostname)) {
                String value = address.getHostAddress();
                int scopeSeparator = value.indexOf('%');
                if (scopeSeparator >= 0) value = value.substring(0, scopeSeparator);
                if (!value.isEmpty()) addresses.add(value);
            }
        } catch (Exception e) {
            Logger.logWarn(LOG_TAG, "Could not resolve runtime repository host");
        }
        return TextUtils.join(" ", addresses);
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
        builder.append("ui.version=").append(UI_CONFIG_VERSION).append('\n');
        builder.append("# Edit like .bashrc: one key=value per line, comments start with #.\n");
        builder.append("# 'export key=value' also works, but the app only reads these UI keys.\n");
        builder.append("# From an agent session, run: ominal-event reload-ui\n");
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
        mDisplayPane = null;
        mDisplayAvailabilityView = null;
        mDisplayAgentStatusView = null;
        mDisplayAgentStatusText = null;
        mDisplayAgentPulse = null;
        mDisplayActivityBorder = null;
        mDisplayWebView = null;
        mNativeDisplayView = null;
        mActiveAgentTurnView = null;
        mDisplayUrlLoaded = false;

        mContentFrame = new FrameLayout(this);
        root.addView(mContentFrame, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        mComposerView = createComposer();
        root.addView(mComposerView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        mDrawerLayout.addView(root, new DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT));

        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(
            getDrawerWidth(), DrawerLayout.LayoutParams.MATCH_PARENT);
        drawerParams.gravity = GravityCompat.START;
        mDrawerLayout.addView(mChatDrawer, drawerParams);

        mDisplayWarmHost = new FrameLayout(this);
        mDisplayWarmHost.setAlpha(1f);
        mDisplayWarmHost.setVisibility(View.VISIBLE);
        mDisplayWarmHost.setClickable(false);
        mDisplayWarmHost.setFocusable(false);
        mDisplayWarmHost.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        mDisplayWarmHost.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams warmParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mRootFrame.addView(mDisplayWarmHost, warmParams);

        mRootFrame.addView(mDrawerLayout, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        renderMode();
        mPairingOverlay = createPairingOverlay();
        mRootFrame.addView(mPairingOverlay, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        mSetupOverlay = createSetupOverlay();
        mRootFrame.addView(mSetupOverlay, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return mRootFrame;
    }

    private View createPairingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setElevation(dp(20));
        overlay.setVisibility(View.GONE);
        overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout content = new LinearLayout(this);
        mPairingContent = content;
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        content.setPadding(dp(30), dp(48), dp(30), dp(40));

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.splash_mark);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setContentDescription("Monolith");
        content.addView(mark, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView title = new TextView(this);
        title.setText("Choose your agent");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, dp(24), 0, 0);
        content.addView(title, titleParams);

        TextView detail = new TextView(this);
        detail.setText("Connect an installed harness. You can change it for any conversation.");
        detail.setTextColor(Color.rgb(174, 174, 174));
        detail.setTextSize(15);
        detail.setGravity(Gravity.CENTER);
        detail.setLineSpacing(0, 1.08f);
        detail.setIncludeFontPadding(false);
        detail.setMaxLines(3);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(dp(10), dp(14), dp(10), 0);
        content.addView(detail, detailParams);

        LinearLayout providerGroup = new LinearLayout(this);
        providerGroup.setOrientation(LinearLayout.VERTICAL);
        providerGroup.setBackground(makeRoundedDrawable(
            Color.rgb(16, 16, 16), Color.rgb(48, 48, 48), dp(18)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            providerGroup.setClipToOutline(true);
        }

        mPairingCodexButton = createPairingProviderRow(
            R.drawable.provider_codex, "Codex", "OpenAI", dp(7));
        mPairingCodexButton.setOnClickListener(v -> {
            setPairingBusy(true, "");
            if (mActiveSession != null)
                selectHarness(mActiveSession, OminalHarnessTerminal.CODEX_ID);
            startCodexTerminal();
        });
        providerGroup.addView(mPairingCodexButton, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(70)));
        providerGroup.addView(createPairingDivider());

        mPairingClaudeButton = createPairingProviderRow(
            R.drawable.provider_claude, "Claude Code", "Anthropic", 0);
        mPairingClaudeButton.setOnClickListener(v -> {
            setPairingBusy(true, "");
            if (mActiveSession != null)
                selectHarness(mActiveSession, OminalHarnessTerminal.CLAUDE_CODE_ID);
            launchHarnessTerminal(OminalHarnessTerminal.CLAUDE_CODE_ID, true);
        });
        providerGroup.addView(mPairingClaudeButton, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(70)));
        providerGroup.addView(createPairingDivider());

        mPairingAntigravityButton = createPairingProviderRow(
            R.drawable.provider_antigravity, "Antigravity", "Google", dp(5));
        mPairingAntigravityButton.setOnClickListener(v -> {
            setPairingBusy(true, "");
            if (mActiveSession != null)
                selectHarness(mActiveSession, OminalHarnessTerminal.ANTIGRAVITY_ID);
            launchHarnessTerminal(OminalHarnessTerminal.ANTIGRAVITY_ID, true);
        });
        providerGroup.addView(mPairingAntigravityButton, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(70)));

        LinearLayout.LayoutParams providerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        providerParams.setMargins(0, dp(32), 0, 0);
        content.addView(providerGroup, providerParams);

        mPairingComputerOnlyButton = createPairingQuietButton("Continue without an agent");
        mPairingComputerOnlyButton.setOnClickListener(v -> completeRunnerPairing(false));
        LinearLayout.LayoutParams computerOnlyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        computerOnlyParams.setMargins(0, dp(10), 0, 0);
        content.addView(mPairingComputerOnlyButton, computerOnlyParams);

        TextView attribution = new TextView(this);
        attribution.setText("Sign-in stays between you and the selected provider.");
        attribution.setTextColor(Color.rgb(100, 100, 100));
        attribution.setTextSize(12);
        attribution.setGravity(Gravity.CENTER);
        attribution.setIncludeFontPadding(false);
        LinearLayout.LayoutParams attributionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        attributionParams.setMargins(0, dp(22), 0, 0);
        content.addView(attribution, attributionParams);

        scroller.addView(content, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        overlay.addView(scroller, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return overlay;
    }

    private View createPairingProviderRow(int iconResource, String title,
                                          String detail, int iconInset) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(7), dp(12), dp(7));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(title + ", " + detail);
        row.setBackground(new RippleDrawable(
            ColorStateList.valueOf(Color.argb(34, 255, 255, 255)),
            makeRoundedDrawable(Color.TRANSPARENT, Color.TRANSPARENT, 0), null));

        FrameLayout iconWell = new FrameLayout(this);
        iconWell.setBackground(makeRoundedDrawable(
            Color.rgb(27, 27, 27), Color.rgb(53, 53, 53), dp(12)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) iconWell.setClipToOutline(true);
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResource);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setPadding(iconInset, iconInset, iconInset, iconInset);
        iconWell.addView(icon, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        row.addView(iconWell, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setIncludeFontPadding(false);
        labels.addView(titleView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView detailView = new TextView(this);
        detailView.setText(detail);
        detailView.setTextColor(Color.rgb(151, 151, 151));
        detailView.setTextSize(12);
        detailView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(3), 0, 0);
        labels.addView(detailView, detailParams);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMargins(dp(14), 0, dp(8), 0);
        row.addView(labels, labelParams);

        ImageView next = new ImageView(this);
        next.setImageResource(R.drawable.ic_chevron_right);
        next.setImageTintList(ColorStateList.valueOf(Color.rgb(132, 132, 132)));
        next.setContentDescription(null);
        row.addView(next, new LinearLayout.LayoutParams(dp(18), dp(18)));
        return row;
    }

    private View createPairingDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(40, 40, 40));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(dp(72), 0, 0, 0);
        divider.setLayoutParams(params);
        return divider;
    }

    private Button createPairingQuietButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        normalizeButton(button);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(174, 174, 174));
        button.setTextSize(14);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(makeRoundedDrawable(Color.TRANSPARENT, Color.TRANSPARENT, dp(14)));
        attachNativeRipple(button);
        return button;
    }

    private void showRunnerPairing() {
        if (mPairingOverlay == null || mCodexSignedIn || mCodexSessionExpired
            || mPrefs == null || mPrefs.getBoolean(PREF_RUNNER_PAIRING_COMPLETE, false)) {
            return;
        }
        showRunnerPairingSurface();
    }

    private void showRunnerPairingPreview() {
        if (!BuildConfig.DEBUG || mPairingOverlay == null) return;
        showRunnerPairingChooser();
    }

    private void showRunnerPairingChooser() {
        if (mPairingOverlay == null) return;
        showRunnerPairingSurface();
    }

    private void scheduleHarnessUpdates() {
        if (!mBootstrapReady || !mRuntimeReady || mHarnessUpdateInFlight) return;
        if (mPromptRunning) {
            if (mRootFrame != null)
                mRootFrame.postDelayed(this::scheduleHarnessUpdates, 30_000);
            return;
        }
        mHarnessUpdateInFlight = true;
        new Thread(() -> {
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-harness-update",
                new String[0], null, OminalConstants.OMINAL_HOME_DIR_PATH,
                ExecutionCommand.Runner.APP_SHELL.getName(), false);
            command.commandLabel = "Update installed harnesses";
            AppShell.execute(this, command, null, new OminalShellEnvironment(),
                codexServerEnvironment(), true);
            runOnUiThread(() -> {
                mHarnessUpdateInFlight = false;
                if (mActiveSession != null)
                    refreshHarnessCapabilities(mActiveSession.harnessId);
            });
        }, "ominal-harness-update").start();
    }

    private void showRunnerPairingSurface() {
        if (mSetupOverlay != null && mSetupOverlay.getVisibility() == View.VISIBLE) {
            if (mRootFrame != null) mRootFrame.postDelayed(this::showRunnerPairingSurface, 300);
            return;
        }
        setPairingBusy(false, "");
        mPairingOverlay.animate().cancel();
        mPairingOverlay.setAlpha(0f);
        mPairingOverlay.setTranslationY(dp(10));
        mPairingOverlay.setVisibility(View.VISIBLE);
        mPairingOverlay.bringToFront();
        hideKeyboardForBlockingSurface(mPairingOverlay);
        mPairingOverlay.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .start();
        animatePairingChildren();
    }

    private void animatePairingChildren() {
        if (mPairingContent == null) return;
        for (int index = 0; index < mPairingContent.getChildCount(); index++) {
            View child = mPairingContent.getChildAt(index);
            child.animate().cancel();
            child.setAlpha(0f);
            child.setTranslationY(dp(10));
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(55L + (index * 42L))
                .setDuration(260)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.7f))
                .start();
        }
    }

    private void setPairingBusy(boolean busy, String status) {
        setPairingButtonEnabled(mPairingCodexButton, !busy);
        setPairingButtonEnabled(mPairingClaudeButton, !busy);
        setPairingButtonEnabled(mPairingAntigravityButton, !busy);
        setPairingButtonEnabled(mPairingComputerOnlyButton, !busy);
    }

    private void setPairingButtonEnabled(View button, boolean enabled) {
        if (button == null) return;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.5f);
    }

    private void completeRunnerPairing(boolean signedIn) {
        if (mPrefs != null)
            mPrefs.edit().putBoolean(PREF_RUNNER_PAIRING_COMPLETE, true).apply();
        boolean pairingWasVisible = mPairingOverlay != null
            && mPairingOverlay.getVisibility() == View.VISIBLE;
        hideRunnerPairing();
        if (pairingWasVisible) switchMode(MODE_CHAT);
        setStatus(signedIn ? "Signed in" : "Ready");
    }

    private void hideRunnerPairing() {
        if (mPairingOverlay == null || mPairingOverlay.getVisibility() != View.VISIBLE) return;
        mPairingOverlay.animate().cancel();
        mPairingOverlay.animate()
            .alpha(0f)
            .translationY(-dp(8))
            .setDuration(180)
            .withEndAction(() -> {
                mPairingOverlay.setVisibility(View.GONE);
                mPairingOverlay.setAlpha(1f);
                mPairingOverlay.setTranslationY(0f);
            })
            .start();
    }

    private View createSetupOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setElevation(dp(24));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(28), dp(28), dp(28));

        mSetupMarkView = new SetupMarkView(this);
        content.addView(mSetupMarkView, new LinearLayout.LayoutParams(dp(48), dp(48)));

        mSetupStageView = new TextView(this);
        mSetupStageView.setTextColor(Color.rgb(144, 144, 144));
        mSetupStageView.setTextSize(12);
        mSetupStageView.setGravity(Gravity.CENTER);
        mSetupStageView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mSetupStageView.setVisibility(View.GONE);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stageParams.setMargins(0, dp(26), 0, 0);
        content.addView(mSetupStageView, stageParams);

        mSetupTitleView = new TextView(this);
        mSetupTitleView.setTextColor(Color.WHITE);
        mSetupTitleView.setTextSize(27);
        mSetupTitleView.setGravity(Gravity.CENTER);
        mSetupTitleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mSetupTitleView.setIncludeFontPadding(false);
        mSetupTitleView.setVisibility(View.GONE);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, dp(10), 0, 0);
        content.addView(mSetupTitleView, titleParams);

        mSetupDetailView = new TextView(this);
        mSetupDetailView.setTextColor(Color.rgb(174, 174, 174));
        mSetupDetailView.setTextSize(15);
        mSetupDetailView.setGravity(Gravity.CENTER);
        mSetupDetailView.setIncludeFontPadding(false);
        mSetupDetailView.setMaxLines(3);
        mSetupDetailView.setVisibility(View.GONE);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(12), 0, 0);
        content.addView(mSetupDetailView, detailParams);

        mSetupProgressView = new RoundedSetupProgressView(this);
        mSetupProgressView.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        progressParams.setMargins(0, dp(34), 0, 0);
        content.addView(mSetupProgressView, progressParams);

        mSetupPercentView = new TextView(this);
        mSetupPercentView.setTextColor(Color.rgb(144, 144, 144));
        mSetupPercentView.setTextSize(13);
        mSetupPercentView.setGravity(Gravity.END);
        mSetupPercentView.setIncludeFontPadding(false);
        mSetupPercentView.setVisibility(View.GONE);
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        percentParams.setMargins(0, dp(10), 0, 0);
        content.addView(mSetupPercentView, percentParams);

        mSetupRetryButton = createSecondaryButton("Try again");
        mSetupRetryButton.setVisibility(View.GONE);
        mSetupRetryButton.setOnClickListener(v -> {
            Runnable action = mSetupRetryAction;
            mSetupRetryAction = null;
            mSetupRetryButton.setVisibility(View.GONE);
            if (action != null) action.run();
        });
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        retryParams.setMargins(0, dp(24), 0, 0);
        content.addView(mSetupRetryButton, retryParams);

        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER);
        overlay.addView(content, contentParams);
        return overlay;
    }

    void showSetupProgress(int stage, String title, String detail, int percent) {
        if (mSetupOverlay == null) return;
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            runOnUiThread(() -> showSetupProgress(stage, title, detail, percent));
            return;
        }
        mSetupOverlay.animate().cancel();
        mSetupOverlay.setAlpha(1f);
        mSetupOverlay.setVisibility(View.VISIBLE);
        hideKeyboardForBlockingSurface(mSetupOverlay);
        mSetupStageView.setText(stage > 0 ? "STEP " + Math.min(stage, 4) + " OF 4" : "SETUP");
        mSetupStageView.setVisibility(View.VISIBLE);
        mSetupTitleView.setText(title == null ? "Preparing your workspace" : title);
        mSetupTitleView.setVisibility(View.VISIBLE);
        mSetupDetailView.setText(detail == null ? "Getting things ready" : detail);
        mSetupDetailView.setVisibility(View.VISIBLE);
        mSetupProgressView.setIndeterminate(percent < 0);
        if (percent >= 0) mSetupProgressView.setProgress(Math.max(0, Math.min(100, percent)));
        mSetupProgressView.setVisibility(View.VISIBLE);
        if (percent >= 0) {
            mSetupPercentView.setText(Math.max(0, Math.min(100, percent)) + "%");
            mSetupPercentView.setVisibility(View.VISIBLE);
        } else {
            mSetupPercentView.setVisibility(View.GONE);
        }
        mSetupRetryAction = null;
        mSetupRetryButton.setVisibility(View.GONE);
    }

    private void hideKeyboardForBlockingSurface(@NonNull View surface) {
        surface.setFocusableInTouchMode(true);
        surface.requestFocus();
        surface.post(() -> {
            InputMethodManager manager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null)
                manager.hideSoftInputFromWindow(surface.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && surface.getWindowInsetsController() != null) {
                surface.getWindowInsetsController().hide(WindowInsets.Type.ime());
            }
        });
    }

    void showSetupFailure(String detail, Runnable retryAction) {
        if (mSetupOverlay == null) return;
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            runOnUiThread(() -> showSetupFailure(detail, retryAction));
            return;
        }
        mSetupOverlay.animate().cancel();
        mSetupOverlay.setAlpha(1f);
        mSetupOverlay.setVisibility(View.VISIBLE);
        mSetupStageView.setVisibility(View.VISIBLE);
        mSetupTitleView.setVisibility(View.VISIBLE);
        mSetupDetailView.setVisibility(View.VISIBLE);
        mSetupProgressView.setVisibility(View.GONE);
        mSetupPercentView.setVisibility(View.GONE);
        mSetupStageView.setText("Setup needs attention");
        mSetupTitleView.setText("Couldn't finish setup");
        mSetupDetailView.setText(detail == null ? "Setup could not finish." : detail);
        mSetupRetryAction = retryAction;
        mSetupRetryButton.setVisibility(retryAction == null ? View.GONE : View.VISIBLE);
    }

    private void completeSetupProgress() {
        if (mSetupOverlay == null) return;
        showSetupProgress(4, "Workspace ready", "Opening your chats", 100);
        long exitDelayMs = 360L;
        if (mSetupMarkView != null) {
            exitDelayMs = Math.max(exitDelayMs, mSetupMarkView.timeUntilExitAllowed());
        }
        mSetupOverlay.postDelayed(() -> {
            Runnable finish = () -> {
                mSetupOverlay.setVisibility(View.GONE);
                mSetupOverlay.setAlpha(1f);
            };
            if (mSetupMarkView != null) {
                mSetupMarkView.playExit(finish);
            } else {
                finish.run();
            }
        }, exitDelayMs);
    }

    private int getDrawerWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.max(dp(288), Math.min(dp(380), screenWidth - dp(48)));
    }

    private static final class RoundedSetupProgressView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private int progress;
        private boolean indeterminate;

        RoundedSetupProgressView(Context context) {
            super(context);
            trackPaint.setColor(Color.rgb(38, 38, 38));
            progressPaint.setColor(Color.WHITE);
            setContentDescription("Setup progress");
        }

        void setProgress(int value) {
            progress = Math.max(0, Math.min(100, value));
            invalidate();
        }

        void setIndeterminate(boolean value) {
            if (indeterminate == value) return;
            indeterminate = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            if (width <= 0f || height <= 0f) return;

            float radius = height / 2f;
            RectF track = new RectF(0f, 0f, width, height);
            canvas.drawRoundRect(track, radius, radius, trackPaint);

            if (indeterminate) {
                float segmentWidth = Math.max(height * 3f, width * 0.24f);
                float cycle = (SystemClock.uptimeMillis() % 1100L) / 1100f;
                float left = -segmentWidth + cycle * (width + segmentWidth * 2f);
                clipPath.reset();
                clipPath.addRoundRect(track, radius, radius, Path.Direction.CW);
                int save = canvas.save();
                canvas.clipPath(clipPath);
                canvas.drawRoundRect(new RectF(left, 0f, left + segmentWidth, height),
                    radius, radius, progressPaint);
                canvas.restoreToCount(save);
                postInvalidateOnAnimation();
                return;
            }

            if (progress <= 0) return;
            float progressWidth = Math.max(height, width * progress / 100f);
            canvas.drawRoundRect(new RectF(0f, 0f, progressWidth, height),
                radius, radius, progressPaint);
        }
    }

    private void applySystemBars() {
        UiSpec ui = ui();
        getWindow().setStatusBarColor(ui.header.fill);
        getWindow().setNavigationBarColor(isLightAppearanceEnabled() ? ui.canvas : Color.BLACK);
        View decor = getWindow().getDecorView();
        int visibility = decor.getSystemUiVisibility()
            & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (isLightAppearanceEnabled() && mMode != MODE_DISPLAY) {
            visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                visibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(visibility);
    }

    private View createHeader() {
        UiSpec ui = ui();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(16), dp(5), dp(16), dp(5));
        header.setMinimumHeight(dp(52));
        header.setBackgroundColor(ui.header.fill);

        ImageButton chatsButton = createToolbarIconButton(R.drawable.ic_menu, "Chat history");
        chatsButton.setOnClickListener(v -> showChatPicker());
        header.addView(chatsButton, new LinearLayout.LayoutParams(dp(40), dp(40)));

        View titleSpacer = new View(this);
        mSubtitleView = null;
        header.addView(titleSpacer, new LinearLayout.LayoutParams(0, dp(40), 1));

        mHeaderDisplayButton = createToolbarIconButton(R.drawable.ic_display, "Screen");
        mHeaderDisplayButton.setOnClickListener(v -> {
            if (mMode == MODE_DISPLAY) {
                switchMode(MODE_CHAT);
            } else {
                switchMode(MODE_DISPLAY);
            }
        });
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        displayParams.setMargins(dp(2), 0, 0, 0);
        header.addView(mHeaderDisplayButton, displayParams);

        mLoloButton = null;

        mAccountButton = createToolbarIconButton(R.drawable.ic_account, "Account and settings");
        mAccountButton.setOnClickListener(v -> showAppMenu());
        LinearLayout.LayoutParams accountParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        accountParams.setMargins(dp(2), 0, 0, 0);
        header.addView(mAccountButton, accountParams);
        styleAccountButton();

        return header;
    }

    private View createComposer() {
        UiSpec ui = ui();
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.VERTICAL);
        composer.setPadding(dp(16), 0, dp(16), dp(10));
        composer.setBackgroundColor(ui.composer.fill);

        mCommandSuggestionsView = new LinearLayout(this);
        mCommandSuggestionsView.setOrientation(LinearLayout.VERTICAL);
        mCommandSuggestionsView.setPadding(dp(4), dp(4), dp(4), dp(4));
        mCommandSuggestionsView.setBackground(makeRoundedDrawable(
            ui.panel, ui.border, dp(14)));
        mCommandSuggestionsView.setVisibility(View.GONE);

        mCommandSuggestionsRow = new LinearLayout(this);
        mCommandSuggestionsRow.setOrientation(LinearLayout.VERTICAL);
        mCommandSuggestionsRow.setGravity(Gravity.START);
        mCommandSuggestionsView.addView(mCommandSuggestionsRow,
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams suggestionsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        suggestionsParams.setMargins(0, 0, 0, dp(6));
        composer.addView(mCommandSuggestionsView, suggestionsParams);

        LinearLayout writingRail = new LinearLayout(this);
        writingRail.setOrientation(LinearLayout.VERTICAL);
        writingRail.setPadding(dp(3), dp(2), dp(3), dp(3));
        writingRail.setMinimumHeight(dp(86));
        writingRail.setBackground(makeSurfaceDrawable(ui.composerInput, false));
        LinearLayout.LayoutParams writingRailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        mPromptInput = new EditText(this);
        mPromptInput.setHint(getString(R.string.oringutan_prompt_hint));
        mPromptInput.setMinLines(1);
        mPromptInput.setMaxLines(8);
        mPromptInput.setMinHeight(dp(46));
        mPromptInput.setMinimumHeight(dp(46));
        mPromptInput.setSingleLine(false);
        mPromptInput.setTextIsSelectable(true);
        mPromptInput.setIncludeFontPadding(false);
        mPromptInput.setTextColor(ui.composerInput.text);
        mPromptInput.setHintTextColor(ui.muted);
        mPromptInput.setTextSize(16f);
        mPromptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mPromptInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        mPromptInput.setBackgroundColor(Color.TRANSPARENT);
        mPromptInput.setPadding(dp(11), dp(9), dp(11), dp(4));
        String savedDraft = mPrefs == null ? "" : mPrefs.getString(PREF_COMPOSER_DRAFT, "");
        mPromptInput.setText(savedDraft);
        mPromptInput.setSelection(mPromptInput.length());
        mPromptInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mPrefs != null)
                    mPrefs.edit().putString(PREF_COMPOSER_DRAFT,
                        s == null ? "" : s.toString()).apply();
                renderCommandSuggestions(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        renderCommandSuggestions(savedDraft);
        mPromptInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitPrompt();
                return true;
            }
            return false;
        });
        mPromptInput.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
                requestComposerKeyboard();
            return false;
        });
        writingRail.addView(mPromptInput, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout actionRail = new LinearLayout(this);
        actionRail.setOrientation(LinearLayout.HORIZONTAL);
        actionRail.setGravity(Gravity.CENTER_VERTICAL);
        actionRail.setPadding(dp(2), 0, dp(2), 0);

        mAttachButton = createComposerIconButton(R.drawable.ic_attach, "Attach file");
        mAttachButton.setOnClickListener(v -> pickAttachment());
        actionRail.addView(mAttachButton, new LinearLayout.LayoutParams(dp(38), dp(38)));

        mHarnessControlsButton = createComposerIconButton(R.drawable.ic_tune,
            "Agent, model, effort, and commands");
        mHarnessControlsButton.setOnClickListener(v -> showHarnessControlsDialog());
        LinearLayout.LayoutParams harnessParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        harnessParams.setMargins(dp(2), 0, 0, 0);
        actionRail.addView(mHarnessControlsButton, harnessParams);

        mHarnessContextView = new TextView(this);
        mHarnessContextView.setTextColor(ui.muted);
        mHarnessContextView.setTextSize(12.5f);
        mHarnessContextView.setGravity(Gravity.CENTER_VERTICAL);
        mHarnessContextView.setIncludeFontPadding(false);
        mHarnessContextView.setSingleLine(true);
        mHarnessContextView.setEllipsize(TextUtils.TruncateAt.END);
        mHarnessContextView.setMaxWidth(dp(156));
        mHarnessContextView.setPadding(dp(4), 0, dp(8), 0);
        mHarnessContextView.setOnClickListener(v -> showHarnessControlsDialog());
        actionRail.addView(mHarnessContextView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        updateHarnessControls();

        View actionSpacer = new View(this);
        actionRail.addView(actionSpacer, new LinearLayout.LayoutParams(0, dp(38), 1));

        mSendButton = createComposerSendButton(R.drawable.ic_send,
            getString(R.string.oringutan_send));
        mSendButton.setContentDescription(getString(R.string.oringutan_send));
        mSendButton.setOnClickListener(v -> submitPrompt());
        mSendButton.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            openAgentTerminalForActiveChat();
            return true;
        });
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        actionRail.addView(mSendButton, sendParams);
        writingRail.addView(actionRail, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));
        composer.addView(writingRail, writingRailParams);

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
            session.harnessId = OminalHarnessRegistry.normalizeSelectedId(
                object.optString("harnessId", OminalHarnessRegistry.DEFAULT_HARNESS_ID));
            copyStringMap(object.optJSONObject("threadIds"), session.threadIds);
            copyStringMap(object.optJSONObject("modelIds"), session.modelIds);
            copyStringMap(object.optJSONObject("effortIds"), session.effortIds);
            copyIntMap(object.optJSONObject("contextCursors"), session.contextCursors);
            session.activeTurn = PendingTurn.fromJson(object.optJSONObject("activeTurn"));
            JSONArray pendingTurns = object.optJSONArray("pendingTurns");
            if (pendingTurns != null) {
                for (int index = 0; index < pendingTurns.length(); index++) {
                    PendingTurn pendingTurn =
                        PendingTurn.fromJson(pendingTurns.optJSONObject(index));
                    if (pendingTurn != null) session.pendingTurns.add(pendingTurn);
                }
            }
            String legacyThreadId = object.optString("codexThreadId", "");
            if (!legacyThreadId.isEmpty()
                && !session.threadIds.containsKey(OminalHarnessTerminal.CODEX_ID)) {
                session.threadIds.put(OminalHarnessTerminal.CODEX_ID, legacyThreadId);
            }
            loadHistory(session);
            return session;
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load chat session", e);
            return null;
        }
    }

    private static void copyStringMap(JSONObject source, HashMap<String, String> target) {
        if (source == null) return;
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = source.optString(key, "").trim();
            if (!value.isEmpty()) target.put(key, value);
        }
    }

    private static void copyIntMap(JSONObject source, HashMap<String, Integer> target) {
        if (source == null) return;
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            int value = source.optInt(key, 0);
            if (value > 0) target.put(key, value);
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
                    object.optString("text", ""), object.optString("timestamp", ""),
                    object.optString("detail", ""),
                    OminalAgentTrace.Snapshot.fromJson(object.optJSONObject("trace")),
                    OminalChatMedia.fromJson(object.optJSONArray("media"))));
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
        if (mPrefs != null) {
            session.harnessId = OminalHarnessRegistry.normalizeSelectedId(
                mPrefs.getString(OminalHarnessRegistry.PREFERENCE_KEY,
                    OminalHarnessRegistry.DEFAULT_HARNESS_ID));
        }
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
        drawer.setPadding(dp(18), dp(16), dp(18), dp(14));
        drawer.setBackgroundColor(ui.drawer.fill);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Chats");
        title.setTextColor(ui.drawer.text);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setTextSize(19);
        title.setSingleLine(true);
        title.setIncludeFontPadding(false);
        topRow.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1));

        ImageButton newChatButton = createToolbarIconButton(R.drawable.ic_add, "New chat");
        newChatButton.setOnClickListener(v -> {
            if (mDrawerLayout != null && mChatDrawer != null) mDrawerLayout.closeDrawer(mChatDrawer);
            createAndSelectSession();
        });
        LinearLayout.LayoutParams newChatParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        newChatParams.setMargins(dp(10), 0, 0, 0);
        topRow.addView(newChatButton, newChatParams);
        drawer.addView(topRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        mChatSearchInput = new EditText(this);
        mChatSearchInput.setSingleLine(true);
        mChatSearchInput.setHint("Search");
        mChatSearchInput.setText(mChatSearchQuery);
        mChatSearchInput.setTextSize(14.5f);
        mChatSearchInput.setTextColor(ui.drawerSearch.text);
        mChatSearchInput.setHintTextColor(ui.onDarkMuted);
        mChatSearchInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mChatSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mChatSearchInput.setPadding(dp(12), 0, dp(12), 0);
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
            LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
        searchParams.setMargins(0, dp(10), 0, dp(10));
        drawer.addView(mChatSearchInput, searchParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setVerticalScrollBarEnabled(false);

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
        OminalAgentRuntime.Snapshot agentSnapshot = mAgentRuntime == null
            ? null : mAgentRuntime.snapshot(session.id);
        mPromptRunning = agentSnapshot != null && agentSnapshot.isRunning();
        stopAgentEventObserver();
        if (mPromptRunning)
            startAgentEventObserver(session, agentEventLogFile(session));
        mPrefs.edit().putString(PREF_ACTIVE_CHAT_ID, session.id).apply();
        renderChatDrawer();
        renderHeader();
        renderMode();
        updateHarnessControls();
        refreshHarnessCapabilities(session.harnessId);
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
            params.setMargins(0, 0, 0, dp(4));
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
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, 0);
        row.setMinimumHeight(dp(66));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(session.title);
        row.setBackground(makeSurfaceDrawable(surface, false));
        attachNativeRipple(row);
        row.setOnClickListener(v -> {
            if (mDrawerLayout != null && mChatDrawer != null) mDrawerLayout.closeDrawer(mChatDrawer);
            setActiveSession(session);
        });

        View selectionRule = new View(this);
        selectionRule.setBackground(makeRoundedDrawable(active ? ui.ink : Color.TRANSPARENT,
            Color.TRANSPARENT, dp(2)));
        LinearLayout.LayoutParams selectionParams = new LinearLayout.LayoutParams(dp(3), dp(30));
        selectionParams.setMargins(0, 0, dp(12), 0);
        row.addView(selectionRule, selectionParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        labels.setPadding(0, dp(8), dp(8), dp(8));

        TextView title = new TextView(this);
        title.setText(session.title);
        title.setTextColor(surface.text);
        title.setTypeface(Typeface.create(active ? "sans-serif-medium" : "sans-serif",
            Typeface.NORMAL));
        title.setTextSize(14.5f);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setIncludeFontPadding(false);
        labels.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(22)));

        TextView meta = new TextView(this);
        meta.setText(sessionMeta(session));
        meta.setTextColor(ui.muted);
        meta.setTextSize(11.5f);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        meta.setIncludeFontPadding(false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(18));
        metaParams.setMargins(0, dp(5), 0, 0);
        labels.addView(meta, metaParams);
        row.addView(labels, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

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
            String text = visibleMessageText(message).replace('\n', ' ').trim();
            if (!text.isEmpty()) {
                if (text.length() > 46) text = text.substring(0, 46).trim() + "...";
                return text;
            }
        }
        return "";
    }

    private void renderHeader() {
        // The active chat is highlighted in the drawer; the main header stays visually quiet.
    }

    private void switchMode(int mode) {
        if (mMode == mode) return;
        boolean leavingDisplay = mMode == MODE_DISPLAY && mode != MODE_DISPLAY;
        if (leavingDisplay) releaseNativeDisplayInput();
        if (mode == MODE_DISPLAY) clearDisplayCloseRequest();
        else stopDisplayControlObserver();
        mMode = mode;
        renderMode();
        if (leavingDisplay) rebindComposerInput();
        if (mode == MODE_DISPLAY) startDisplayControlObserver();
    }

    private void clearDisplayCloseRequest() {
        File request = displayCloseRequestFile();
        if (request != null && request.exists() && !request.delete())
            Logger.logWarn(LOG_TAG, "Could not clear stale display close request");
    }

    private File displayCloseRequestFile() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH,
            ".ominal/runtime/tmp/" + DISPLAY_CLOSE_REQUEST_FILE_NAME);
    }

    @SuppressWarnings("deprecation")
    private void startDisplayControlObserver() {
        stopDisplayControlObserver();
        File request = displayCloseRequestFile();
        if (request == null) return;
        File directory = request.getParentFile();
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) return;
        String requestName = request.getName();
        mDisplayControlObserver = new FileObserver(directory.getAbsolutePath(),
            FileObserver.CREATE | FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                if (!requestName.equals(path)) return;
                runOnUiThread(OringutanActivity.this::consumeDisplayCloseRequest);
            }
        };
        mDisplayControlObserver.startWatching();
    }

    private void stopDisplayControlObserver() {
        if (mDisplayControlObserver == null) return;
        mDisplayControlObserver.stopWatching();
        mDisplayControlObserver = null;
    }

    @SuppressWarnings("deprecation")
    private void startDisplayActivityObserver() {
        stopDisplayActivityObserver();
        File activityFile = displayActivityFile();
        File directory = activityFile.getParentFile();
        if (directory == null) return;
        ensureDirectory(directory.getAbsolutePath());
        String activityName = activityFile.getName();
        mDisplayActivityObserver = new FileObserver(directory.getAbsolutePath(),
            FileObserver.CREATE | FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                if (!activityName.equals(path)) return;
                runOnUiThread(() -> consumeDisplayActivity(activityFile));
            }
        };
        mDisplayActivityObserver.startWatching();
    }

    private void stopDisplayActivityObserver() {
        if (mDisplayActivityObserver == null) return;
        mDisplayActivityObserver.stopWatching();
        mDisplayActivityObserver = null;
    }

    private File displayActivityFile() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH,
            ".ominal/runtime/tmp/" + DISPLAY_ACTIVITY_FILE_NAME);
    }

    private void consumeDisplayActivity(File activityFile) {
        try {
            JSONObject event = new JSONObject(readFile(activityFile));
            if (event.optInt("schemaVersion", -1) != 1) return;
            String sessionId = event.optString("sessionId", "").trim();
            ChatSession session = findSession(sessionId);
            if (sessionId.isEmpty() || session == null || session.activeTurn == null
                || mDisplayNeedsUser) {
                return;
            }
            long turnStartedAt = session.activeTurn.startedAt;
            if (turnStartedAt > 0L
                && activityFile.lastModified() + 1500L < turnStartedAt) {
                return;
            }
            mDisplayActivitySessionId = sessionId;
            setAgentUsingDisplay(true);
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Failed to read display activity", e);
        }
    }

    private void syncDisplayActivityFromDisk() {
        if (mActiveSession == null || mActiveSession.activeTurn == null || mDisplayNeedsUser)
            return;
        File activityFile = displayActivityFile();
        if (activityFile.isFile()) consumeDisplayActivity(activityFile);
    }

    private void consumeDisplayCloseRequest() {
        if (mMode != MODE_DISPLAY) return;
        File request = displayCloseRequestFile();
        if (request != null && request.exists()) {
            if (!request.delete())
                Logger.logWarn(LOG_TAG, "Could not consume display close request");
            switchMode(MODE_CHAT);
        }
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
        mMessagesView = null;
        mScrollView = null;
        mActiveAgentTurnView = null;
        if (mMode == MODE_DISPLAY) {
            View displayPane = getOrCreateDisplayPane();
            attachDisplayPaneToWarmHost(displayPane);
            displayPane.setVisibility(View.VISIBLE);
            if (mNativeDisplayView != null) {
                mNativeDisplayView.setViewportUpdatesEnabled(true);
                mNativeDisplayView.activateInputBridge();
                mNativeDisplayView.post(this::showLiveDisplay);
            }
            animateModeView(displayPane);
            return;
        }

        View nextView = mMode == MODE_TERMINAL
            ? createToolPaneSurface(createTerminalPane())
            : createChatPane();
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
        if (mNativeDisplayView != null) {
            mNativeDisplayView.deactivateInputBridge();
            mNativeDisplayView.setViewportUpdatesEnabled(false);
        }
        attachDisplayPaneToWarmHost(mDisplayPane);
        mDisplayPane.setVisibility(View.VISIBLE);
    }

    private void releaseNativeDisplayInput() {
        if (mNativeDisplayView != null) mNativeDisplayView.deactivateInputBridge();
    }

    private void rebindComposerInput() {
        if (mPromptInput == null) return;
        mPromptInput.setShowSoftInputOnFocus(false);
        mPromptInput.requestFocus();
        mPromptInput.post(() -> {
            InputMethodManager manager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) manager.restartInput(mPromptInput);
            mPromptInput.setShowSoftInputOnFocus(true);
        });
    }

    private void requestComposerKeyboard() {
        if (mPromptInput == null || mMode == MODE_DISPLAY) return;
        mPromptInput.postDelayed(() -> {
            if (mPromptInput == null || mMode == MODE_DISPLAY
                || !mPromptInput.hasWindowFocus()) return;
            mPromptInput.setShowSoftInputOnFocus(true);
            mPromptInput.requestFocus();
            InputMethodManager manager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.restartInput(mPromptInput);
                manager.showSoftInput(mPromptInput, InputMethodManager.SHOW_IMPLICIT);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && mPromptInput.getWindowInsetsController() != null) {
                mPromptInput.getWindowInsetsController().show(WindowInsets.Type.ime());
            }
        }, 40);
    }

    private void attachDisplayPaneToWarmHost(View pane) {
        if (pane == null || mDisplayWarmHost == null || pane.getParent() == mDisplayWarmHost) return;
        detachFromParent(pane);
        mDisplayWarmHost.addView(pane, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void prewarmDisplaySurface() {
        if (!mBootstrapReady || mActiveSession == null || mDisplayWarmHost == null
            || mMode == MODE_DISPLAY) return;

        View pane = getOrCreateDisplayPane();
        pane.setVisibility(View.VISIBLE);
        attachDisplayPaneToWarmHost(pane);
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
        mActiveAgentTurnView = null;
        mScrollView = new ScrollView(this);
        mScrollView.setFillViewport(true);
        mScrollView.setBackgroundColor(ui.chat.fill);

        mMessagesView = new LinearLayout(this);
        mMessagesView.setOrientation(LinearLayout.VERTICAL);
        mMessagesView.setPadding(dp(18), dp(10), dp(18), dp(22));
        mScrollView.addView(mMessagesView, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        if (mActiveSession != null) {
            for (ChatMessage message : mActiveSession.messages) {
                if (shouldHideSystemReadyMessage(message)) continue;
                renderChatMessage(mActiveSession, message, false);
            }
        }
        if (mAgentRuntime != null && mActiveSession != null)
            bindAgentSnapshotToChat(mAgentRuntime.snapshot(mActiveSession.id));

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

        TextView shellName = terminalMetaText("Terminal for this chat");
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

        Button copyCd = createSecondaryButton("Copy chat folder");
        copyCd.setOnClickListener(v -> {
            copyToClipboard("Chat folder", mActiveSession.workspacePath);
            setStatus("Chat folder copied");
        });
        pane.addView(copyCd, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));

        mStatusView = terminalMetaText(mPromptRunning ? "Working" : "Ready");
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dp(18), 0, 0);
        pane.addView(mStatusView, statusParams);

        return scrollView;
    }

    private View getOrCreateDisplayPane() {
        if (mDisplayPane != null) return mDisplayPane;

        UiSpec ui = ui();
        FrameLayout pane = new FrameLayout(this);
        pane.setBackgroundColor(ui.displayHome.fill);

        if (mActiveSession == null) return pane;

        FrameLayout screen = new FrameLayout(this);
        screen.setPadding(0, 0, 0, 0);
        screen.setBackgroundColor(Color.BLACK);
        pane.addView(screen, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (usesNativeDisplay()) {
            mNativeDisplayView = new LorieView(this);
            mNativeDisplayView.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (right - left <= 0 || bottom - top <= 0
                        || right - left == oldRight - oldLeft
                            && bottom - top == oldBottom - oldTop) {
                        return;
                    }
                    view.post(() -> {
                        if (mRuntimeReady && mActiveSession != null)
                            writeRuntimeContract(mActiveSession);
                    });
                });
            screen.addView(mNativeDisplayView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            mDisplayWebView = new WebView(this);
            mDisplayRetryCount = 0;
            mDisplayWebView.setBackgroundColor(Color.BLACK);
            mDisplayWebView.getSettings().setJavaScriptEnabled(true);
            mDisplayWebView.getSettings().setDomStorageEnabled(true);
            mDisplayWebView.getSettings().setLoadWithOverviewMode(true);
            mDisplayWebView.getSettings().setUseWideViewPort(true);
            mDisplayWebView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
            mDisplayWebView.addJavascriptInterface(new DisplayBridge(), "OminalDisplay");
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
                    if (failingUrl != null && failingUrl.startsWith("http://127.0.0.1:6080/")) {
                        showDisplayState("Screen connection failed");
                        retryDisplayLoad();
                    }
                }
            });
            mDisplayWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage message) {
                    Logger.logDebug(LOG_TAG, "Screen viewer: " + message.message());
                    return true;
                }
            });
            screen.addView(mDisplayWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        mDisplayAvailabilityView = new TextView(this);
        mDisplayAvailabilityView.setText("Starting screen...");
        mDisplayAvailabilityView.setTextColor(Color.rgb(174, 174, 174));
        mDisplayAvailabilityView.setTextSize(15);
        mDisplayAvailabilityView.setGravity(Gravity.CENTER);
        mDisplayAvailabilityView.setBackgroundColor(Color.BLACK);
        mDisplayAvailabilityView.setClickable(false);
        screen.addView(mDisplayAvailabilityView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        pane.addView(createDisplayOverlay(), new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mDisplayPane = pane;
        startDisplayAfterFirstLayout();
        return mDisplayPane;
    }

    private void startDisplayAfterFirstLayout() {
        if (!usesNativeDisplay() || mNativeDisplayView == null) {
            if (mDisplayReady) showLiveDisplay();
            else ensureDisplayServerStarted(true);
            return;
        }
        if (mNativeDisplayView.getWidth() > 0 && mNativeDisplayView.getHeight() > 0) {
            startDisplayWhenViewportStable(0);
            return;
        }
        mNativeDisplayView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right <= left || bottom <= top) return;
                view.removeOnLayoutChangeListener(this);
                view.post(() -> startDisplayWhenViewportStable(0));
            }
        });
    }

    private void startDisplayWhenViewportStable(int attempt) {
        if (mNativeDisplayView == null) return;
        OminalDisplayGeometry expected = expectedDisplayGeometry();
        boolean stable = mDisplayInsetsReady
            && mNativeDisplayView.getWidth() == expected.widthPixels
            && mNativeDisplayView.getHeight() == expected.heightPixels;
        if (!stable && attempt < 20) {
            mNativeDisplayView.postDelayed(
                () -> startDisplayWhenViewportStable(attempt + 1), 16);
            return;
        }
        if (mDisplayReady) showLiveDisplay();
        else ensureDisplayServerStarted(true);
    }

    private void showLiveDisplay() {
        if (usesNativeDisplay() && !isNativeDesktopReady()) mDisplayReady = false;
        if (!mDisplayReady) {
            setDisplayLifecycleState(DISPLAY_STATE_STARTING);
            ensureDisplayServerStarted(true);
            setStatus("Opening screen");
            return;
        }
        setDisplayLifecycleState(DISPLAY_STATE_READY_IDLE);
        syncDisplayActivityFromDisk();
        if (usesNativeDisplay() && mNativeDisplayView != null) {
            mNativeDisplayView.refreshDisplaySize();
            hideDisplayState();
        } else {
            loadDisplayWebView();
        }
    }

    private View createDisplayOverlay() {
        FrameLayout overlay = new FrameLayout(this);

        mDisplayActivityBorder = new DisplayActivityBorderView(this);
        mDisplayActivityBorder.setVisibility(View.GONE);
        mDisplayActivityBorder.setClickable(false);
        mDisplayActivityBorder.setFocusable(false);
        overlay.addView(mDisplayActivityBorder, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mDisplayAgentStatusView = new LinearLayout(this);
        mDisplayAgentStatusView.setOrientation(LinearLayout.HORIZONTAL);
        mDisplayAgentStatusView.setGravity(Gravity.CENTER_VERTICAL);
        mDisplayAgentStatusView.setPadding(dp(10), dp(7), dp(12), dp(7));
        mDisplayAgentStatusView.setBackground(makeRoundedDrawable(
            Color.argb(188, 0, 0, 0), Color.argb(72, 255, 255, 255), dp(18)));
        mDisplayAgentStatusView.setVisibility(View.GONE);
        mDisplayAgentStatusView.setClickable(false);
        mDisplayAgentStatusView.setFocusable(false);

        mDisplayAgentPulse = new WorkPulseView(this,
            Color.rgb(148, 148, 148), Color.rgb(242, 242, 242));
        mDisplayAgentStatusView.addView(mDisplayAgentPulse,
            new LinearLayout.LayoutParams(dp(22), dp(18)));

        mDisplayAgentStatusText = new TextView(this);
        mDisplayAgentStatusText.setTextColor(Color.rgb(242, 242, 242));
        mDisplayAgentStatusText.setTextSize(12);
        mDisplayAgentStatusText.setSingleLine(true);
        mDisplayAgentStatusText.setMaxWidth(dp(250));
        mDisplayAgentStatusText.setEllipsize(TextUtils.TruncateAt.END);
        mDisplayAgentStatusView.addView(mDisplayAgentStatusText,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP | Gravity.START;
        overlay.addView(mDisplayAgentStatusView, statusParams);

        if (mAgentRuntime != null && mActiveSession != null)
            updateDisplayAgentStatus(mAgentRuntime.snapshot(mActiveSession.id));
        return overlay;
    }

    private static final class DisplayActivityBorderView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;

        DisplayActivityBorderView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f * density);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(Color.argb(238, 255, 255, 255));
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = Math.max(2f * density, paint.getStrokeWidth() / 2f);
            float right = getWidth() - inset;
            float bottom = getHeight() - inset;
            if (right <= inset || bottom <= inset) return;
            float radius = Math.min(22f * density,
                Math.min(right - inset, bottom - inset) / 2f);
            canvas.drawRoundRect(new RectF(inset, inset, right, bottom),
                radius, radius, paint);
        }
    }

    private void loadDisplayWebView() {
        if (mDisplayWebView != null) {
            showDisplayState("Starting screen...");
            hideViewerChrome(mDisplayWebView);
            mDisplayUrlLoaded = false;
            mDisplayWebView.loadUrl(displayUrl());
        }
    }

    private String displayUrl() {
        return DISPLAY_URL + "?_ominal=" + System.currentTimeMillis();
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
            + "style.textContent='html,body,#screen{margin:0!important;width:100%!important;height:100%!important;overflow:hidden!important;background:#000!important;cursor:none!important;touch-action:none!important;}canvas{display:block!important;width:100%!important;height:100%!important;max-width:none!important;max-height:none!important;margin:0!important;object-fit:fill!important;background:#000!important;image-rendering:auto!important;cursor:none!important;}canvas:focus{outline:none!important;}';"
            + "var screen=document.getElementById('screen');if(screen){screen.style.position='fixed';screen.style.inset='0';screen.style.width='100vw';screen.style.height='100vh';}"
            + "Array.prototype.forEach.call(document.getElementsByTagName('canvas'),function(canvas){canvas.style.width='100%';canvas.style.height='100%';canvas.style.maxWidth='none';canvas.style.maxHeight='none';canvas.style.objectFit='fill';canvas.style.margin='0';canvas.style.cursor='none';});"
            + "document.body.style.margin='0';document.body.style.background='#000';"
            + "})()";
        view.evaluateJavascript(script, null);
        view.postDelayed(() -> view.evaluateJavascript(script, null), 1000);
        view.postDelayed(() -> view.evaluateJavascript(script, null), 3000);
    }

    private void showDisplayState(String message) {
        if (mDisplayAvailabilityView == null) return;
        mDisplayAvailabilityView.setText(message);
        mDisplayAvailabilityView.setVisibility(View.VISIBLE);
    }

    private void hideDisplayState() {
        if (mDisplayAvailabilityView != null) mDisplayAvailabilityView.setVisibility(View.GONE);
    }

    private final class DisplayBridge {
        @JavascriptInterface
        public void state(String state, String detail) {
            runOnUiThread(() -> {
                if ("connected".equals(state)) {
                    mDisplayUrlLoaded = true;
                    mDisplayReady = true;
                    setDisplayLifecycleState(DISPLAY_STATE_READY_IDLE);
                    hideDisplayState();
                    return;
                }
                setDisplayLifecycleState(DISPLAY_STATE_STARTING);
                String message = "Connecting".equals(detail) || TextUtils.isEmpty(detail)
                    ? "Starting screen..." : detail;
                showDisplayState(message);
            });
        }
    }

    private void ensureDisplayServerStarted(boolean reloadWhenReady) {
        if (!mBootstrapReady || mActiveSession == null) return;
        if (usesNativeDisplay() && mNativeDisplayView == null) {
            if (mRootFrame != null) mRootFrame.post(this::prewarmDisplaySurface);
            return;
        }
        long now = System.currentTimeMillis();
        if (mDisplayStartInFlight) {
            if (reloadWhenReady) mReloadDisplayWhenReady = true;
            return;
        }
        if (reloadWhenReady && mDisplayReady && now - mDisplayLastStartedAt < 8000) {
            setDisplayLifecycleState(DISPLAY_STATE_READY_IDLE);
            showLiveDisplay();
            return;
        }

        mDisplayStartInFlight = true;
        setDisplayLifecycleState(DISPLAY_STATE_STARTING);
        mReloadDisplayWhenReady = reloadWhenReady;
        mDisplayLastStartedAt = now;

        if (usesNativeDisplay()) startNativeDisplaySurface();

        new Thread(() -> {
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
                new String[]{"-lc", DISPLAY_START_COMMAND},
                null,
                mActiveSession.workspacePath,
                ExecutionCommand.Runner.APP_SHELL.getName(),
                false);
            command.commandLabel = "Open screen";

            HashMap<String, String> environment = new HashMap<>();
            environment.put("OMINAL_DISPLAY", ":20");
            environment.put("OMINAL_DISPLAY_GEOMETRY", getDisplayGeometry());
            environment.put("OMINAL_DISPLAY_DPI", Integer.toString(currentDisplayGeometry().densityDpi));
            environment.put("OMINAL_DISPLAY_BACKEND", usesNativeDisplay() ? "native" : "novnc");
            environment.put("OMINAL_WORKDIR", mActiveSession.workspacePath);

            AppShell.execute(this, command, null, new OminalShellEnvironment(), environment, true);
            boolean ready = waitForDisplayEndpoint();
            runOnUiThread(() -> {
                mDisplayReady = ready;
                mDisplayStartInFlight = false;
                boolean shouldReload = mReloadDisplayWhenReady;
                mReloadDisplayWhenReady = false;
                if (ready) {
                    mDisplayRetryCount = 0;
                    setDisplayLifecycleState(DISPLAY_STATE_READY_IDLE);
                    setStatus("Screen ready");
                    if (shouldReload) showLiveDisplay();
                } else {
                    setDisplayLifecycleState(DISPLAY_STATE_ERROR);
                    setStatus("Screen unavailable");
                    mDisplayStartupDetail = "The screen didn't open. Try again.";
                    if (mDisplayAvailabilityView != null)
                        mDisplayAvailabilityView.setText(mDisplayStartupDetail);
                    if (usesNativeDisplay() && mMode == MODE_DISPLAY
                        && mDisplayRetryCount < 2 && mRootFrame != null) {
                        mDisplayRetryCount++;
                        mRootFrame.postDelayed(() -> ensureDisplayServerStarted(true), 1500);
                    }
                }
            });
        }).start();
    }

    private boolean waitForDisplayEndpoint() {
        if (usesNativeDisplay()) {
            File readyMarker = new File(
                OminalConstants.OMINAL_HOME_DIR_PATH, ".ominal/display/ready");
            for (int attempt = 0; attempt < NATIVE_DISPLAY_HEALTH_RETRIES; attempt++) {
                if (LorieView.connected() && readyMarker.isFile()) return true;
                try {
                    Thread.sleep(125);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }
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

    private boolean usesNativeDisplay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
        for (String abi : Build.SUPPORTED_ABIS)
            if ("arm64-v8a".equals(abi)) return true;
        return false;
    }

    private void startNativeDisplaySurface() {
        if (mNativeDisplayView == null) return;
        OminalDisplayGeometry geometry = currentDisplayGeometry();
        File runtimeRoot = new File(OminalConstants.OMINAL_HOME_DIR_PATH, ".ominal/runtime");
        File temporaryDirectory = new File(runtimeRoot, "tmp");
        File xkbConfigRoot = new File(runtimeRoot, "linux/rootfs/usr/share/X11/xkb");
        OminalNativeDisplay.startAndConnect(mNativeDisplayView, temporaryDirectory, xkbConfigRoot,
            geometry.densityDpi, new OminalNativeDisplay.Callback() {
                @Override
                public void onConnected() {
                    if (mDisplayReady && isNativeDesktopReady()) {
                        setDisplayLifecycleState(DISPLAY_STATE_READY_IDLE);
                        hideDisplayState();
                        if (mMode == MODE_DISPLAY) showLiveDisplay();
                    } else {
                        setDisplayLifecycleState(DISPLAY_STATE_STARTING);
                        showDisplayState("Starting screen...");
                        ensureDisplayServerStarted(mMode == MODE_DISPLAY);
                    }
                }

                @Override
                public void onDisconnected() {
                    mDisplayReady = false;
                    setDisplayLifecycleState(DISPLAY_STATE_STARTING);
                    showDisplayState("Reconnecting screen...");
                }

                @Override
                public void onError(String message) {
                    mDisplayReady = false;
                    setDisplayLifecycleState(DISPLAY_STATE_ERROR);
                    showDisplayState(message);
                    Logger.logError(LOG_TAG, message);
                }
            });
    }

    private void restoreNativeDisplayConnection() {
        if (!usesNativeDisplay() || mNativeDisplayView == null || LorieView.connected()) return;
        mDisplayReady = false;
        setDisplayLifecycleState(DISPLAY_STATE_STARTING);
        showDisplayState("Reconnecting screen...");
        startNativeDisplaySurface();
    }

    private boolean isNativeDesktopReady() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH,
            ".ominal/display/ready").isFile();
    }

    private void submitPrompt() {
        if (!mBootstrapReady) {
            addTransientSystemMessage("Ominal is still getting ready.");
            return;
        }
        if (mActiveSession == null) return;

        String prompt = mPromptInput.getText().toString().trim();
        if (prompt.isEmpty()) return;

        mPromptInput.setText("");
        if ("New chat".equals(mActiveSession.title))
            renameSessionFromPrompt(mActiveSession, prompt);

        ChatMessage userMessage = new ChatMessage("user", prompt, nowLabel());
        appendMessage(mActiveSession, userMessage, true);
        if (handleMonolithCommand(mActiveSession, prompt)) return;
        PendingTurn turn = new PendingTurn(prompt, mActiveSession.harnessId,
            activeModelId(mActiveSession), activeEffortId(mActiveSession),
            mActiveSession.messages.size() - 1);
        OminalAgentRuntime.Snapshot current = mAgentRuntime == null
            ? null : mAgentRuntime.snapshot(mActiveSession.id);
        if (current != null && !current.isIdle()) {
            mActiveSession.pendingTurns.add(turn);
            saveMeta(mActiveSession);
            updateHarnessControls();
            setStatus("Queued  /  " + mActiveSession.pendingTurns.size());
            setInputEnabled(true);
            return;
        }
        startPendingTurn(mActiveSession, turn);
    }

    private boolean handleMonolithCommand(ChatSession session, String prompt) {
        if (session == null || prompt == null || !prompt.startsWith("/")
            || prompt.startsWith("//")) {
            return false;
        }
        String[] parts = prompt.trim().split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        switch (command) {
            case "/harness":
                if (argument.isEmpty()) {
                    appendCommandResponse(session, harnessCatalogText(session));
                    return true;
                }
                String harnessId = normalizeHarnessCommandId(argument);
                if (!OminalHarnessRegistry.isSelectable(harnessId)) {
                    appendCommandResponse(session,
                        "Unknown harness `" + argument + "`. Use `/harness` to list them.");
                    return true;
                }
                selectHarness(session, harnessId);
                appendCommandResponse(session, "Using `" + harnessId + "`.");
                return true;
            case "/login":
                appendCommandResponse(session, "Opening `" + session.harnessId + "`.");
                launchHarnessTerminal(session.harnessId, false);
                return true;
            case "/model":
                OminalHarnessManifest manifest = OminalHarnessManifest.load(session.harnessId);
                if (manifest == null || manifest.modelFlag.isEmpty()
                    || manifest.models.isEmpty()) {
                    return false;
                }
                if (argument.isEmpty()) {
                    appendCommandResponse(session, modelCatalogText(session, manifest));
                    return true;
                }
                OminalHarnessManifest.Model selectedModel = findModel(manifest, argument);
                if (selectedModel == null) {
                    appendCommandResponse(session,
                        "Unknown model `" + argument + "`. Use `/model` to list them.");
                    return true;
                }
                session.setModelId(selectedModel.id);
                if (!selectedModel.efforts.contains(session.effortId()))
                    session.setEffortId("");
                saveMeta(session);
                updateHarnessControls();
                appendCommandResponse(session, "Using `" + selectedModel.label + "`.");
                return true;
            case "/effort":
                OminalHarnessManifest effortManifest =
                    OminalHarnessManifest.load(session.harnessId);
                List<String> efforts = availableEfforts(effortManifest);
                if (effortManifest == null || effortManifest.effortFlag.isEmpty()
                    || efforts.isEmpty()) {
                    return false;
                }
                if (argument.isEmpty()) {
                    appendCommandResponse(session, effortCatalogText(session, efforts));
                    return true;
                }
                if (!efforts.contains(argument.toLowerCase(Locale.ROOT))) {
                    appendCommandResponse(session,
                        "Unknown effort `" + argument + "`. Use `/effort` to list them.");
                    return true;
                }
                session.setEffortId(argument.toLowerCase(Locale.ROOT));
                saveMeta(session);
                updateHarnessControls();
                appendCommandResponse(session, "Using `" + session.effortId() + "` effort.");
                return true;
            case "/capabilities":
            case "/commands":
                OminalHarnessManifest capabilities =
                    OminalHarnessManifest.load(session.harnessId);
                if (capabilities == null) {
                    refreshHarnessCapabilities(session.harnessId);
                    appendCommandResponse(session, "Harness capabilities are being refreshed.");
                } else {
                    appendCommandResponse(session,
                        capabilityCatalogText(session, capabilities));
                }
                return true;
            case "/refresh":
                refreshHarnessCapabilities(session.harnessId);
                appendCommandResponse(session, "Refreshing harness capabilities.");
                return true;
            case "/terminal":
                openTerminalForActiveChat();
                return true;
            case "/computer":
            case "/display":
                switchMode(MODE_DISPLAY);
                return true;
            default:
                // Slash commands not owned by Monolith belong to the active harness.
                return false;
        }
    }

    private String harnessCatalogText(ChatSession session) {
        StringBuilder text = new StringBuilder("Harnesses:");
        for (OminalAgentHarness harness : OminalHarnessRegistry.all()) {
            if (!harness.isAvailable()) continue;
            text.append("\n- `").append(harness.getId()).append("`");
            if (session != null && harness.getId().equals(session.harnessId))
                text.append(" (current)");
        }
        text.append("\n\nSwitch with `/harness <id>`.");
        return text.toString();
    }

    private String modelCatalogText(ChatSession session, OminalHarnessManifest manifest) {
        StringBuilder text = new StringBuilder("Models:");
        String selected = session == null ? "" : session.modelId();
        for (OminalHarnessManifest.Model model : manifest.models) {
            text.append("\n- `").append(model.id).append("`");
            if (model.id.equals(selected)) text.append(" (current)");
        }
        text.append("\n\nSwitch with `/model <id>`.");
        return text.toString();
    }

    private String effortCatalogText(ChatSession session, List<String> efforts) {
        StringBuilder text = new StringBuilder("Effort:");
        String selected = session == null ? "" : session.effortId();
        text.append("\n- `auto`");
        if (selected.isEmpty()) text.append(" (current)");
        for (String effort : efforts) {
            text.append("\n- `").append(effort).append("`");
            if (effort.equals(selected)) text.append(" (current)");
        }
        text.append("\n\nSwitch with `/effort <level>`.");
        return text.toString();
    }

    private String capabilityCatalogText(ChatSession session,
                                         OminalHarnessManifest manifest) {
        StringBuilder text = new StringBuilder();
        text.append("Harness `").append(manifest.harnessId).append("` ")
            .append(manifest.binaryVersion);
        if (!manifest.models.isEmpty())
            text.append("\n\n").append(modelCatalogText(session, manifest));
        List<String> efforts = availableEfforts(manifest);
        if (!efforts.isEmpty())
            text.append("\n\n").append(effortCatalogText(session, efforts));
        if (!manifest.commands.isEmpty()) {
            text.append("\n\nCommands:");
            for (OminalHarnessManifest.Command command : manifest.commands)
                text.append("\n- `").append(command.name).append("`");
        }
        return text.toString();
    }

    private OminalHarnessManifest.Model findModel(OminalHarnessManifest manifest,
                                                   String requestedId) {
        String id = requestedId == null ? "" : requestedId.trim();
        for (OminalHarnessManifest.Model model : manifest.models)
            if (model.id.equals(id)) return model;
        return null;
    }

    private String activeModelId(ChatSession session) {
        if (session == null || session.modelId().isEmpty()) return "";
        return validatedModelId(session.harnessId, session.modelId());
    }

    private String validatedModelId(String harnessId, String modelId) {
        if (modelId == null || modelId.isEmpty()) return "";
        OminalHarnessManifest manifest = OminalHarnessManifest.load(harnessId);
        return manifest != null && !manifest.modelFlag.isEmpty()
            && findModel(manifest, modelId) != null ? modelId : "";
    }

    private List<String> availableEfforts(OminalHarnessManifest manifest) {
        if (manifest == null || manifest.effortFlag.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> efforts = new LinkedHashSet<>();
        for (OminalHarnessManifest.Model model : manifest.models)
            efforts.addAll(model.efforts);
        return new ArrayList<>(efforts);
    }

    private String activeEffortId(ChatSession session) {
        if (session == null || session.effortId().isEmpty()) return "";
        return validatedEffortId(session.harnessId, session.effortId());
    }

    private String validatedEffortId(String harnessId, String effortId) {
        if (effortId == null || effortId.isEmpty()) return "";
        OminalHarnessManifest manifest = OminalHarnessManifest.load(harnessId);
        return availableEfforts(manifest).contains(effortId) ? effortId : "";
    }

    private String normalizeHarnessCommandId(String value) {
        String id = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("claude".equals(id)) return OminalHarnessTerminal.CLAUDE_CODE_ID;
        if ("agy".equals(id)) return OminalHarnessTerminal.ANTIGRAVITY_ID;
        return id;
    }

    private void selectHarness(ChatSession session, String harnessId) {
        session.harnessId = OminalHarnessRegistry.normalizeSelectedId(harnessId);
        if (mPrefs != null) {
            mPrefs.edit()
                .putString(OminalHarnessRegistry.PREFERENCE_KEY, session.harnessId)
                .apply();
        }
        saveMeta(session);
        writeRuntimeContract(session);
        styleAccountButton();
        updateHarnessControls();
        refreshHarnessCapabilities(session.harnessId);
        setStatus("Ready");
    }

    private void updateHarnessControls() {
        if (mHarnessControlsButton == null) return;
        if (mActiveSession == null) {
            mHarnessControlsButton.setVisibility(View.GONE);
            if (mHarnessContextView != null) mHarnessContextView.setVisibility(View.GONE);
            return;
        }
        OminalAgentHarness harness =
            OminalHarnessRegistry.activeOrDefault(mActiveSession.harnessId);
        OminalHarnessManifest manifest =
            OminalHarnessManifest.load(mActiveSession.harnessId);
        StringBuilder label = new StringBuilder(harness.getDisplayName());
        String modelLabel = "Auto";
        if (manifest == null) {
            if (!OminalHarnessTerminal.CODEX_ID.equals(mActiveSession.harnessId)) {
                label.append("  ·  setting up");
                modelLabel = "Setting up";
            }
        } else {
            String modelId = activeModelId(mActiveSession);
            String effortId = activeEffortId(mActiveSession);
            OminalHarnessManifest.Model model = findModel(manifest, modelId);
            label.append("  ·  ").append(model == null ? "Automatic" : model.label);
            modelLabel = model == null ? "Auto" : model.label;
            if (!availableEfforts(manifest).isEmpty())
                label.append("  ·  ").append(effortId.isEmpty() ? "Auto effort" : effortId);
        }
        if (!mActiveSession.pendingTurns.isEmpty())
            label.append("  ·  ").append(mActiveSession.pendingTurns.size()).append(" queued");
        mHarnessControlsButton.setContentDescription(
            "Agent controls. " + label.toString().replace("  ·  ", ", "));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mHarnessControlsButton.setTooltipText("Agent controls");
        mHarnessControlsButton.setVisibility(View.VISIBLE);
        if (mHarnessContextView != null) {
            mHarnessContextView.setText(harness.getDisplayName() + "  ·  " + modelLabel);
            mHarnessContextView.setContentDescription(label.toString().replace("  ·  ", ", "));
            mHarnessContextView.setVisibility(View.VISIBLE);
        }
    }

    private void showHarnessControlsDialog() {
        if (mActiveSession == null) return;
        OminalHarnessManifest manifest =
            OminalHarnessManifest.load(mActiveSession.harnessId);
        ArrayList<OminalInteractionSheet.Section> sections = new ArrayList<>();

        ArrayList<OminalInteractionSheet.Row> harnessRows = new ArrayList<>();
        for (OminalAgentHarness harness : OminalHarnessRegistry.all()) {
            if (!harness.isAvailable()) continue;
            boolean selected = harness.getId().equals(mActiveSession.harnessId);
            harnessRows.add(new OminalInteractionSheet.Row("harness:" + harness.getId(),
                harness.getDisplayName(), selected ? "Active for this conversation" : "Switch agent",
                selected ? "Current" : "", selected, true, false));
        }
        sections.add(new OminalInteractionSheet.Section("Agent", harnessRows));

        ArrayList<OminalInteractionSheet.Row> configurationRows = new ArrayList<>();
        if (manifest == null) {
            String discoveryError = mHarnessDiscoveryErrors.get(mActiveSession.harnessId);
            if (TextUtils.isEmpty(discoveryError)) {
                configurationRows.add(new OminalInteractionSheet.Row("loading", "Loading controls",
                    "Reading models and commands from the active agent", "",
                    false, false, false));
                refreshHarnessCapabilities(mActiveSession.harnessId);
            } else {
                configurationRows.add(new OminalInteractionSheet.Row("refresh",
                    "Controls unavailable", "The active agent did not return its catalog",
                    "Retry", false, true, false));
            }
        } else {
            if (!manifest.models.isEmpty() && !manifest.modelFlag.isEmpty()) {
                String selectedId = activeModelId(mActiveSession);
                OminalHarnessManifest.Model selectedModel = findModel(manifest, selectedId);
                configurationRows.add(new OminalInteractionSheet.Row("model", "Model",
                    selectedModel == null ? "Selected automatically" : selectedModel.label,
                    Integer.toString(manifest.models.size()), false, true, false));
            }
            List<String> efforts = availableEfforts(manifest);
            if (!efforts.isEmpty()) {
                String selected = activeEffortId(mActiveSession);
                configurationRows.add(new OminalInteractionSheet.Row("effort", "Effort",
                    selected.isEmpty() ? "Selected automatically" : selected,
                    Integer.toString(efforts.size()), false, true, false));
            }
            if (!manifest.commands.isEmpty()) {
                configurationRows.add(new OminalInteractionSheet.Row("commands", "Commands",
                    "Commands reported by the active agent",
                    Integer.toString(manifest.commands.size()), false, true, false));
            }
            configurationRows.add(new OminalInteractionSheet.Row("refresh", "Refresh",
                "Read installed capabilities again", "", false, true, false));
        }
        sections.add(new OminalInteractionSheet.Section("Controls", configurationRows));

        String agentName = OminalHarnessRegistry.activeOrDefault(
            mActiveSession.harnessId).getDisplayName();
        OminalInteractionSheet.show(this, interactionSheetTheme(), "Agent",
            agentName + " is active for this conversation.", sections, id -> {
                if (id.startsWith("harness:")) {
                    selectHarness(mActiveSession, id.substring("harness:".length()));
                    if (mRootFrame != null)
                        mRootFrame.postDelayed(this::showHarnessControlsDialog, 180);
                } else if ("model".equals(id) && manifest != null) {
                    showHarnessModelPicker(manifest);
                } else if ("effort".equals(id) && manifest != null) {
                    showHarnessEffortPicker(manifest);
                } else if ("commands".equals(id) && manifest != null) {
                    showHarnessCommands(manifest);
                } else if ("refresh".equals(id)) {
                    refreshHarnessCapabilities(mActiveSession.harnessId);
                    setStatus("Refreshing controls");
                }
            });
    }

    private void showHarnessModelPicker(OminalHarnessManifest manifest) {
        if (mActiveSession == null) return;
        ArrayList<OminalInteractionSheet.Row> models = new ArrayList<>();
        models.add(new OminalInteractionSheet.Row("", "Automatic",
            "Let the active agent choose", "", mActiveSession.modelId().isEmpty(), true, false));
        for (OminalHarnessManifest.Model model : manifest.models) {
            String detail = model.efforts.isEmpty() ? model.id
                : model.id + " · " + model.efforts.size() + " effort levels";
            models.add(new OminalInteractionSheet.Row(model.id, model.label, detail, "",
                model.id.equals(mActiveSession.modelId()), true, false));
        }
        OminalInteractionSheet.showChoices(this, interactionSheetTheme(), "Model",
            "Available models are reported by the installed agent.", models, id -> {
                mActiveSession.setModelId(id);
                OminalHarnessManifest.Model selected = findModel(manifest, id);
                if (selected != null && !selected.efforts.contains(mActiveSession.effortId()))
                    mActiveSession.setEffortId("");
                saveMeta(mActiveSession);
                updateHarnessControls();
            });
    }

    private void showHarnessEffortPicker(OminalHarnessManifest manifest) {
        if (mActiveSession == null) return;
        List<String> efforts = availableEfforts(manifest);
        ArrayList<OminalInteractionSheet.Row> choices = new ArrayList<>();
        choices.add(new OminalInteractionSheet.Row("", "Automatic",
            "Use the model default", "", mActiveSession.effortId().isEmpty(), true, false));
        for (String effort : efforts) {
            choices.add(new OminalInteractionSheet.Row(effort,
                effort.substring(0, 1).toUpperCase(Locale.ROOT) + effort.substring(1),
                "Reasoning effort", "", effort.equals(mActiveSession.effortId()), true, false));
        }
        OminalInteractionSheet.showChoices(this, interactionSheetTheme(), "Effort",
            "Controls how much work the selected model spends before responding.",
            choices, id -> {
                mActiveSession.setEffortId(id);
                saveMeta(mActiveSession);
                updateHarnessControls();
            });
    }

    private void showHarnessCommands(OminalHarnessManifest manifest) {
        if (manifest.commands.isEmpty()) {
            Toast.makeText(this, "No native commands reported", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<OminalInteractionSheet.Row> commands = new ArrayList<>();
        for (OminalHarnessManifest.Command command : manifest.commands) {
            commands.add(new OminalInteractionSheet.Row(command.name, command.name,
                commandTypeLabel(command.type), "", false, true, false));
        }
        OminalInteractionSheet.showChoices(this, interactionSheetTheme(), "Commands",
            "Insert a command from the active agent.", commands,
            this::completeComposerCommand);
    }

    private String commandTypeLabel(String type) {
        if (type == null || type.isEmpty() || "command".equals(type)) return "Agent command";
        return type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1) + " command";
    }

    private void refreshHarnessCapabilities(String harnessId) {
        if (!mBootstrapReady || !mRuntimeReady
            || !OminalHarnessRegistry.isSelectable(harnessId)) {
            return;
        }
        synchronized (mHarnessDiscoveryInFlight) {
            if (!mHarnessDiscoveryInFlight.add(harnessId)) return;
        }
        mHarnessDiscoveryErrors.remove(harnessId);
        if (OminalHarnessTerminal.CODEX_ID.equals(harnessId)) {
            ChatSession session = mActiveSession;
            boolean started = session != null && mAgentRuntime != null
                && mAgentRuntime.refreshCodexCapabilities(session.id, codexServerEnvironment(),
                    new OminalCodexAppServer.CapabilityListener() {
                        @Override
                        public void onReady() {
                            finishHarnessCapabilityRefresh(harnessId, "");
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            finishHarnessCapabilityRefresh(harnessId, message);
                        }
                    });
            if (!started) {
                synchronized (mHarnessDiscoveryInFlight) {
                    mHarnessDiscoveryInFlight.remove(harnessId);
                }
            }
            return;
        }
        new Thread(() -> {
            ExecutionCommand command = new ExecutionCommand(-1,
                OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-harness-chat",
                new String[]{harnessId, "discover"}, null,
                OminalConstants.OMINAL_HOME_DIR_PATH,
                ExecutionCommand.Runner.APP_SHELL.getName(), false);
            command.commandLabel = "Read " + harnessId + " capabilities";
            AppShell.execute(this, command, null, new OminalShellEnvironment(),
                codexServerEnvironment(), true);
            synchronized (mHarnessDiscoveryInFlight) {
                mHarnessDiscoveryInFlight.remove(harnessId);
            }
            runOnUiThread(() -> {
                if (mActiveSession != null && harnessId.equals(mActiveSession.harnessId)) {
                    updateHarnessControls();
                    renderCommandSuggestions(
                        mPromptInput == null ? "" : mPromptInput.getText().toString());
                }
            });
        }, "ominal-harness-discovery-" + harnessId).start();
    }

    private void finishHarnessCapabilityRefresh(String harnessId, String error) {
        synchronized (mHarnessDiscoveryInFlight) {
            mHarnessDiscoveryInFlight.remove(harnessId);
        }
        runOnUiThread(() -> {
            if (TextUtils.isEmpty(error)) mHarnessDiscoveryErrors.remove(harnessId);
            else mHarnessDiscoveryErrors.put(harnessId, error);
            if (!TextUtils.isEmpty(error)) setStatus("Agent controls unavailable");
            if (mActiveSession != null && harnessId.equals(mActiveSession.harnessId)) {
                updateHarnessControls();
                renderCommandSuggestions(
                    mPromptInput == null ? "" : mPromptInput.getText().toString());
            }
        });
    }

    private void appendCommandResponse(ChatSession session, String text) {
        appendMessage(session, new ChatMessage("assistant", text, nowLabel()), true);
        if (session == mActiveSession) scrollToBottom();
    }

    private void renderCommandSuggestions(String input) {
        if (mCommandSuggestionsView == null || mCommandSuggestionsRow == null) return;
        mCommandSuggestionsRow.removeAllViews();
        if (input == null || !input.startsWith("/") || input.contains("\n")) {
            mCommandSuggestionsView.setVisibility(View.GONE);
            return;
        }

        String lower = input.toLowerCase(Locale.ROOT);
        OminalHarnessManifest manifest = mActiveSession == null
            ? null : OminalHarnessManifest.load(mActiveSession.harnessId);
        if (lower.startsWith("/harness ")) {
            String query = lower.substring("/harness ".length()).trim();
            for (OminalAgentHarness harness : OminalHarnessRegistry.all()) {
                if (!harness.isAvailable() || !harness.getId().startsWith(query)) continue;
                addCommandSuggestion(harness.getDisplayName(), "Agent",
                    "/harness " + harness.getId());
            }
        } else if (lower.startsWith("/model ") && manifest != null
            && !manifest.modelFlag.isEmpty()) {
            String query = lower.substring("/model ".length()).trim();
            for (OminalHarnessManifest.Model model : manifest.models) {
                if (!model.id.toLowerCase(Locale.ROOT).startsWith(query)
                    && !model.label.toLowerCase(Locale.ROOT).startsWith(query)) {
                    continue;
                }
                addCommandSuggestion(model.label, "Model", "/model " + model.id);
            }
        } else if (lower.startsWith("/effort ") && manifest != null
            && !manifest.effortFlag.isEmpty()) {
            String query = lower.substring("/effort ".length()).trim();
            for (String effort : availableEfforts(manifest)) {
                if (!effort.startsWith(query)) continue;
                addCommandSuggestion(effort, "Effort", "/effort " + effort);
            }
        } else if (input.indexOf(' ') < 0) {
            LinkedHashSet<String> commands = new LinkedHashSet<>();
            Collections.addAll(commands, "/harness", "/login", "/capabilities",
                "/refresh", "/computer", "/terminal");
            if (manifest != null) {
                commands.addAll(manifest.commandNames());
                if (!manifest.modelFlag.isEmpty() && !manifest.models.isEmpty())
                    commands.add("/model");
                if (!manifest.effortFlag.isEmpty() && !availableEfforts(manifest).isEmpty())
                    commands.add("/effort");
            }
            for (String command : commands) {
                if (!command.startsWith(lower)) continue;
                String completion = "/harness".equals(command) || "/model".equals(command)
                    || "/effort".equals(command)
                    ? command + " " : command;
                addCommandSuggestion(command, commandSuggestionDetail(command, manifest), completion);
            }
        }
        boolean hasSuggestions = mCommandSuggestionsRow.getChildCount() > 0;
        boolean wasVisible = mCommandSuggestionsView.getVisibility() == View.VISIBLE;
        mCommandSuggestionsView.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
        if (hasSuggestions && !wasVisible) {
            mCommandSuggestionsView.setAlpha(0f);
            mCommandSuggestionsView.setTranslationY(dp(8));
            mCommandSuggestionsView.animate().alpha(1f).translationY(0f).setDuration(180)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f)).start();
        }
    }

    private String commandSuggestionDetail(String command, OminalHarnessManifest manifest) {
        switch (command) {
            case "/harness": return "Switch the agent for this conversation";
            case "/login": return "Open the active agent sign-in";
            case "/capabilities": return "Show discovered models and commands";
            case "/refresh": return "Refresh discovered capabilities";
            case "/computer": return "Open the shared computer";
            case "/terminal": return "Open this conversation's terminal";
            case "/model": return "Choose a model";
            case "/effort": return "Choose reasoning effort";
            default:
                if (manifest != null) {
                    for (OminalHarnessManifest.Command value : manifest.commands)
                        if (command.equals(value.name)) return commandTypeLabel(value.type);
                }
                return "Agent command";
        }
    }

    private void addCommandSuggestion(String label, String detail, String completion) {
        if (mCommandSuggestionsRow == null || mCommandSuggestionsRow.getChildCount() >= 5) return;
        UiSpec ui = ui();
        LinearLayout suggestion = new LinearLayout(this);
        suggestion.setOrientation(LinearLayout.VERTICAL);
        suggestion.setGravity(Gravity.CENTER_VERTICAL);
        suggestion.setMinimumHeight(dp(52));
        suggestion.setPadding(dp(13), dp(8), dp(13), dp(8));
        suggestion.setBackground(makeRoundedDrawable(Color.TRANSPARENT, Color.TRANSPARENT, dp(10)));
        suggestion.setOnClickListener(v -> completeComposerCommand(completion));
        attachNativeRipple(suggestion);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(ui.ink);
        title.setTextSize(14);
        title.setTypeface(Typeface.MONOSPACE);
        title.setIncludeFontPadding(false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        suggestion.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView detailView = new TextView(this);
        detailView.setText(detail);
        detailView.setTextColor(ui.muted);
        detailView.setTextSize(12);
        detailView.setIncludeFontPadding(false);
        detailView.setSingleLine(true);
        detailView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(3), 0, 0);
        suggestion.addView(detailView, detailParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mCommandSuggestionsRow.addView(suggestion, params);
    }

    private void completeComposerCommand(String completion) {
        if (mPromptInput == null) return;
        mPromptInput.setText(completion);
        mPromptInput.setSelection(mPromptInput.length());
        requestComposerKeyboard();
    }

    private void submitPromptToTerminal() {
        if (!mBootstrapReady || !mRuntimeReady || !OminalProotTerminal.isReady()) {
            addTransientSystemMessage("Ominal is still getting ready.");
            return;
        }
        if (mActiveSession == null) return;

        String commandLine = mPromptInput.getText().toString().trim();
        if (commandLine.isEmpty()) return;

        ensureDirectory(mActiveSession.workspacePath);
        Intent executeIntent = new Intent(OMINAL_SERVICE.ACTION_SERVICE_EXECUTE);
        executeIntent.setClass(this, OminalService.class);
        OminalProotTerminal.configureIntent(executeIntent, mActiveSession.workspacePath);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_RUNNER,
            ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_NAME, mActiveSession.terminalName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE,
            ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_COMMAND_LABEL, mActiveSession.title);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_STDIN, commandLine);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION,
            Integer.toString(
                OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY));

        try {
            startService(executeIntent);
            mPromptInput.setText("");
            Toast.makeText(this, "Sent to terminal", Toast.LENGTH_SHORT).show();
            mContentFrame.postDelayed(() ->
                startActivity(new Intent(this, OminalActivity.class)), 240);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to send command to terminal", e);
            Toast.makeText(this, "Terminal unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickAttachment() {
        if (!mBootstrapReady) {
            addTransientSystemMessage("Ominal is still getting ready.");
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

    private void startTermuxConfigImport() {
        if (!mBootstrapReady) {
            addTransientSystemMessage("Ominal is still getting ready.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/gzip");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "application/gzip", "application/x-gzip", "application/x-tar"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(Intent.createChooser(intent, "Import Termux settings"),
                REQUEST_IMPORT_TERMUX_CONFIG);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open Termux settings import picker", e);
            addTransientSystemMessage("File picker unavailable.");
        }
    }

    private void handleTermuxConfigImport(Intent data) {
        Uri archiveUri = data.getData();
        if (archiveUri == null) return;
        setStatus("Importing settings");
        new Thread(() -> {
            try {
                OminalTermuxConfigMigration.Result result = OminalTermuxConfigMigration.importArchive(this,
                    archiveUri, new File(OminalConstants.OMINAL_HOME_DIR_PATH));
                runOnUiThread(() -> {
                    setStatus("Ready");
                    addTransientSystemMessage("Imported " + result.copiedEntries
                        + " Termux setting groups.");
                });
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to import Termux settings", e);
                runOnUiThread(() -> {
                    setStatus("Ready");
                    addTransientSystemMessage("Termux settings import failed.");
                });
            }
        }, "ominal-termux-config-import").start();
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
                OminalChatMedia.Item media = OminalChatMedia.fromRelativePath(
                    new File(mActiveSession.workspacePath), relativePath,
                    getContentResolver().getType(uri));
                if (media == null) throw new IOException("Copied attachment is unavailable");
                appendMessage(mActiveSession, new ChatMessage("user", "", nowLabel(),
                    "", OminalAgentTrace.Snapshot.empty(),
                    Collections.singletonList(media)), true);
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to copy attachment", e);
                addTransientSystemMessage("Attachment failed: " + getDisplayName(uri));
            }
        }
        writeRuntimeContract(mActiveSession);
    }

    private void startPendingTurn(ChatSession session, PendingTurn turn) {
        if (session == null || turn == null) return;
        turn.startedAt = System.currentTimeMillis();
        session.activeTurn = turn;
        saveMeta(session);
        AgentTurnView responseView = session == mActiveSession
            ? addAgentTurn("Working", true) : null;
        runPrompt(session, turn, responseView);
    }

    private void startNextPendingTurn(ChatSession session) {
        if (session == null || session.activeTurn != null || session.pendingTurns.isEmpty())
            return;
        PendingTurn next = session.pendingTurns.remove(0);
        saveMeta(session);
        if (session == mActiveSession) updateHarnessControls();
        startPendingTurn(session, next);
    }

    private void resumePendingTurns() {
        if (mAgentRuntime == null) return;
        for (ChatSession session : mSessions) {
            OminalAgentRuntime.Snapshot snapshot = mAgentRuntime.snapshot(session.id);
            if (session.activeTurn != null && snapshot.isIdle()) {
                session.pendingTurns.add(0, session.activeTurn);
                session.activeTurn = null;
                saveMeta(session);
            }
            if (snapshot.isIdle()) startNextPendingTurn(session);
        }
    }

    private void runPrompt(ChatSession session, PendingTurn turn,
                           AgentTurnView responseView) {
        refreshRuntimeDns();
        if (session == mActiveSession) {
            mPromptRunning = true;
            mActiveAgentTurnView = responseView;
            setDisplayNeedsUser(false);
            setInputEnabled(true);
            setStatus("Working");
        }
        ensureDisplayServerStarted(false);
        attachAgentRuntime();

        ensureDirectory(session.workspacePath);
        File eventLog = agentEventLogFile(session);
        if (eventLog.exists() && !eventLog.delete())
            Logger.logWarn(LOG_TAG, "Could not reset the agent event log");
        if (session == mActiveSession) startAgentEventObserver(session, eventLog);

        new Thread(() -> {
            turn.mediaBefore = OminalChatMedia.snapshot(new File(session.workspacePath));
            writeRuntimeContract(session, turn.harnessId);
            boolean harnessCommand = isHarnessCommand(turn.prompt);
            OminalAgentTransport.TurnRequest request = new OminalAgentTransport.TurnRequest(
                turn.harnessId, session.threadId(turn.harnessId), guestWorkspacePath(session),
                harnessCommand ? turn.prompt : buildAgentPrompt(session, turn),
                harnessCommand ? "" : buildAgentDeveloperInstructions(),
                validatedModelId(turn.harnessId, turn.modelId),
                validatedEffortId(turn.harnessId, turn.effortId),
                codexServerEnvironment(session));
            OminalAgentRuntime runtime = mAgentRuntime;
            boolean accepted = runtime != null && runtime.submit(session.id, request);
            if (!accepted) runOnUiThread(() -> {
                boolean running = runtime != null
                    && runtime.snapshot(session.id).isRunning();
                if (session == mActiveSession) {
                    mPromptRunning = running;
                    setInputEnabled(true);
                }
                if (!running) {
                    if (session == mActiveSession) stopAgentEventObserver();
                    failAgentTurn(session, responseView,
                        "The selected harness could not accept this request. Try again.",
                        OminalAgentTrace.Snapshot.empty(), turn.harnessId,
                        turn.userMessageIndex, -1L);
                }
            });
        }).start();
    }

    @SuppressWarnings("deprecation")
    private void startAgentEventObserver(ChatSession session, File eventLog) {
        stopAgentEventObserver();
        File directory = eventLog.getParentFile();
        if (directory == null) return;
        ensureDirectory(directory.getAbsolutePath());
        mObservedAgentSessionId = session.id;
        mObservedAgentEventCount = 0;
        String eventLogName = eventLog.getName();
        mAgentEventObserver = new FileObserver(directory.getAbsolutePath(),
            FileObserver.CREATE | FileObserver.MODIFY | FileObserver.CLOSE_WRITE
                | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                if (!eventLogName.equals(path)) return;
                runOnUiThread(() -> consumeLiveAgentEvents(session.id, eventLog));
            }
        };
        mAgentEventObserver.startWatching();
    }

    private void stopAgentEventObserver() {
        if (mAgentEventObserver != null) {
            mAgentEventObserver.stopWatching();
            mAgentEventObserver = null;
        }
        mObservedAgentSessionId = "";
        mObservedAgentEventCount = 0;
    }

    private void consumeLiveAgentEvents(String sessionId, File eventLog) {
        if (!sessionId.equals(mObservedAgentSessionId)) return;
        List<OminalAgentEventLog.Event> allEvents;
        try {
            allEvents = OminalAgentEventLog.read(eventLog);
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read live agent events", e);
            return;
        }
        if (allEvents.size() <= mObservedAgentEventCount) return;

        ArrayList<OminalAgentEventLog.Event> newEvents = new ArrayList<>(
            allEvents.subList(mObservedAgentEventCount, allEvents.size()));
        mObservedAgentEventCount = allEvents.size();
        OminalAgentEventLog.Summary events = OminalAgentEventLog.summarize(newEvents);
        if (!events.status.isEmpty()) setStatus(events.status);
        if (events.reloadUi) setStatus("Appearance update ready");
        if (!events.openDisplay) return;

        setDisplayNeedsUser(events.userInputRequired);
        setStatus(events.userInputRequired
            ? events.reason.isEmpty() ? "Your input is needed" : events.reason
            : events.reason.isEmpty() ? "Screen open" : events.reason);
        switchMode(MODE_DISPLAY);
        if (events.userInputRequired && mNativeDisplayView != null)
            mNativeDisplayView.postDelayed(mNativeDisplayView::showKeyboard, 220);
    }

    private synchronized void attachAgentRuntime() {
        if (mAgentRuntime == null)
            mAgentRuntime = OminalAgentRuntime.get(getApplicationContext(), getChatRootPath());
        mAgentRuntime.addObserver(this);
    }

    private void shutdownAgentRuntime() {
        OminalAgentRuntime runtime = mAgentRuntime;
        if (runtime != null) runtime.shutdown();
        stopAgentEventObserver();
        mPromptRunning = false;
        mActiveAgentTurnView = null;
        setInputEnabled(true);
    }

    @Override
    public void onAgentStateChanged(OminalAgentRuntime.Snapshot snapshot) {
        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())
                return;
            applyAgentSnapshot(snapshot);
        });
    }

    private void applyAgentSnapshot(OminalAgentRuntime.Snapshot snapshot) {
        if (snapshot == null) return;
        ChatSession session = findSession(snapshot.sessionId);
        if (session != null && !snapshot.threadId.isEmpty()
            && !snapshot.threadId.equals(session.threadId(snapshot.harnessId))) {
            session.setThreadId(snapshot.harnessId, snapshot.threadId);
            saveMeta(session);
            writeRuntimeContract(session, snapshot.harnessId);
        }

        boolean activeSessionSnapshot = mActiveSession != null
            && snapshot.sessionId.equals(mActiveSession.id);
        if (!mDisplayActivitySessionId.isEmpty()
            && mDisplayActivitySessionId.equals(snapshot.sessionId)
            && !snapshot.isRunning()) {
            mDisplayActivitySessionId = "";
            setAgentUsingDisplay(false);
        }
        if (activeSessionSnapshot) {
            mPromptRunning = snapshot.isRunning();
            bindAgentSnapshotToChat(snapshot);
            updateDisplayAgentStatus(snapshot);
            setInputEnabled(true);
        }

        if (snapshot.isRunning()) {
            if (activeSessionSnapshot)
                setStatus(snapshot.status.isEmpty() ? "Working" : snapshot.status);
            return;
        }
        if (!snapshot.isTerminal() || snapshot.revision == mHandledAgentRevision
            || session == null) {
            if (snapshot.isIdle() && mBootstrapReady && mRuntimeReady) setStatus("Ready");
            return;
        }

        mHandledAgentRevision = snapshot.revision;
        AgentTurnView responseView = session == mActiveSession ? mActiveAgentTurnView : null;
        File eventLog = agentEventLogFile(session);
        if (activeSessionSnapshot) stopAgentEventObserver();
        if (snapshot.isComplete())
            finishAgentTurn(session, eventLog, responseView, snapshot.message, snapshot.usage,
                snapshot.trace, snapshot.harnessId,
                session.activeTurn == null ? -1 : session.activeTurn.userMessageIndex,
                snapshot.revision);
        else
            failAgentTurn(session, responseView, snapshot.message, snapshot.trace,
                snapshot.harnessId,
                session.activeTurn == null ? -1 : session.activeTurn.userMessageIndex,
                snapshot.revision);
    }

    private void bindAgentSnapshotToChat(OminalAgentRuntime.Snapshot snapshot) {
        if (snapshot == null || snapshot.isIdle() || mActiveSession == null
            || !snapshot.sessionId.equals(mActiveSession.id) || mMessagesView == null) {
            return;
        }
        if (mActiveAgentTurnView == null
            || !mActiveAgentTurnView.detail.isAttachedToWindow()
                && mMessagesView.isAttachedToWindow()) {
            mActiveAgentTurnView = addAgentTurn(
                snapshot.status.isEmpty() ? "Working" : snapshot.status, false);
        }
        mActiveAgentTurnView.status = snapshot.status;
        mActiveAgentTurnView.usage = snapshot.usage;
        mActiveAgentTurnView.trace = snapshot.trace;
        mActiveAgentTurnView.running = snapshot.isRunning();
        mActiveAgentTurnView.setMessage(snapshot.message);
        renderAgentTurnStatus(mActiveAgentTurnView);
        scrollToBottom();
    }

    private void updateDisplayAgentStatus(OminalAgentRuntime.Snapshot snapshot) {
        if (mDisplayAgentStatusView == null || mDisplayAgentStatusText == null
            || mDisplayAgentPulse == null) return;
        boolean visible = snapshot != null && snapshot.isRunning();
        if (visible) syncDisplayActivityFromDisk();
        // Computer use is represented by the display-edge highlight, not a floating badge.
        mDisplayAgentStatusView.setVisibility(View.GONE);
        mDisplayAgentPulse.setRunning(visible);
        if (visible) {
            String status = snapshot.status.isEmpty() ? "Working" : snapshot.status;
            String total = tokenUsageTotal(snapshot.usage);
            mDisplayAgentStatusText.setText(total.isEmpty() ? status : status + "  /  " + total);
        }
        updateDisplayLifecycleVisuals();
    }

    private void setDisplayLifecycleState(String state) {
        if (TextUtils.isEmpty(state) || state.equals(mDisplayLifecycleState)) {
            updateDisplayLifecycleVisuals();
            return;
        }
        mDisplayLifecycleState = state;
        updateDisplayLifecycleVisuals();
        if (mRuntimeReady && mActiveSession != null) writeRuntimeContract(mActiveSession);
    }

    private void setAgentUsingDisplay(boolean active) {
        if (mAgentUsingDisplay == active) {
            updateDisplayLifecycleVisuals();
            return;
        }
        mAgentUsingDisplay = active;
        if (active) mDisplayNeedsUser = false;
        updateDisplayLifecycleVisuals();
        if (mRuntimeReady && mActiveSession != null) writeRuntimeContract(mActiveSession);
    }

    private void setDisplayNeedsUser(boolean needsUser) {
        if (mDisplayNeedsUser == needsUser) {
            updateDisplayLifecycleVisuals();
            return;
        }
        mDisplayNeedsUser = needsUser;
        if (needsUser) mAgentUsingDisplay = false;
        updateDisplayLifecycleVisuals();
        if (mRuntimeReady && mActiveSession != null) writeRuntimeContract(mActiveSession);
    }

    private String currentDisplayState() {
        if (isDisplayOperational()) {
            if (mDisplayNeedsUser) return DISPLAY_STATE_NEEDS_USER;
            if (mAgentUsingDisplay) return DISPLAY_STATE_AGENT_ACTIVE;
            return DISPLAY_STATE_READY_IDLE;
        }
        return mDisplayLifecycleState;
    }

    private boolean isDisplayOperational() {
        if (!mDisplayReady) return false;
        return !usesNativeDisplay() || LorieView.connected() && isNativeDesktopReady();
    }

    private void updateDisplayLifecycleVisuals() {
        String state = currentDisplayState();
        boolean active = DISPLAY_STATE_AGENT_ACTIVE.equals(state);
        if (mDisplayActivityBorder != null) {
            mDisplayActivityBorder.animate().cancel();
            if (active) {
                mDisplayActivityBorder.setAlpha(0f);
                mDisplayActivityBorder.setVisibility(View.VISIBLE);
                mDisplayActivityBorder.animate().alpha(1f).setDuration(160).start();
            } else if (mDisplayActivityBorder.getVisibility() == View.VISIBLE) {
                mDisplayActivityBorder.animate().alpha(0f).setDuration(140)
                    .withEndAction(() -> {
                        if (!DISPLAY_STATE_AGENT_ACTIVE.equals(currentDisplayState()))
                            mDisplayActivityBorder.setVisibility(View.GONE);
                    })
                    .start();
            }
        }
        if (mDisplayPane != null)
            mDisplayPane.setContentDescription("Screen " + state.replace('_', ' '));
    }

    private HashMap<String, String> codexServerEnvironment() {
        return codexServerEnvironment(null);
    }

    private HashMap<String, String> codexServerEnvironment(ChatSession session) {
        HashMap<String, String> environment = new HashMap<>();
        environment.put("ORINGUTAN_FRONTEND", "chat");
        environment.put("OMINAL_WORKDIR", getChatRootPath());
        environment.put("OMINAL_DISPLAY", ":20");
        environment.put("OMINAL_DISPLAY_GEOMETRY", getDisplayGeometry());
        environment.put("OMINAL_DISPLAY_DPI", Integer.toString(currentDisplayGeometry().densityDpi));
        environment.put("OMINAL_LOLO_MODE", isLoloModeEnabled() ? "1" : "0");
        if (session != null) {
            environment.put("OMINAL_AGENT_SESSION", session.id);
            environment.put("OMINAL_EVENT_LOG",
                guestWorkspacePath(session) + "/.ominal/events.jsonl");
        }
        return environment;
    }

    private String guestWorkspacePath(ChatSession session) {
        return "/root/workspace/" + session.id + "/workspace";
    }

    private String buildAgentDeveloperInstructions() {
        return "You are the selected intelligence harness inside Monolith, an Android computer. "
            + "Use the current working directory for this chat's files and outputs. "
            + "Read ./.ominal/runtime.json before acting; it is the authoritative Ominal runtime contract. "
            + "The user's primary interface is chat and the Linux desktop stays hidden until needed. "
            + "A graphical desktop is available on DISPLAY=:20. Before GUI work run `ominal-screen wait 20`; "
            + "`ominal-screen status` is the authoritative readiness probe. Then take a screenshot before "
            + "acting and use tap, double-tap, type, key, windows, focus, or close as needed. "
            + "Keep useful state visible because the user shares the same touch desktop. Use the desktop "
            + "autonomously for GUI work. If user input, visual confirmation, login, or manual control is truly "
            + "required, run `ominal-event request-user-input \"short reason\"`. Run `ominal-event open-display "
            + "\"short reason\"` when the user should see the display but does not need to type. "
            + "The user theme is `/root/.ominal/ui.properties`. After editing it, run "
            + "`ominal-event reload-ui` to apply it without restarting the harness. "
            + "Use `ominal-install` for Linux packages and downloaded .deb files; never use raw `dpkg -i`. "
            + "Put images, audio, video, or PDFs created for the user under ./"
            + MEDIA_DIR_NAME + "; Monolith surfaces new and changed media inline in the chat. "
            + "Do not print protocol control markers in the chat response.";
    }

    private void finishAgentTurn(ChatSession session, File eventLog, AgentTurnView responseView,
                                 String output, OminalAgentTransport.TokenUsage usage,
                                 OminalAgentTrace.Snapshot trace, String harnessId,
                                 int userMessageIndex, long runtimeRevision) {
        OminalAgentEventLog.Summary events = readAgentEvents(eventLog);
        boolean openDisplay = events.openDisplay || shouldAutoOpenDisplay(output);
        String visibleOutput = stripDisplayMarkers(output);
        String detail = tokenUsageLabel(usage);
        HashMap<String, String> mediaBefore = session.activeTurn == null
            ? OminalChatMedia.snapshot(new File(session.workspacePath))
            : session.activeTurn.mediaBefore;
        ArrayList<OminalChatMedia.Item> media = OminalChatMedia.changedSince(
            new File(session.workspacePath), mediaBefore);
        runOnUiThread(() -> {
            if (responseView != null) {
                responseView.setMessage(visibleOutput);
                responseView.setMedia(session, media);
                responseView.status = detail.isEmpty() ? "Complete" : "";
                responseView.usage = usage;
                responseView.trace = trace;
                responseView.running = false;
                renderAgentTurnStatus(responseView);
            }
            ChatMessage assistantMessage = new ChatMessage("assistant", visibleOutput, nowLabel(),
                detail, trace, media);
            session.messages.add(assistantMessage);
            appendHistory(session, assistantMessage);
            session.markContextCurrent(harnessId, userMessageIndex + 1);
            session.activeTurn = null;
            saveMeta(session);
            OminalAgentRuntime runtime = mAgentRuntime;
            if (runtime != null && runtimeRevision >= 0)
                runtime.acknowledge(runtimeRevision);
            boolean activeSession = session == mActiveSession;
            if (activeSession) updateHarnessControls();
            if (activeSession) mPromptRunning = false;
            boolean codex = OminalHarnessTerminal.CODEX_ID.equals(harnessId);
            if (codex) {
                mCodexSignedIn = true;
                setCodexSessionExpired(false);
            }
            styleAccountButton();
            completeRunnerPairing(codex);
            if (activeSession) setInputEnabled(true);
            boolean openedAndroid = handleLoloRequests(events.androidRequests);
            if (activeSession && openDisplay) {
                setDisplayNeedsUser(events.userInputRequired);
                setStatus(events.userInputRequired ? "Your input is needed" : "Screen open");
                switchMode(MODE_DISPLAY);
                if (events.userInputRequired && mNativeDisplayView != null)
                    mNativeDisplayView.showKeyboard();
            } else if (activeSession && !events.status.isEmpty()) {
                setStatus(events.status);
            } else if (activeSession && openedAndroid) {
                setStatus("Opened in Android");
            } else if (activeSession) {
                setStatus("Ready");
            }
            writeRuntimeContract(session, harnessId);
            if (activeSession) scrollToBottom();
            if (events.reloadUi) {
                mUi = loadUiSpec();
                recreate();
                return;
            }
            startNextPendingTurn(session);
        });
    }

    private void failAgentTurn(ChatSession session, AgentTurnView responseView, String error,
                               OminalAgentTrace.Snapshot trace, String harnessId,
                               int userMessageIndex, long runtimeRevision) {
        runOnUiThread(() -> {
            boolean authenticationRequired =
                OminalHarnessTerminal.CODEX_ID.equals(harnessId)
                    && requiresCodexLogin(error);
            String visibleError = authenticationRequired
                ? OminalCodexAppServer.AUTHENTICATION_REQUIRED_MESSAGE : error;
            if (responseView != null) {
                responseView.setMessage(visibleError);
                responseView.status = "";
                responseView.trace = trace;
                responseView.running = false;
                renderAgentTurnStatus(responseView);
            }
            ChatMessage assistantMessage = new ChatMessage("assistant", visibleError, nowLabel(),
                "", trace);
            session.messages.add(assistantMessage);
            appendHistory(session, assistantMessage);
            session.markContextCurrent(harnessId, userMessageIndex + 1);
            session.activeTurn = null;
            saveMeta(session);
            OminalAgentRuntime runtime = mAgentRuntime;
            if (runtime != null && runtimeRevision >= 0)
                runtime.acknowledge(runtimeRevision);
            boolean activeSession = session == mActiveSession;
            if (activeSession) updateHarnessControls();
            if (activeSession) {
                mPromptRunning = false;
                setInputEnabled(true);
            }
            if (authenticationRequired) {
                mCodexSignedIn = false;
                setCodexSessionExpired(true);
                styleAccountButton();
                if (runtime != null) runtime.releaseSessionTransport(session.id);
                stopAgentEventObserver();
                if (activeSession) setStatus("Sign in");
                if (activeSession && mRootFrame != null)
                    mRootFrame.postDelayed(this::showCodexAccountDialog, 180);
            } else if (activeSession) {
                setStatus("Ready");
            }
            if (activeSession) scrollToBottom();
            if (!authenticationRequired) startNextPendingTurn(session);
        });
    }

    private String buildAgentPrompt(ChatSession session, PendingTurn turn) {
        File attachmentDir = new File(session.workspacePath, ATTACHMENTS_DIR_NAME);
        File[] files = attachmentDir.listFiles(File::isFile);

        StringBuilder builder = new StringBuilder();
        String handoff = conversationHandoff(session, turn.harnessId,
            turn.userMessageIndex);
        if (!handoff.isEmpty()) {
            builder.append("Conversation context supplied by Monolith:\n")
                .append(handoff)
                .append("\n\nContinue this same conversation. Do not describe the handoff.\n\n");
        }
        builder.append("Current Ominal capabilities: ");
        if (isLoloModeEnabled()) {
            builder.append("Experimental Lolo mode is enabled. Use `ominal-device` only when the request ")
                .append("requires an Android app, link, or Settings screen; Android still enforces the app UID. ");
        } else {
            builder.append("Experimental Lolo mode is disabled. Do not attempt Android-level actions. ");
        }
        builder.append("\n\nUser request:\n")
            .append(turn.prompt);
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

    private static boolean isHarnessCommand(String prompt) {
        return prompt != null && prompt.startsWith("/") && !prompt.startsWith("//");
    }

    private String conversationHandoff(ChatSession session, String harnessId,
                                       int currentPromptIndex) {
        if (session == null || session.messages.isEmpty()) return "";
        int boundedPromptIndex = Math.max(0,
            Math.min(currentPromptIndex, session.messages.size() - 1));
        int start = Math.min(session.contextCursor(harnessId), boundedPromptIndex);
        StringBuilder transcript = new StringBuilder();
        for (int index = start; index < session.messages.size(); index++) {
            ChatMessage message = session.messages.get(index);
            if (!"user".equals(message.role) && !"assistant".equals(message.role)) continue;
            if ("user".equals(message.role) && index >= boundedPromptIndex) continue;
            transcript.append("user".equals(message.role) ? "User: " : "Assistant: ")
                .append(message.text.trim())
                .append("\n\n");
        }
        final int maximumCharacters = 24000;
        if (transcript.length() > maximumCharacters) {
            int cut = transcript.length() - maximumCharacters;
            int boundary = transcript.indexOf("\n\n", cut);
            transcript.delete(0, boundary >= 0 ? boundary + 2 : cut);
            transcript.insert(0, "[Earlier conversation omitted]\n\n");
        }
        return transcript.toString().trim();
    }

    private File agentRuntimeDirectory(ChatSession session) {
        return new File(session.workspacePath, AGENT_RUNTIME_DIR_NAME);
    }

    private File agentEventLogFile(ChatSession session) {
        return new File(agentRuntimeDirectory(session), AGENT_EVENT_LOG_NAME);
    }

    private void writeRuntimeContract(ChatSession session) {
        writeRuntimeContract(session, session == null ? "" : session.harnessId);
    }

    private void writeRuntimeContract(ChatSession session, String harnessId) {
        if (session == null) return;
        File runtimeDirectory = agentRuntimeDirectory(session);
        ensureDirectory(runtimeDirectory.getAbsolutePath());
        try {
            String guestWorkspace = guestWorkspacePath(session);
            String selectedHarness = OminalHarnessRegistry.normalizeSelectedId(harnessId);
            OminalAgentHarness harness = OminalHarnessRegistry.activeOrDefault(selectedHarness);
            boolean displayOperational = isDisplayOperational();
            String contract = OminalRuntimeContract.create(session.id, session.title,
                guestWorkspace, attachmentPaths(session), currentDisplayGeometry(),
                usesNativeDisplay() ? "native-x11-surface" : "novnc-webview",
                mMode == MODE_DISPLAY, displayOperational, currentDisplayState(),
                harness, mCodexSignedIn,
                guestWorkspace + "/.ominal/events.jsonl", session.threadId(selectedHarness),
                isLoloModeEnabled());
            writeFile(new File(runtimeDirectory, AGENT_RUNTIME_CONTRACT_NAME), contract);
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to write the Ominal runtime contract", e);
        }
    }

    private List<String> attachmentPaths(ChatSession session) {
        ArrayList<String> paths = new ArrayList<>();
        File[] files = new File(session.workspacePath, ATTACHMENTS_DIR_NAME).listFiles(File::isFile);
        if (files == null) return paths;
        for (File file : files) paths.add(ATTACHMENTS_DIR_NAME + "/" + file.getName());
        return paths;
    }

    private OminalAgentEventLog.Summary readAgentEvents(File eventLog) {
        try {
            return OminalAgentEventLog.summarize(OminalAgentEventLog.read(eventLog));
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read agent events", e);
            return OminalAgentEventLog.summarize(new ArrayList<>());
        }
    }

    private boolean handleLoloRequests(List<OminalAgentEventLog.Event> requests) {
        if (!isLoloModeEnabled() || requests == null || requests.isEmpty()) return false;
        boolean opened = false;
        int handled = 0;
        for (OminalAgentEventLog.Event request : requests) {
            if (handled >= 3) break;
            Intent intent = null;
            if (OminalAgentEventLog.TYPE_ANDROID_SETTINGS.equals(request.type)) {
                intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            } else if (OminalAgentEventLog.TYPE_ANDROID_OPEN.equals(request.type)) {
                Uri uri = Uri.parse(request.message);
                String scheme = uri.getScheme();
                if (scheme != null && ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme)
                    || "mailto".equalsIgnoreCase(scheme)
                    || "geo".equalsIgnoreCase(scheme)
                    || "market".equalsIgnoreCase(scheme)))
                    intent = new Intent(Intent.ACTION_VIEW, uri);
            } else if (OminalAgentEventLog.TYPE_ANDROID_APP.equals(request.type)
                && request.message.matches("[A-Za-z0-9._]+")) {
                intent = getPackageManager().getLaunchIntentForPackage(request.message);
            }
            if (intent != null) {
                opened |= startExternalActivity(intent);
                handled++;
            }
        }
        return opened;
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
            result.append("Something went wrong before the reply finished.");
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

    private String formatAgentOutput(ExecutionCommand command) {
        String stdout = command.resultData.stdout.toString().trim();
        String stderr = command.resultData.stderr.toString().trim();
        String diagnostic = stdout + "\n" + stderr;
        if (requiresCodexLogin(diagnostic))
            return OminalCodexAppServer.AUTHENTICATION_REQUIRED_MESSAGE;
        if (!stdout.isEmpty()) return stdout;

        String normalized = diagnostic.toLowerCase(Locale.ROOT);
        if (normalized.contains("timed out") || normalized.contains("timeout"))
            return "The request took too long. Try again.";
        if (command.isStateFailed()
            || (command.resultData.exitCode != null && command.resultData.exitCode != 0))
            return "Codex couldn't finish that request. Try again.";
        return "No response was returned.";
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
        if (cleaned.isEmpty()) return "Tap the screen to continue.";
        return cleaned;
    }

    private void openTerminalForActiveChat() {
        if (!mBootstrapReady || !mRuntimeReady || !OminalProotTerminal.isReady()) {
            addTransientSystemMessage("The Linux environment is still getting ready.");
            return;
        }
        if (mActiveSession == null) return;

        ensureDirectory(mActiveSession.workspacePath);
        Intent executeIntent = new Intent(OMINAL_SERVICE.ACTION_SERVICE_EXECUTE);
        executeIntent.setClass(this, OminalService.class);
        OminalProotTerminal.configureIntent(executeIntent, mActiveSession.workspacePath);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_RUNNER, ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_NAME, mActiveSession.terminalName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE, ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_COMMAND_LABEL, mActiveSession.title);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION,
            Integer.toString(OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY));
        startService(executeIntent);
        mContentFrame.postDelayed(() -> startActivity(new Intent(this, OminalActivity.class)), 300);
    }

    private void openAgentTerminalForActiveChat() {
        if (mActiveSession == null) return;
        launchHarnessTerminal(mActiveSession.harnessId, false);
    }

    private void launchHarnessTerminal(String harnessId, boolean completePairingAtLaunch) {
        if (!mBootstrapReady || !mRuntimeReady || mActiveSession == null) {
            setPairingBusy(false, "");
            addTransientSystemMessage("The Linux environment is still getting ready.");
            return;
        }
        if (!OminalHarnessTerminal.isSupported(harnessId)) {
            setPairingBusy(false, "");
            addTransientSystemMessage("That harness is not available in this build.");
            return;
        }

        ensureProviderCommands();
        if (!OminalHarnessTerminal.isReady()) {
            setPairingBusy(false, "");
            addTransientSystemMessage("The harness terminal is still getting ready.");
            return;
        }

        OminalAgentHarness harness = OminalHarnessRegistry.find(harnessId);
        String displayName = harness == null ? harnessId : harness.getDisplayName();
        ensureDirectory(mActiveSession.workspacePath);

        Intent executeIntent = new Intent(OMINAL_SERVICE.ACTION_SERVICE_EXECUTE);
        executeIntent.setClass(this, OminalService.class);
        OminalHarnessTerminal.configureIntent(
            executeIntent, harnessId, mActiveSession.workspacePath,
            mActiveSession.modelId(), mActiveSession.effortId());
        executeIntent.putExtra(
            OMINAL_SERVICE.EXTRA_RUNNER, ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_NAME,
            OminalHarnessTerminal.sessionName(mActiveSession.id, harnessId));
        executeIntent.putExtra(
            OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE, ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_COMMAND_LABEL, displayName);
        executeIntent.putExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION,
            Integer.toString(
                OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY));

        if (mPrefs != null)
            mPrefs.edit().putString(PREF_LAST_TERMINAL_HARNESS, harnessId).apply();
        if (completePairingAtLaunch) completeRunnerPairing(false);

        startService(executeIntent);
        View launchAnchor = mContentFrame == null ? mRootFrame : mContentFrame;
        if (launchAnchor != null)
            launchAnchor.postDelayed(
                () -> startActivity(new Intent(this, OminalActivity.class)), 300);
        else
            startActivity(new Intent(this, OminalActivity.class));
    }

    private void appendMessage(ChatSession session, ChatMessage message, boolean persist) {
        session.messages.add(message);
        if (persist) appendHistory(session, message);
        if (session == mActiveSession && mMode == MODE_CHAT && !shouldHideSystemReadyMessage(message)) {
            renderChatMessage(session, message, true);
        }
    }

    private void renderChatMessage(ChatSession session, ChatMessage message, boolean scrollNow) {
        if ("assistant".equals(message.role)
            && (!message.detail.isEmpty() || !message.trace.isEmpty())) {
            AgentTurnView view = addAgentTurn("", false);
            view.setMessage(visibleMessageText(message));
            view.setMedia(session, message.media);
            view.detail.setText(message.detail);
            view.detail.setVisibility(View.VISIBLE);
            view.trace = message.trace;
            renderAgentTurnStatus(view);
            if (scrollNow) scrollToBottom();
            return;
        }
        if (message.media.isEmpty()) {
            addBubble(visibleMessageText(message), "user".equals(message.role), scrollNow);
        } else {
            addMediaMessage(session, visibleMessageText(message),
                "user".equals(message.role), message.media, scrollNow);
        }
    }

    private String visibleMessageText(ChatMessage message) {
        if (message == null || message.text == null) return "";
        if (!"assistant".equals(message.role)) return message.text;
        return requiresCodexLogin(message.text)
            ? OminalCodexAppServer.AUTHENTICATION_REQUIRED_MESSAGE : message.text;
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
        TextView bubble = createBubbleView(message, fromUser);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.setMargins(0, dp(5), 0, dp(12));
        if (mMessagesView != null) mMessagesView.addView(bubble, params);
        if (scrollNow) animateMessageIn(bubble, fromUser);
        if (scrollNow) scrollToBottom();
        return bubble;
    }

    private TextView createBubbleView(String message, boolean fromUser) {
        UiSpec ui = ui();
        SurfaceSpec surface = fromUser ? ui.bubbleUser : ui.bubbleAgent;
        TextView bubble = new TextView(this);
        if (fromUser) bubble.setText(message);
        else renderMarkdown(bubble, message);
        bubble.setTextSize(16f);
        bubble.setLineSpacing(dp(3), 1.12f);
        bubble.setTextColor(surface.text);
        if (fromUser) bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        else bubble.setPadding(0, dp(8), 0, dp(8));
        bubble.setBackground(makeSurfaceDrawable(surface, false));
        bubble.setTextIsSelectable(true);
        bubble.setMovementMethod(fromUser
            ? ArrowKeyMovementMethod.getInstance() : LinkMovementMethod.getInstance());
        bubble.setFocusable(true);
        bubble.setFocusableInTouchMode(true);
        bubble.setOnLongClickListener(v -> {
            copyToClipboard("Ominal message", bubble.getText().toString());
            Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
            return true;
        });
        int width = getResources().getDisplayMetrics().widthPixels;
        bubble.setMaxWidth(fromUser
            ? Math.min(Math.round(width * 0.88f), dp(620))
            : Math.min(width - dp(36), dp(720)));
        return bubble;
    }

    private void addMediaMessage(ChatSession session, String message, boolean fromUser,
                                 List<OminalChatMedia.Item> media, boolean scrollNow) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(fromUser ? Gravity.END : Gravity.START);

        if (!TextUtils.isEmpty(message)) {
            TextView bubble = createBubbleView(message, fromUser);
            LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bubbleParams.gravity = fromUser ? Gravity.END : Gravity.START;
            bubbleParams.setMargins(0, 0, 0, dp(6));
            container.addView(bubble, bubbleParams);
        }

        LinearLayout mediaView = new LinearLayout(this);
        mediaView.setOrientation(LinearLayout.VERTICAL);
        mediaView.setGravity(fromUser ? Gravity.END : Gravity.START);
        renderMediaItems(mediaView, session, media);
        container.addView(mediaView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.setMargins(dp(2), dp(4), dp(2), dp(8));
        if (mMessagesView != null) mMessagesView.addView(container, params);
        if (scrollNow) animateMessageIn(container, fromUser);
        if (scrollNow) scrollToBottom();
    }

    private void renderMediaItems(LinearLayout container, ChatSession session,
                                  List<OminalChatMedia.Item> media) {
        container.removeAllViews();
        if (session == null || media == null || media.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        File workspace = new File(session.workspacePath);
        for (OminalChatMedia.Item item : media) {
            File file = OminalChatMedia.resolve(workspace, item.path);
            if (file == null || !file.isFile()) continue;
            View mediaItem = item.isImage() ? createImageMediaView(file, item)
                : createFileMediaView(file, item);
            ViewGroup.LayoutParams currentParams = mediaItem.getLayoutParams();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                currentParams == null ? LinearLayout.LayoutParams.WRAP_CONTENT : currentParams.width,
                currentParams == null ? LinearLayout.LayoutParams.WRAP_CONTENT : currentParams.height);
            params.setMargins(0, 0, 0, dp(6));
            container.addView(mediaItem, params);
        }
        container.setVisibility(container.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private View createImageMediaView(File file, OminalChatMedia.Item item) {
        int maxWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(440));
        int maxHeight = dp(440);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
            return createFileMediaView(file, item);

        int width = maxWidth;
        int height = Math.max(dp(72), Math.round(width * bounds.outHeight / (float) bounds.outWidth));
        if (height > maxHeight) {
            height = maxHeight;
            width = Math.max(dp(72), Math.round(height * bounds.outWidth / (float) bounds.outHeight));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = imageSampleSize(bounds.outWidth, bounds.outHeight, width, height);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) return createFileMediaView(file, item);

        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackground(makeRoundedDrawable(ui().panel, ui().border, dp(8)));
        image.setClipToOutline(true);
        image.setContentDescription(item.name);
        image.setOnClickListener(ignored -> showMediaPreview(file, item.name));
        image.setFocusable(true);
        image.setClickable(true);
        image.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return image;
    }

    private int imageSampleSize(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int sample = 1;
        while (sourceWidth / (sample * 2) >= targetWidth
            && sourceHeight / (sample * 2) >= targetHeight) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }

    private View createFileMediaView(File file, OminalChatMedia.Item item) {
        UiSpec ui = ui();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(14), dp(10));
        row.setBackground(makeRoundedDrawable(ui.panel, ui.border, dp(8)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_save);
        icon.setImageTintList(ColorStateList.valueOf(ui.muted));
        row.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextColor(ui.ink);
        name.setTextSize(14);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        name.setPadding(dp(10), 0, 0, 0);
        row.addView(name, new LinearLayout.LayoutParams(
            Math.min(getResources().getDisplayMetrics().widthPixels - dp(120), dp(360)),
            LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setContentDescription("File " + item.name);
        row.setOnClickListener(ignored ->
            Toast.makeText(this, file.getName(), Toast.LENGTH_SHORT).show());
        return row;
    }

    private void showMediaPreview(File file, String name) {
        int targetWidth = Math.max(dp(320), getResources().getDisplayMetrics().widthPixels);
        int targetHeight = Math.max(dp(480), getResources().getDisplayMetrics().heightPixels);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = imageSampleSize(bounds.outWidth, bounds.outHeight,
            targetWidth, targetHeight);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) return;

        FrameLayout preview = new FrameLayout(this);
        preview.setBackgroundColor(Color.BLACK);
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(name);
        preview.addView(image, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_close);
        close.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        close.setBackground(makeRoundedDrawable(Color.argb(176, 0, 0, 0),
            Color.argb(48, 255, 255, 255), dp(22)));
        close.setContentDescription("Close image");
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dp(14), dp(14), 0);
        preview.addView(close, closeParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(preview)
            .create();
        close.setOnClickListener(ignored -> dialog.dismiss());
        preview.setOnClickListener(ignored -> dialog.dismiss());
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.BLACK));
            }
        });
        dialog.show();
    }

    private AgentTurnView addAgentTurn(String status, boolean scrollNow) {
        UiSpec ui = ui();
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.START);

        TextView message = new TextView(this);
        message.setTextSize(16);
        message.setLineSpacing(dp(2), 1.1f);
        message.setTextColor(ui.bubbleAgent.text);
        message.setPadding(0, dp(7), 0, dp(7));
        message.setBackground(makeSurfaceDrawable(ui.bubbleAgent, false));
        message.setTextIsSelectable(true);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setFocusable(true);
        message.setFocusableInTouchMode(true);
        message.setVisibility(View.GONE);
        message.setOnLongClickListener(v -> {
            copyToClipboard("Ominal message", message.getText().toString());
            Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
            return true;
        });
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.gravity = Gravity.START;
        message.setMaxWidth(Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(720)));
        container.addView(message, messageParams);

        LinearLayout media = new LinearLayout(this);
        media.setOrientation(LinearLayout.VERTICAL);
        media.setGravity(Gravity.START);
        media.setVisibility(View.GONE);
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mediaParams.setMargins(0, dp(6), 0, 0);
        container.addView(media, mediaParams);

        LinearLayout workSurface = new LinearLayout(this);
        workSurface.setOrientation(LinearLayout.HORIZONTAL);
        workSurface.setPadding(0, dp(4), 0, dp(4));
        workSurface.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout workBody = new LinearLayout(this);
        workBody.setOrientation(LinearLayout.VERTICAL);

        LinearLayout trace = new LinearLayout(this);
        trace.setOrientation(LinearLayout.HORIZONTAL);
        trace.setGravity(Gravity.CENTER_VERTICAL);
        trace.setPadding(0, dp(4), 0, 0);
        trace.setVisibility(View.GONE);
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(0, dp(3), 0, 0);

        WorkPulseView pulse = new WorkPulseView(this, ui.muted, ui.ink);
        statusRow.addView(pulse, new LinearLayout.LayoutParams(dp(14), dp(18)));

        TextView detail = new TextView(this);
        detail.setText(status);
        detail.setTextSize(12.5f);
        detail.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        detail.setTextColor(ui.muted);
        detail.setPadding(dp(5), 0, dp(5), 0);
        detail.setSingleLine(false);
        detail.setVisibility(status == null || status.isEmpty() ? View.GONE : View.VISIBLE);
        statusRow.addView(detail, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        workBody.addView(statusRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        workBody.addView(trace, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TokenMeterView meter = new TokenMeterView(this, ui.border, ui.muted, ui.ink);
        LinearLayout.LayoutParams meterParams = new LinearLayout.LayoutParams(dp(236), dp(5));
        meterParams.setMargins(0, dp(7), 0, 0);
        meter.setVisibility(View.GONE);
        workBody.addView(meter, meterParams);

        TextView breakdown = new TextView(this);
        breakdown.setTextSize(11);
        breakdown.setTextColor(ui.muted);
        breakdown.setPadding(0, dp(7), 0, 0);
        breakdown.setVisibility(View.GONE);
        workBody.addView(breakdown, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        workSurface.addView(workBody, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams workParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        workParams.setMargins(0, dp(8), 0, 0);
        container.addView(workSurface, workParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.START;
        params.setMargins(0, dp(5), 0, dp(12));
        if (mMessagesView != null) mMessagesView.addView(container, params);
        if (scrollNow) animateMessageIn(container, false);
        if (scrollNow) scrollToBottom();
        AgentTurnView view = new AgentTurnView(message, media, workSurface, trace, detail,
            breakdown, pulse, meter, status);
        workSurface.setClickable(true);
        workSurface.setFocusable(true);
        workSurface.setOnClickListener(ignored -> {
            view.expanded = !view.expanded;
            renderAgentTurnStatus(view);
        });
        workSurface.setContentDescription("Work and token usage");
        renderAgentTurnStatus(view);
        return view;
    }

    private void renderAgentTurnStatus(AgentTurnView view) {
        if (view == null) return;
        boolean hasTrace = view.trace != null && !view.trace.isEmpty();
        String total = tokenUsageTotal(view.usage);
        if (view.status.isEmpty())
            view.detail.setText(total.isEmpty() && hasTrace ? workTraceSummary(view.trace) : total);
        else if (total.isEmpty()) view.detail.setText(view.status);
        else view.detail.setText(view.status + "  /  " + total);
        view.detail.setVisibility(view.detail.getText().length() == 0 ? View.GONE : View.VISIBLE);
        view.pulse.setRunning(view.running);
        view.pulse.setVisibility(view.detail.getVisibility());
        view.meter.setUsage(view.usage);
        view.meter.setVisibility(view.expanded && view.usage != null ? View.VISIBLE : View.GONE);
        view.breakdown.setText(tokenUsageLabel(view.usage));
        view.breakdown.setVisibility(view.expanded && view.usage != null ? View.VISIBLE : View.GONE);
        renderAgentTrace(view);
        view.workSurface.setVisibility(view.detail.getVisibility() == View.VISIBLE
            || view.usage != null || hasTrace ? View.VISIBLE : View.GONE);
    }

    private String workTraceSummary(OminalAgentTrace.Snapshot trace) {
        if (trace == null || trace.entries.isEmpty()) return "";
        OminalAgentTrace.Entry latest = trace.entries.get(trace.entries.size() - 1);
        if (latest.running) return latest.label;
        int count = trace.entries.size();
        return count + (count == 1 ? " step" : " steps");
    }

    private void renderMarkdown(TextView target, String markdown) {
        String text = OminalChatText.forDisplay(markdown);
        if (mMarkwon == null) target.setText(text);
        else mMarkwon.setMarkdown(target, text);
    }

    private void renderAgentTrace(AgentTurnView view) {
        LinearLayout container = view.traceView;
        container.removeAllViews();
        OminalAgentTrace.Snapshot trace = view.trace;
        if (trace == null || trace.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        UiSpec ui = ui();
        if (!view.expanded) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.START);
        for (int index = 0; index < trace.entries.size(); index++) {
            OminalAgentTrace.Entry entry = trace.entries.get(index);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            WorkStateThumbnailView thumbnail = new WorkStateThumbnailView(this, entry.type,
                entry.running, ui.muted, ui.ink);
            LinearLayout.LayoutParams thumbnailParams = new LinearLayout.LayoutParams(dp(20), dp(18));
            thumbnailParams.setMargins(0, dp(1), dp(8), 0);
            row.addView(thumbnail, thumbnailParams);

            TextView label = new TextView(this);
            label.setText(entry.label + (entry.count > 1 ? "  x" + entry.count : ""));
            label.setTextSize(12.5f);
            label.setTextColor(entry.running ? ui.ink : ui.muted);
            label.setIncludeFontPadding(false);
            row.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(27));
            container.addView(row, rowParams);
        }
        container.setVisibility(View.VISIBLE);
    }

    private String tokenUsageTotal(OminalAgentTransport.TokenUsage usage) {
        if (usage == null) return "";
        long total = usage.totalTokens > 0 ? usage.totalTokens : usage.inputTokens + usage.outputTokens;
        return compactNumber(total) + " tokens";
    }

    private String tokenUsageLabel(OminalAgentTransport.TokenUsage usage) {
        if (usage == null) return "";
        return "Input " + compactNumber(usage.inputTokens)
            + "   Cached " + compactNumber(usage.cachedInputTokens)
            + "\nOutput " + compactNumber(usage.outputTokens)
            + "   Reasoning " + compactNumber(usage.reasoningOutputTokens);
    }

    private String compactNumber(long value) {
        if (value < 1000) return Long.toString(value);
        if (value < 1000000) {
            float thousands = value / 1000f;
            return String.format(Locale.US, thousands >= 100 ? "%.0fk" : "%.1fk", thousands);
        }
        float millions = value / 1000000f;
        return String.format(Locale.US, millions >= 100 ? "%.0fM" : "%.1fM", millions);
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
            if (!message.detail.isEmpty()) object.put("detail", message.detail);
            if (!message.trace.isEmpty()) object.put("trace", message.trace.toJson());
            if (!message.media.isEmpty())
                object.put("media", OminalChatMedia.toJson(message.media));
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
            object.put("harnessId", session.harnessId);
            if (!session.threadIds.isEmpty())
                object.put("threadIds", new JSONObject(session.threadIds));
            if (!session.modelIds.isEmpty())
                object.put("modelIds", new JSONObject(session.modelIds));
            if (!session.effortIds.isEmpty())
                object.put("effortIds", new JSONObject(session.effortIds));
            if (session.activeTurn != null)
                object.put("activeTurn", session.activeTurn.toJson());
            if (!session.pendingTurns.isEmpty()) {
                JSONArray pendingTurns = new JSONArray();
                for (PendingTurn pendingTurn : session.pendingTurns)
                    pendingTurns.put(pendingTurn.toJson());
                object.put("pendingTurns", pendingTurns);
            }
            if (!session.contextCursors.isEmpty())
                object.put("contextCursors", new JSONObject(session.contextCursors));
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
        boolean available = enabled && mBootstrapReady && mRuntimeReady;
        if (mPromptInput != null) mPromptInput.setEnabled(available);
        if (mAttachButton != null) mAttachButton.setEnabled(available);
        if (mSendButton != null) mSendButton.setEnabled(available);
        boolean toolsAvailable = mBootstrapReady && mActiveSession != null;
        if (mTerminalToolButton != null) mTerminalToolButton.setEnabled(toolsAvailable);
        if (mDisplayToolButton != null) mDisplayToolButton.setEnabled(toolsAvailable);
        if (mHeaderDisplayButton != null) mHeaderDisplayButton.setEnabled(toolsAvailable);
        if (mLoloButton != null) mLoloButton.setEnabled(!mPromptRunning);
        updateComposerTools();
    }

    private void updateComposerTools() {
        if (mTerminalToolButton != null) {
            mTerminalToolButton.setText(mMode == MODE_TERMINAL ? "Chat" : "Terminal");
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

    private void animateMessageIn(View view, boolean fromUser) {
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(dp(8));
        view.setScaleX(fromUser ? 0.98f : 0.995f);
        view.setScaleY(fromUser ? 0.98f : 0.995f);
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(fromUser ? 210 : 240)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f))
            .start();
    }

    private void updateAppChromeForMode() {
        boolean visible = mMode != MODE_DISPLAY;
        if (mDrawerLayout != null)
            mDrawerLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        animateChromeVisibility(mHeaderView, visible, -dp(8));
        animateChromeVisibility(mComposerView, visible, dp(14));
        setDisplayFullscreen(!visible);
    }

    private void setDisplayFullscreen(boolean fullscreen) {
        View decor = getWindow().getDecorView();
        if (fullscreen) {
            getWindow().setSoftInputMode(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                : WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
            decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else {
            applyDisplayImeBottomInset(0);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            applySystemBars();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            decor.requestApplyInsets();
        decor.post(this::applyDisplayViewportBottomInset);
    }

    private void configureDisplayImeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (view, insets) -> {
            Insets systemBars = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars());
            Insets navigation = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.navigationBars());
            Insets mandatoryGestures = insets.getInsets(
                WindowInsetsCompat.Type.mandatorySystemGestures());
            Insets cutout = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.isVisible(WindowInsetsCompat.Type.ime())
                ? insets.getInsets(WindowInsetsCompat.Type.ime())
                : Insets.NONE;
            mChatInsetLeft = Math.max(systemBars.left,
                Math.max(mandatoryGestures.left, cutout.left));
            mChatInsetTop = Math.max(systemBars.top,
                Math.max(mandatoryGestures.top, cutout.top));
            mChatInsetRight = Math.max(systemBars.right,
                Math.max(mandatoryGestures.right, cutout.right));
            mChatInsetBottom = Math.max(ime.bottom, Math.max(systemBars.bottom,
                Math.max(mandatoryGestures.bottom, cutout.bottom)));
            if (mDrawerLayout != null) mDrawerLayout.post(this::applyChatViewportInsets);

            int reportedSystemBottom = Math.max(navigation.bottom, mandatoryGestures.bottom);
            int stableSystemBottom = reportedSystemBottom > 0
                ? reportedSystemBottom
                : Math.max(mDisplaySystemInsetBottom, navigationBarHeightFallback());
            boolean navigationChanged = mDisplayNavigationInsetLeft != navigation.left
                || mDisplayNavigationInsetTop != navigation.top
                || mDisplayNavigationInsetRight != navigation.right
                || mDisplayNavigationInsetBottom != navigation.bottom
                || mDisplaySystemInsetBottom != stableSystemBottom;
            mDisplayNavigationInsetLeft = navigation.left;
            mDisplayNavigationInsetTop = navigation.top;
            mDisplayNavigationInsetRight = navigation.right;
            mDisplayNavigationInsetBottom = navigation.bottom;
            mDisplaySystemInsetBottom = stableSystemBottom;
            mDisplayInsetsReady = true;

            int imeBottom = 0;
            if (mMode == MODE_DISPLAY && insets.isVisible(WindowInsetsCompat.Type.ime()))
                imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            applyDisplayImeBottomInset(
                OminalDisplayGeometry.keyboardOcclusion(imeBottom, mDisplaySystemInsetBottom));
            if (navigationChanged && mMode == MODE_DISPLAY && mNativeDisplayView != null)
                mNativeDisplayView.post(mNativeDisplayView::refreshDisplaySize);
            return insets;
        });
    }

    private void applyChatViewportInsets() {
        if (mDrawerLayout == null) return;
        ViewGroup.LayoutParams rawParams = mDrawerLayout.getLayoutParams();
        if (!(rawParams instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int[] location = new int[2];
        mDrawerLayout.getLocationOnScreen(location);
        int excludedLeft = Math.max(0, location[0] - params.leftMargin);
        int excludedTop = Math.max(0, location[1] - params.topMargin);
        int excludedRight = Math.max(0, metrics.widthPixels
            - location[0] - mDrawerLayout.getWidth() - params.rightMargin);
        int excludedBottom = Math.max(0, metrics.heightPixels
            - location[1] - mDrawerLayout.getHeight() - params.bottomMargin);

        int left = OminalDisplayGeometry.remainingInset(mChatInsetLeft, excludedLeft);
        int top = OminalDisplayGeometry.remainingInset(mChatInsetTop, excludedTop);
        int right = OminalDisplayGeometry.remainingInset(mChatInsetRight, excludedRight);
        int bottom = OminalDisplayGeometry.remainingInset(mChatInsetBottom, excludedBottom);
        if (params.leftMargin == left && params.topMargin == top
            && params.rightMargin == right && params.bottomMargin == bottom) {
            return;
        }
        params.setMargins(left, top, right, bottom);
        mDrawerLayout.setLayoutParams(params);
    }

    private int navigationBarHeightFallback() {
        int resourceId = getResources().getIdentifier(
            "navigation_bar_height", "dimen", "android");
        return resourceId == 0 ? 0 : getResources().getDimensionPixelSize(resourceId);
    }

    private void applyDisplayImeBottomInset(int bottomInset) {
        mDisplayImeInsetBottom = Math.max(0, bottomInset);
        applyDisplayViewportBottomInset();
    }

    private void applyDisplayViewportBottomInset() {
        if (mDisplayWarmHost == null) return;
        int systemInset = 0;
        if (mMode == MODE_DISPLAY) {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int decorHeight = getWindow().getDecorView().getHeight();
            systemInset = OminalDisplayGeometry.unconsumedSystemInset(
                mDisplaySystemInsetBottom, metrics.heightPixels, decorHeight);
        }
        int inset = systemInset + (mMode == MODE_DISPLAY ? mDisplayImeInsetBottom : 0);
        ViewGroup.LayoutParams rawParams = mDisplayWarmHost.getLayoutParams();
        if (!(rawParams instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;
        if (params.bottomMargin == inset) return;
        params.bottomMargin = inset;
        mDisplayWarmHost.setLayoutParams(params);
        mDisplayWarmHost.post(() -> {
            if (mNativeDisplayView != null) mNativeDisplayView.refreshDisplaySize();
        });
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
        return currentDisplayGeometry().toX11Spec();
    }

    private OminalDisplayGeometry currentDisplayGeometry() {
        OminalDisplayGeometry expected = expectedDisplayGeometry();
        boolean measuredDisplayAvailable = mNativeDisplayView != null
            && mNativeDisplayView.getWidth() > 0 && mNativeDisplayView.getHeight() > 0;
        if (!measuredDisplayAvailable) return expected;
        return OminalDisplayGeometry.fromBounds(mNativeDisplayView.getWidth(),
            mNativeDisplayView.getHeight(), expected.widthPixels, expected.heightPixels,
            expected.densityDpi);
    }

    private OminalDisplayGeometry expectedDisplayGeometry() {
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        else
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return OminalDisplayGeometry.fromViewport(0, 0,
            metrics.widthPixels, metrics.heightPixels,
            mDisplayNavigationInsetLeft, mDisplayNavigationInsetTop,
            mDisplayNavigationInsetRight,
            Math.max(mDisplayNavigationInsetBottom, mDisplaySystemInsetBottom),
            metrics.densityDpi);
    }

    private ImageButton createToolbarIconButton(int iconRes, String contentDescription) {
        UiSpec ui = ui();
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setImageTintList(ColorStateList.valueOf(ui.toolbarButton.text));
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setPadding(dp(9), dp(9), dp(9), dp(9));
        button.setContentDescription(contentDescription);
        button.setBackground(makeSurfaceDrawable(ui.toolbarButton, true));
        attachNativeRipple(button);
        attachPressFeedback(button);
        return button;
    }

    private void styleAccountButton() {
        if (mAccountButton == null) return;
        String harnessId = mActiveSession == null
            ? OminalHarnessRegistry.DEFAULT_HARNESS_ID : mActiveSession.harnessId;
        OminalAgentHarness harness = OminalHarnessRegistry.activeOrDefault(harnessId);
        boolean signedIn = OminalHarnessTerminal.CODEX_ID.equals(harnessId) && mCodexSignedIn;
        styleHeaderButton(mAccountButton, signedIn);
        mAccountButton.setContentDescription(harness.getDisplayName());
    }

    private boolean isLoloModeEnabled() {
        return mPrefs != null && mPrefs.getBoolean(PREF_LOLO_MODE_ENABLED, false);
    }

    private boolean isLightAppearanceEnabled() {
        return mPrefs != null && mPrefs.getBoolean(PREF_LIGHT_APPEARANCE, false);
    }

    private void setLightAppearanceEnabled(boolean enabled) {
        if (mPrefs == null || enabled == isLightAppearanceEnabled()) return;
        mPrefs.edit().putBoolean(PREF_LIGHT_APPEARANCE, enabled).commit();
        mLauncherSyncPending = true;
        recreate();
    }

    private void syncLauncherIcon(boolean lightAppearance) {
        PackageManager packageManager = getPackageManager();
        ComponentName lightIcon = new ComponentName(this, LAUNCHER_LIGHT_COMPONENT);
        ComponentName darkIcon = new ComponentName(this, LAUNCHER_DARK_COMPONENT);
        ComponentName desiredIcon = lightAppearance ? darkIcon : lightIcon;
        ComponentName staleIcon = lightAppearance ? lightIcon : darkIcon;

        // Keep one launcher entry alive throughout the swap. Some launchers cache the
        // disabled component immediately and otherwise drop the task or lose the icon.
        setLauncherComponentEnabled(packageManager, desiredIcon, true);
        setLauncherComponentEnabled(packageManager, staleIcon, false);
    }

    private void setLauncherComponentEnabled(PackageManager packageManager,
                                             ComponentName component, boolean enabled) {
        int desiredState = enabled
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if (packageManager.getComponentEnabledSetting(component) == desiredState) return;
        packageManager.setComponentEnabledSetting(component, desiredState,
            PackageManager.DONT_KILL_APP);
    }

    private void setLoloModeEnabled(boolean enabled) {
        if (mPrefs == null) return;
        mPrefs.edit().putBoolean(PREF_LOLO_MODE_ENABLED, enabled).apply();
        shutdownAgentRuntime();
        styleLoloButton();
        writeRuntimeContract(mActiveSession);
    }

    private void styleLoloButton() {
        if (mLoloButton == null) return;
        boolean enabled = isLoloModeEnabled();
        styleHeaderButton(mLoloButton, enabled);
        mLoloButton.setContentDescription(enabled
            ? "Lolo mode, experimental, on" : "Lolo mode, experimental, off");
    }

    private void showLoloModeDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(22), dp(24), dp(12));
        content.setBackgroundColor(dialogSurfaceColor());

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(dialogTitle("Lolo mode"), new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = new TextView(this);
        badge.setText("EXPERIMENTAL");
        badge.setTextColor(ui().ink);
        badge.setTextSize(10);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(makeRoundedDrawable(dialogBadgeColor(), ui().border, dp(10)));
        titleRow.addView(badge, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(titleRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView detail = dialogBody(
            "Lets Codex request selected actions outside its Linux workspace. It stays off until you enable it.");
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(10), 0, dp(18));
        content.addView(detail, detailParams);

        TextView appearanceSection = dialogBody("APPEARANCE");
        appearanceSection.setTextSize(11);
        appearanceSection.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams appearanceSectionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        appearanceSectionParams.setMargins(0, 0, 0, dp(6));
        content.addView(appearanceSection, appearanceSectionParams);

        LinearLayout appearanceRow = new LinearLayout(this);
        appearanceRow.setOrientation(LinearLayout.HORIZONTAL);
        appearanceRow.setGravity(Gravity.CENTER_VERTICAL);
        appearanceRow.setPadding(dp(14), 0, dp(8), 0);
        appearanceRow.setBackground(makeRoundedDrawable(dialogSubtleColor(), ui().border, dp(12)));

        LinearLayout appearanceCopy = new LinearLayout(this);
        appearanceCopy.setOrientation(LinearLayout.VERTICAL);
        TextView appearanceTitle = new TextView(this);
        appearanceTitle.setText("Light appearance");
        appearanceTitle.setTextColor(ui().ink);
        appearanceTitle.setTextSize(15);
        appearanceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        appearanceTitle.setIncludeFontPadding(false);
        TextView appearanceStatus = dialogBody(
            isLightAppearanceEnabled() ? "Light UI · dark launcher" : "Dark UI · silver launcher");
        appearanceCopy.addView(appearanceTitle);
        appearanceCopy.addView(appearanceStatus);
        appearanceRow.addView(appearanceCopy, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        SwitchCompat appearanceSwitch = createDialogSwitch();
        appearanceSwitch.setChecked(isLightAppearanceEnabled());
        appearanceSwitch.setOnCheckedChangeListener((button, checked) ->
            setLightAppearanceEnabled(checked));
        appearanceRow.addView(appearanceSwitch, new LinearLayout.LayoutParams(dp(54), dp(48)));
        content.addView(appearanceRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));

        TextView bridgeSection = dialogBody("AGENT ACCESS");
        bridgeSection.setTextSize(11);
        bridgeSection.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams bridgeSectionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bridgeSectionParams.setMargins(0, dp(20), 0, dp(6));
        content.addView(bridgeSection, bridgeSectionParams);

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setPadding(dp(14), 0, dp(8), 0);
        switchRow.setBackground(makeRoundedDrawable(dialogSubtleColor(), ui().border, dp(12)));

        LinearLayout switchCopy = new LinearLayout(this);
        switchCopy.setOrientation(LinearLayout.VERTICAL);
        TextView switchTitle = new TextView(this);
        switchTitle.setText("Android bridge");
        switchTitle.setTextColor(ui().ink);
        switchTitle.setTextSize(15);
        switchTitle.setTypeface(Typeface.DEFAULT_BOLD);
        switchTitle.setIncludeFontPadding(false);
        TextView switchStatus = dialogBody("");
        switchCopy.addView(switchTitle);
        switchCopy.addView(switchStatus);
        switchRow.addView(switchCopy, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        SwitchCompat modeSwitch = createDialogSwitch();
        switchRow.addView(modeSwitch, new LinearLayout.LayoutParams(dp(54), dp(48)));
        content.addView(switchRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));

        TextView section = dialogBody("AVAILABLE TO THE AGENT");
        section.setTextSize(11);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sectionParams.setMargins(0, dp(20), 0, dp(6));
        content.addView(section, sectionParams);

        TextView appsStatus = addLoloCapability(content, "Open apps and links");
        TextView settingsStatus = addLoloCapability(content, "Open Android settings");
        TextView screenStatus = addLoloCapability(content, "See the Android screen");
        TextView touchStatus = addLoloCapability(content, "Touch across Android");

        TextView footnote = dialogBody(
            "Screen sharing and global touch need their own Android consent. They are not enabled by this switch.");
        footnote.setTextSize(12);
        LinearLayout.LayoutParams footnoteParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        footnoteParams.setMargins(0, dp(16), 0, 0);
        content.addView(footnote, footnoteParams);

        updateLoloDialogState(modeSwitch, switchStatus, appsStatus, settingsStatus,
            screenStatus, touchStatus, isLoloModeEnabled());
        modeSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (Boolean.TRUE.equals(button.getTag())) return;
            if (!checked) {
                setLoloModeEnabled(false);
                updateLoloDialogState(modeSwitch, switchStatus, appsStatus, settingsStatus,
                    screenStatus, touchStatus, false);
                return;
            }
            button.setTag(Boolean.TRUE);
            modeSwitch.setChecked(false);
            button.setTag(null);
            showLoloEnableConfirmation(() -> {
                setLoloModeEnabled(true);
                updateLoloDialogState(modeSwitch, switchStatus, appsStatus, settingsStatus,
                    screenStatus, touchStatus, true);
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(content)
            .setNegativeButton("Done", null)
            .create();
        dialog.show();
        styleBlackDialog(dialog);
    }

    private TextView addLoloCapability(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(ui().ink);
        title.setTextSize(14);
        title.setIncludeFontPadding(false);
        row.addView(title, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView status = dialogBody("");
        status.setTextSize(12);
        status.setGravity(Gravity.END);
        row.addView(status, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        parent.addView(row, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View divider = new View(this);
        divider.setBackgroundColor(ui().border);
        parent.addView(divider, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        return status;
    }

    private void updateLoloDialogState(SwitchCompat modeSwitch, TextView switchStatus,
                                       TextView appsStatus, TextView settingsStatus,
                                       TextView screenStatus, TextView touchStatus,
                                       boolean enabled) {
        modeSwitch.setTag(Boolean.TRUE);
        modeSwitch.setChecked(enabled);
        modeSwitch.setTag(null);
        switchStatus.setText(enabled ? "On for this app" : "Off");
        appsStatus.setText(enabled ? "Available" : "Off");
        settingsStatus.setText(enabled ? "Available" : "Off");
        screenStatus.setText("Not connected");
        touchStatus.setText("Not connected");
    }

    private void showLoloEnableConfirmation(Runnable onEnabled) {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Enable Lolo mode?")
            .setMessage("Codex will be able to open Android apps, links, and Settings while Ominal is running. Android permissions still apply.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Enable", (ignored, which) -> onEnabled.run())
            .create();
        dialog.show();
        styleBlackDialog(dialog);
    }

    private TextView dialogTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ui().ink);
        view.setTextSize(20);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView dialogBody(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ui().muted);
        view.setTextSize(14);
        view.setLineSpacing(0, 1.08f);
        view.setIncludeFontPadding(false);
        return view;
    }

    private int dialogSurfaceColor() {
        return isLightAppearanceEnabled() ? Color.WHITE : Color.BLACK;
    }

    private int dialogSubtleColor() {
        return isLightAppearanceEnabled()
            ? Color.rgb(242, 242, 247) : Color.rgb(12, 12, 12);
    }

    private int dialogBadgeColor() {
        return isLightAppearanceEnabled()
            ? Color.rgb(232, 232, 237) : Color.rgb(24, 24, 24);
    }

    private SwitchCompat createDialogSwitch() {
        SwitchCompat toggle = new SwitchCompat(this);
        toggle.setShowText(false);
        toggle.setThumbTintList(new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{ui().ink, ui().muted}));
        toggle.setTrackTintList(new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{ui().border, dialogBadgeColor()}));
        return toggle;
    }

    private void styleBlackDialog(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawable(
            makeRoundedDrawable(dialogSurfaceColor(), ui().border, dp(16)));
        dialog.getWindow().setDimAmount(0.72f);
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) positive.setTextColor(ui().ink);
        if (negative != null) negative.setTextColor(ui().muted);
    }

    private void styleHeaderButton(ImageButton button, boolean active) {
        UiSpec ui = ui();
        SurfaceSpec surface = active ? ui.toolbarButtonActive : ui.toolbarButton;
        button.setImageTintList(ColorStateList.valueOf(surface.text));
        button.setBackground(makeSurfaceDrawable(surface, true));
        attachNativeRipple(button);
    }

    private ImageButton createComposerIconButton(int iconRes, String contentDescription) {
        UiSpec ui = ui();
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setImageTintList(ColorStateList.valueOf(ui.composerIcon.text));
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setContentDescription(contentDescription);
        button.setBackground(makeSurfaceDrawable(ui.composerIcon, true));
        attachNativeRipple(button);
        attachPressFeedback(button);
        return button;
    }

    private ImageButton createComposerSendButton(int iconRes, String contentDescription) {
        UiSpec ui = ui();
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setImageTintList(ColorStateList.valueOf(ui.composerSend.text));
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setContentDescription(contentDescription);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(ui.composerSend.fill);
        background.setStroke(dp(1), ui.composerSend.stroke);
        button.setBackground(background);
        attachNativeRipple(button);
        attachPressFeedback(button);
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
        int ink = ui().ink;
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(38,
            Color.red(ink), Color.green(ink), Color.blue(ink))), content, null));
    }

    private void attachPressFeedback(View view) {
        view.setOnTouchListener((target, event) -> {
            if (!target.isEnabled()) return false;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                target.animate().cancel();
                target.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                target.animate().cancel();
                target.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f)).start();
            }
            return false;
        });
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

        static UiSpec light(BrandSkin skin) {
            int canvas = Color.rgb(248, 248, 250);
            int panel = Color.WHITE;
            int panelSoft = Color.rgb(242, 242, 247);
            int ink = Color.rgb(17, 17, 18);
            int muted = Color.rgb(99, 99, 104);
            int accent = Color.BLACK;
            int accentDark = Color.WHITE;
            int border = Color.rgb(209, 209, 214);
            int dark = Color.WHITE;
            int onDark = ink;
            int onDarkMuted = muted;

            SurfaceSpec app = new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0);
            SurfaceSpec header = new SurfaceSpec(panel, Color.TRANSPARENT, ink, 0);
            SurfaceSpec toolbarButton =
                new SurfaceSpec(Color.TRANSPARENT, Color.TRANSPARENT, ink, 12);
            SurfaceSpec toolbarButtonActive =
                new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 12);
            SurfaceSpec drawer = new SurfaceSpec(panel, Color.TRANSPARENT, ink, 0);
            SurfaceSpec drawerSearch = new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 12);
            SurfaceSpec drawerRow = new SurfaceSpec(panel, Color.TRANSPARENT, ink, 12);
            SurfaceSpec drawerRowActive = new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 12);
            SurfaceSpec chat = new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0);
            SurfaceSpec bubbleUser = new SurfaceSpec(panelSoft,
                Color.TRANSPARENT, ink, 14);
            SurfaceSpec bubbleAgent = new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0);
            SurfaceSpec composer = new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0);
            SurfaceSpec composerInput = new SurfaceSpec(panel, border, ink, 16);
            SurfaceSpec composerIcon =
                new SurfaceSpec(Color.TRANSPARENT, Color.TRANSPARENT, ink, 10);
            SurfaceSpec composerSend = new SurfaceSpec(Color.BLACK, Color.BLACK, Color.WHITE, 12);
            SurfaceSpec buttonPrimary = new SurfaceSpec(Color.BLACK, Color.BLACK, Color.WHITE, 8);
            SurfaceSpec buttonSecondary = new SurfaceSpec(panel, border, ink, 8);
            SurfaceSpec modeButton = new SurfaceSpec(panel, border, ink, 8);
            SurfaceSpec modeButtonActive = new SurfaceSpec(Color.BLACK, Color.BLACK, Color.WHITE, 8);
            SurfaceSpec terminalBlock =
                new SurfaceSpec(Color.BLACK, Color.rgb(58, 58, 58), Color.WHITE, 6);
            SurfaceSpec displayHome =
                new SurfaceSpec(Color.BLACK, Color.TRANSPARENT, Color.WHITE, 0);
            SurfaceSpec displayTile =
                new SurfaceSpec(Color.BLACK, Color.rgb(58, 58, 58), Color.WHITE, 8);

            return new UiSpec(canvas, panel, panelSoft, ink, muted, accent, accentDark, border,
                dark, onDark, onDarkMuted, app, header, toolbarButton, toolbarButtonActive,
                drawer, drawerSearch, drawerRow, drawerRowActive, chat, bubbleUser, bubbleAgent,
                composer, composerInput, composerIcon, composerSend, buttonPrimary, buttonSecondary,
                modeButton, modeButtonActive, terminalBlock, displayHome, displayTile);
        }

        static UiSpec fromProperties(BrandSkin skin, Properties properties) {
            int canvas = readColor(properties, "color.canvas", skin.canvas);
            int panel = readColor(properties, "color.panel", skin.panel);
            int panelSoft = readColor(properties, "color.panelSoft", skin.panelSoft);
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
                new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0));
            SurfaceSpec toolbarButton = SurfaceSpec.fromProperties(properties, "surface.toolbarButton",
                new SurfaceSpec(Color.TRANSPARENT, Color.TRANSPARENT, ink, 12));
            SurfaceSpec toolbarButtonActive = SurfaceSpec.fromProperties(properties, "surface.toolbarButtonActive",
                new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 12));
            SurfaceSpec drawer = SurfaceSpec.fromProperties(properties, "surface.drawer",
                new SurfaceSpec(panel, Color.TRANSPARENT, ink, 0));
            SurfaceSpec drawerSearch = SurfaceSpec.fromProperties(properties, "surface.drawerSearch",
                new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 12));
            SurfaceSpec drawerRow = SurfaceSpec.fromProperties(properties, "surface.drawerRow",
                new SurfaceSpec(Color.TRANSPARENT, Color.TRANSPARENT, ink, 12));
            SurfaceSpec drawerRowActive = SurfaceSpec.fromProperties(properties, "surface.drawerRowActive",
                new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 12));
            SurfaceSpec chat = SurfaceSpec.fromProperties(properties, "surface.chat",
                new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0));
            SurfaceSpec bubbleUser = SurfaceSpec.fromProperties(properties, "surface.bubble.user",
                new SurfaceSpec(panelSoft, Color.TRANSPARENT, ink, 14));
            SurfaceSpec bubbleAgent = SurfaceSpec.fromProperties(properties, "surface.bubble.agent",
                new SurfaceSpec(Color.TRANSPARENT, Color.TRANSPARENT, ink, 0));
            SurfaceSpec composer = SurfaceSpec.fromProperties(properties, "surface.composer",
                new SurfaceSpec(canvas, Color.TRANSPARENT, ink, 0));
            SurfaceSpec composerInput = SurfaceSpec.fromProperties(properties, "surface.composerInput",
                new SurfaceSpec(COLOR_INPUT_GLASS, border, ink, 16));
            SurfaceSpec composerIcon = SurfaceSpec.fromProperties(properties, "surface.composerIcon",
                new SurfaceSpec(Color.TRANSPARENT, Color.TRANSPARENT, ink, 10));
            SurfaceSpec composerSend = SurfaceSpec.fromProperties(properties, "surface.composerSend",
                new SurfaceSpec(Color.WHITE, Color.WHITE, Color.BLACK, 12));
            SurfaceSpec buttonPrimary = SurfaceSpec.fromProperties(properties, "surface.buttonPrimary",
                new SurfaceSpec(Color.WHITE, Color.WHITE, Color.BLACK, 8));
            SurfaceSpec buttonSecondary = SurfaceSpec.fromProperties(properties, "surface.buttonSecondary",
                new SurfaceSpec(Color.BLACK, border, ink, 8));
            SurfaceSpec modeButton = SurfaceSpec.fromProperties(properties, "surface.modeButton",
                new SurfaceSpec(Color.BLACK, border, ink, 8));
            SurfaceSpec modeButtonActive = SurfaceSpec.fromProperties(properties, "surface.modeButtonActive",
                new SurfaceSpec(Color.WHITE, Color.WHITE, Color.BLACK, 8));
            SurfaceSpec terminalBlock = SurfaceSpec.fromProperties(properties, "surface.terminalBlock",
                new SurfaceSpec(Color.BLACK, border, ink, 6));
            SurfaceSpec displayHome = SurfaceSpec.fromProperties(properties, "surface.displayHome",
                new SurfaceSpec(Color.BLACK, Color.TRANSPARENT, onDark, 0));
            SurfaceSpec displayTile = SurfaceSpec.fromProperties(properties, "surface.displayTile",
                new SurfaceSpec(Color.BLACK, border, onDark, 8));

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
            this.panelSoft = mix(panel, ink, 16, 1);
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
        final HashMap<String, String> threadIds = new HashMap<>();
        final HashMap<String, String> modelIds = new HashMap<>();
        final HashMap<String, String> effortIds = new HashMap<>();
        final HashMap<String, Integer> contextCursors = new HashMap<>();
        final ArrayList<PendingTurn> pendingTurns = new ArrayList<>();
        PendingTurn activeTurn;
        String title;
        String harnessId = OminalHarnessRegistry.DEFAULT_HARNESS_ID;

        ChatSession(String id, String title, long createdAt, String rootPath) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
            this.rootPath = rootPath;
            this.workspacePath = rootPath + "/workspace";
            this.historyPath = rootPath + "/" + HISTORY_FILE_NAME;
        }

        String terminalName() {
            return "ominal-proot-" + id;
        }

        String threadId() {
            return threadId(harnessId);
        }

        String threadId(String requestedHarnessId) {
            String value = threadIds.get(requestedHarnessId);
            return value == null ? "" : value;
        }

        void setThreadId(String value) {
            setThreadId(harnessId, value);
        }

        void setThreadId(String requestedHarnessId, String value) {
            if (value == null || value.trim().isEmpty()) threadIds.remove(requestedHarnessId);
            else threadIds.put(requestedHarnessId, value.trim());
        }

        String modelId() {
            String value = modelIds.get(harnessId);
            return value == null ? "" : value;
        }

        void setModelId(String value) {
            if (value == null || value.trim().isEmpty()) modelIds.remove(harnessId);
            else modelIds.put(harnessId, value.trim());
        }

        String effortId() {
            String value = effortIds.get(harnessId);
            return value == null ? "" : value;
        }

        void setEffortId(String value) {
            if (value == null || value.trim().isEmpty()) effortIds.remove(harnessId);
            else effortIds.put(harnessId, value.trim());
        }

        int contextCursor() {
            return contextCursor(harnessId);
        }

        int contextCursor(String requestedHarnessId) {
            Integer value = contextCursors.get(requestedHarnessId);
            return value == null ? 0 : Math.max(0, value);
        }

        void markContextCurrent() {
            markContextCurrent(harnessId, messages.size());
        }

        void markContextCurrent(String requestedHarnessId, int cursor) {
            if (!threadId(requestedHarnessId).isEmpty())
                contextCursors.put(requestedHarnessId, Math.max(0, cursor));
        }
    }

    private static final class PendingTurn {
        final String prompt;
        final String harnessId;
        final String modelId;
        final String effortId;
        final int userMessageIndex;
        HashMap<String, String> mediaBefore = new HashMap<>();
        long startedAt;

        PendingTurn(String prompt, String harnessId, String modelId,
                    String effortId, int userMessageIndex) {
            this.prompt = prompt;
            this.harnessId = harnessId;
            this.modelId = modelId;
            this.effortId = effortId;
            this.userMessageIndex = Math.max(0, userMessageIndex);
        }

        JSONObject toJson() throws JSONException {
            return new JSONObject()
                .put("prompt", prompt)
                .put("harnessId", harnessId)
                .put("modelId", modelId)
                .put("effortId", effortId)
                .put("userMessageIndex", userMessageIndex)
                .put("startedAt", startedAt);
        }

        static PendingTurn fromJson(JSONObject object) {
            if (object == null) return null;
            String prompt = object.optString("prompt", "").trim();
            String harnessId = OminalHarnessRegistry.normalizeSelectedId(
                object.optString("harnessId", ""));
            if (prompt.isEmpty() || harnessId.isEmpty()) return null;
            PendingTurn turn = new PendingTurn(prompt, harnessId,
                object.optString("modelId", ""),
                object.optString("effortId", ""),
                object.optInt("userMessageIndex", 0));
            turn.startedAt = Math.max(0L, object.optLong("startedAt", 0L));
            return turn;
        }
    }

    private static final class SetupMarkView extends View {
        private static final long ENTRANCE_DURATION_MS = 720L;
        private static final long MINIMUM_VISIBLE_MS = 640L;
        private static final long EXIT_DURATION_MS = 280L;
        private static final float ENTRANCE_SCALE = 0.58f;
        private static final float EXIT_SCALE = 1.18f;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mark = new Path();
        private long entranceStartedAt = -1L;
        private long exitStartedAt = -1L;
        private float exitStartAlpha = 1f;
        private float exitStartScale = 1f;
        private Runnable exitEndAction;

        private final Runnable runEntranceFrame = new Runnable() {
            @Override
            public void run() {
                if (!isAttachedToWindow() || entranceStartedAt < 0L || exitStartedAt >= 0L) return;
                float progress = elapsedFraction(entranceStartedAt, ENTRANCE_DURATION_MS);
                float eased = 1f - (float) Math.pow(1f - progress, 4);
                setAlpha(Math.min(1f, progress / 0.68f));
                float scale = ENTRANCE_SCALE + ((1f - ENTRANCE_SCALE) * eased);
                setScaleX(scale);
                setScaleY(scale);
                if (progress < 1f) {
                    postOnAnimation(this);
                } else {
                    setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        };

        private final Runnable runExitFrame = new Runnable() {
            @Override
            public void run() {
                if (!isAttachedToWindow() || exitStartedAt < 0L) return;
                float progress = elapsedFraction(exitStartedAt, EXIT_DURATION_MS);
                float eased = progress * progress * (3f - (2f * progress));
                setAlpha(exitStartAlpha * (1f - eased));
                float scale = exitStartScale + ((EXIT_SCALE - exitStartScale) * eased);
                setScaleX(scale);
                setScaleY(scale);
                if (progress < 1f) {
                    postOnAnimation(this);
                    return;
                }
                setLayerType(View.LAYER_TYPE_NONE, null);
                Runnable endAction = exitEndAction;
                exitEndAction = null;
                if (endAction != null) endAction.run();
            }
        };

        private final Runnable startEntrance = () -> {
            if (!isAttachedToWindow()) return;
            removeCallbacks(runEntranceFrame);
            removeCallbacks(runExitFrame);
            entranceStartedAt = android.os.SystemClock.uptimeMillis();
            exitStartedAt = -1L;
            exitEndAction = null;
            setAlpha(0f);
            setScaleX(ENTRANCE_SCALE);
            setScaleY(ENTRANCE_SCALE);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            postOnAnimation(runEntranceFrame);
        };

        SetupMarkView(Context context) {
            super(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setForceDarkAllowed(false);
            setAlpha(0f);
            setScaleX(ENTRANCE_SCALE);
            setScaleY(ENTRANCE_SCALE);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setDither(true);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            postOnAnimation(startEntrance);
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(startEntrance);
            removeCallbacks(runEntranceFrame);
            removeCallbacks(runExitFrame);
            entranceStartedAt = -1L;
            exitStartedAt = -1L;
            exitEndAction = null;
            setLayerType(View.LAYER_TYPE_NONE, null);
            super.onDetachedFromWindow();
        }

        long timeUntilExitAllowed() {
            if (entranceStartedAt < 0L) return MINIMUM_VISIBLE_MS;
            long elapsed = android.os.SystemClock.uptimeMillis() - entranceStartedAt;
            return Math.max(0L, MINIMUM_VISIBLE_MS - elapsed);
        }

        void playExit(Runnable endAction) {
            removeCallbacks(startEntrance);
            removeCallbacks(runEntranceFrame);
            removeCallbacks(runExitFrame);
            exitStartedAt = android.os.SystemClock.uptimeMillis();
            exitStartAlpha = getAlpha();
            exitStartScale = getScaleX();
            exitEndAction = endAction;
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            postOnAnimation(runExitFrame);
        }

        private static float elapsedFraction(long startedAt, long durationMs) {
            long elapsed = Math.max(0L, android.os.SystemClock.uptimeMillis() - startedAt);
            return Math.min(1f, elapsed / (float) durationMs);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            float left = (getWidth() - size) / 2f;
            float top = (getHeight() - size) / 2f;
            float unit = size / 108f;

            canvas.save();
            canvas.translate(left + (8.64f * unit), top + (8.64f * unit));
            canvas.scale(0.84f * unit, 0.84f * unit);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.5f);

            mark.reset();
            mark.moveTo(34f, 69.5f);
            mark.cubicTo(26.2f, 66.2f, 20.8f, 58.9f, 20.8f, 50.5f);
            mark.cubicTo(20.8f, 38.5f, 30.5f, 28.8f, 42.5f, 28.8f);
            mark.cubicTo(54.5f, 28.8f, 64.3f, 38.5f, 64.3f, 50.5f);
            mark.cubicTo(64.3f, 58.9f, 58.9f, 66.2f, 51f, 69.5f);
            canvas.drawPath(mark, paint);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(20.8f, 71.1f, 36.2f, 75.5f, paint);
            canvas.drawRect(30.7f, 67.8f, 36.2f, 75.5f, paint);
            canvas.drawRect(48.8f, 67.8f, 54.3f, 75.5f, paint);
            canvas.drawRect(48.8f, 71.1f, 64.3f, 75.5f, paint);
            canvas.drawRect(71.6f, 71.1f, 90.1f, 75.5f, paint);
            canvas.restore();
        }
    }

    private static final class ChatMessage {
        final String role;
        final String text;
        final String timestamp;
        final String detail;
        final OminalAgentTrace.Snapshot trace;
        final List<OminalChatMedia.Item> media;

        ChatMessage(String role, String text, String timestamp) {
            this(role, text, timestamp, "", OminalAgentTrace.Snapshot.empty(),
                Collections.emptyList());
        }

        ChatMessage(String role, String text, String timestamp, String detail) {
            this(role, text, timestamp, detail, OminalAgentTrace.Snapshot.empty(),
                Collections.emptyList());
        }

        ChatMessage(String role, String text, String timestamp, String detail,
                    OminalAgentTrace.Snapshot trace) {
            this(role, text, timestamp, detail, trace, Collections.emptyList());
        }

        ChatMessage(String role, String text, String timestamp, String detail,
                    OminalAgentTrace.Snapshot trace, List<OminalChatMedia.Item> media) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
            this.detail = detail == null ? "" : detail;
            this.trace = trace == null ? OminalAgentTrace.Snapshot.empty() : trace;
            this.media = media == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(media));
        }
    }

    private final class AgentTurnView {
        final TextView message;
        final LinearLayout media;
        final View workSurface;
        final LinearLayout traceView;
        final TextView detail;
        final TextView breakdown;
        final WorkPulseView pulse;
        final TokenMeterView meter;
        String status;
        OminalAgentTrace.Snapshot trace = OminalAgentTrace.Snapshot.empty();
        OminalAgentTransport.TokenUsage usage;
        boolean running;
        boolean expanded;

        AgentTurnView(TextView message, LinearLayout media, View workSurface,
                      LinearLayout traceView, TextView detail, TextView breakdown,
                      WorkPulseView pulse, TokenMeterView meter, String status) {
            this.message = message;
            this.media = media;
            this.workSurface = workSurface;
            this.traceView = traceView;
            this.detail = detail;
            this.breakdown = breakdown;
            this.pulse = pulse;
            this.meter = meter;
            this.status = status == null ? "" : status;
            this.running = !this.status.isEmpty();
        }

        void setMessage(String value) {
            String text = value == null ? "" : value;
            renderMarkdown(message, text);
            message.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        }

        void setMedia(ChatSession session, List<OminalChatMedia.Item> items) {
            renderMediaItems(media, session, items);
        }
    }

    private static final class WorkPulseView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private final int idleColor;
        private final int activeColor;
        private int phase;
        private boolean running;
        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                phase = (phase + 1) % 2;
                invalidate();
                postDelayed(this, 520);
            }
        };

        WorkPulseView(Context context, int idleColor, int activeColor) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            this.idleColor = idleColor;
            this.activeColor = activeColor;
        }

        void setRunning(boolean value) {
            if (running == value) return;
            running = value;
            removeCallbacks(tick);
            if (running) post(tick);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStrokeWidth(1.5f * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(running ? activeColor : idleColor);
            paint.setAlpha(running ? (phase == 0 ? 255 : 96) : 132);
            float half = 4f * density;
            canvas.drawLine(getWidth() / 2f - half, getHeight() / 2f,
                getWidth() / 2f + half, getHeight() / 2f, paint);
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(tick);
            super.onDetachedFromWindow();
        }
    }

    private static final class WorkStateThumbnailView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private final String type;
        private final boolean running;
        private final int idleColor;
        private final int activeColor;
        private int phase;
        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                phase = (phase + 1) % 2;
                invalidate();
                postDelayed(this, 560);
            }
        };

        WorkStateThumbnailView(Context context, String type, boolean running,
                               int idleColor, int activeColor) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            this.type = type == null ? "" : type;
            this.running = running;
            this.idleColor = idleColor;
            this.activeColor = activeColor;
            if (running) post(tick);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(running ? activeColor : idleColor);
            paint.setAlpha(running && phase == 1 ? 118 : 230);
            paint.setStrokeWidth(1.35f * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            drawGlyph(canvas);
            paint.setAlpha(255);
        }

        private void drawGlyph(Canvas canvas) {
            float left = 4f * density;
            float right = getWidth() - 4f * density;
            float top = 4f * density;
            float middle = getHeight() / 2f;
            float bottom = getHeight() - 4f * density;

            if (type.startsWith("browser") || "webSearch".equals(type)) {
                canvas.drawRoundRect(left, top, right, bottom, 2f * density, 2f * density, paint);
                canvas.drawLine(left, top + 3.5f * density, right, top + 3.5f * density, paint);
            } else if ("commandExecution".equals(type)) {
                canvas.drawLine(left, top + density, left + 3f * density, middle, paint);
                canvas.drawLine(left + 3f * density, middle, left, bottom - density, paint);
                canvas.drawLine(left + 5f * density, bottom - density,
                    right, bottom - density, paint);
            } else if ("workspaceRead".equals(type) || "fileChange".equals(type)) {
                Path file = new Path();
                file.moveTo(left + density, top);
                file.lineTo(right - 3f * density, top);
                file.lineTo(right, top + 3f * density);
                file.lineTo(right, bottom);
                file.lineTo(left + density, bottom);
                file.close();
                canvas.drawPath(file, paint);
                canvas.drawLine(left + 3f * density, middle, right - 3f * density, middle, paint);
            } else if (type.startsWith("image")) {
                canvas.drawRoundRect(left, top, right, bottom, 2f * density, 2f * density, paint);
                Path mountain = new Path();
                mountain.moveTo(left + 2f * density, bottom - 2f * density);
                mountain.lineTo(left + 5f * density, middle);
                mountain.lineTo(right - 2f * density, bottom - 2f * density);
                canvas.drawPath(mountain, paint);
            } else if ("question".equals(type)) {
                canvas.drawCircle(getWidth() / 2f, middle, 4.5f * density, paint);
                canvas.drawLine(getWidth() / 2f, middle - 2f * density,
                    getWidth() / 2f, middle + density, paint);
            } else if (type.endsWith("ToolCall")) {
                canvas.drawCircle(left + density, middle, 1.2f * density, paint);
                canvas.drawCircle(getWidth() / 2f, middle, 1.2f * density, paint);
                canvas.drawCircle(right - density, middle, 1.2f * density, paint);
                canvas.drawLine(left + 2.2f * density, middle,
                    getWidth() / 2f - 1.2f * density, middle, paint);
                canvas.drawLine(getWidth() / 2f + 1.2f * density, middle,
                    right - 2.2f * density, middle, paint);
            } else {
                canvas.drawLine(left, top, right, top, paint);
                canvas.drawLine(left, middle, right - 2f * density, middle, paint);
                canvas.drawLine(left, bottom, right - 5f * density, bottom, paint);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(tick);
            super.onDetachedFromWindow();
        }
    }

    private static final class TokenMeterView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private final int trackColor;
        private final int mutedColor;
        private final int inkColor;
        private OminalAgentTransport.TokenUsage usage;

        TokenMeterView(Context context, int trackColor, int mutedColor, int inkColor) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            this.trackColor = trackColor;
            this.mutedColor = mutedColor;
            this.inkColor = inkColor;
        }

        void setUsage(OminalAgentTransport.TokenUsage value) {
            usage = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = getHeight() / 2f;
            paint.setColor(trackColor);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, paint);
            if (usage == null) return;

            long[] values = new long[]{
                Math.max(0, usage.inputTokens - usage.cachedInputTokens),
                Math.max(0, usage.cachedInputTokens),
                Math.max(0, usage.outputTokens - usage.reasoningOutputTokens),
                Math.max(0, usage.reasoningOutputTokens)
            };
            int[] colors = new int[]{
                withAlpha(mutedColor, 150), withAlpha(mutedColor, 220),
                withAlpha(inkColor, 235), withAlpha(inkColor, 150)
            };
            long total = 0;
            for (long value : values) total += value;
            if (total <= 0) return;

            float left = 0;
            float gap = 1.5f * density;
            for (int index = 0; index < values.length; index++) {
                if (values[index] <= 0) continue;
                float width = getWidth() * values[index] / (float) total;
                paint.setColor(colors[index]);
                canvas.drawRoundRect(left, 0,
                    Math.min(getWidth(), left + Math.max(density, width - gap)),
                    getHeight(), radius, radius, paint);
                left += width;
            }
        }

        private static int withAlpha(int color, int alpha) {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }
    }
}
