package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;
import com.ominal.shared.runtime.OminalConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Validated, non-secret capabilities reported by an installed harness. */
public final class OminalHarnessManifest {
    static final int SCHEMA_VERSION = 1;
    private static final String LOG_TAG = "OminalHarnessManifest";
    private static final long MAX_FILE_BYTES = 256L * 1024L;
    private static final int MAX_MODELS = 128;
    private static final int MAX_COMMANDS = 256;
    private static final Pattern HARNESS_ID =
        Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    private static final Pattern FLAG =
        Pattern.compile("--[a-zA-Z0-9][a-zA-Z0-9-]{0,63}");
    private static final Pattern COMMAND =
        Pattern.compile("/[a-z][a-z0-9._-]{0,63}");
    private static final Pattern ADAPTER_COMMAND =
        Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern ICON_FILE =
        Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}\\.(?:png|webp)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHA256 = Pattern.compile("[a-fA-F0-9]{64}");
    private static final Set<String> OUTPUT_FORMATS =
        setOf("text", "json", "stream-json", "monopot-jsonl");
    private static final Set<String> COMMAND_TYPES = setOf(
        "command", "model", "effort", "agent", "account", "session", "plugin");

    public static final class Model {
        @NonNull public final String id;
        @NonNull public final String label;
        @NonNull public final List<String> efforts;

        private Model(@NonNull String id, @NonNull String label,
                      @NonNull List<String> efforts) {
            this.id = id;
            this.label = label;
            this.efforts = efforts;
        }
    }

    public static final class Command {
        @NonNull public final String name;
        @NonNull public final String type;

        private Command(@NonNull String name, @NonNull String type) {
            this.name = name;
            this.type = type;
        }
    }

    @NonNull public final String harnessId;
    @NonNull public final String binaryVersion;
    @NonNull public final String displayName;
    @NonNull public final String publisher;
    @NonNull public final String providerId;
    @NonNull public final String outputFormat;
    @NonNull public final String adapterCommand;
    @NonNull public final String transportId;
    @NonNull public final String resumeFlag;
    @NonNull public final String modelFlag;
    @NonNull public final String effortFlag;
    @NonNull public final String autonomyFlag;
    public final boolean autonomyEnabledByDefault;
    @NonNull public final List<Model> models;
    @NonNull public final List<Command> commands;
    @NonNull public final String iconFileName;
    @NonNull public final String monochromeIconFileName;
    @NonNull public final String iconSha256;
    @Nullable private final File sourceFile;

    private OminalHarnessManifest(@NonNull String harnessId,
                                  @NonNull String binaryVersion,
                                  @NonNull String displayName,
                                  @NonNull String publisher,
                                  @NonNull String providerId,
                                  @NonNull String outputFormat,
                                  @NonNull String adapterCommand,
                                  @NonNull String transportId,
                                  @NonNull String resumeFlag,
                                  @NonNull String modelFlag,
                                  @NonNull String effortFlag,
                                  @NonNull String autonomyFlag,
                                  boolean autonomyEnabledByDefault,
                                  @NonNull List<Model> models,
                                  @NonNull List<Command> commands,
                                  @NonNull String iconFileName,
                                  @NonNull String monochromeIconFileName,
                                  @NonNull String iconSha256,
                                  @Nullable File sourceFile) {
        this.harnessId = harnessId;
        this.binaryVersion = binaryVersion;
        this.displayName = displayName;
        this.publisher = publisher;
        this.providerId = providerId;
        this.outputFormat = outputFormat;
        this.adapterCommand = adapterCommand;
        this.transportId = transportId;
        this.resumeFlag = resumeFlag;
        this.modelFlag = modelFlag;
        this.effortFlag = effortFlag;
        this.autonomyFlag = autonomyFlag;
        this.autonomyEnabledByDefault = autonomyEnabledByDefault;
        this.models = Collections.unmodifiableList(models);
        this.commands = Collections.unmodifiableList(commands);
        this.iconFileName = iconFileName;
        this.monochromeIconFileName = monochromeIconFileName;
        this.iconSha256 = iconSha256;
        this.sourceFile = sourceFile;
    }

    @NonNull
    static OminalHarnessManifest fromJson(@NonNull JSONObject object) throws JSONException {
        return fromJson(object, null);
    }

    @NonNull
    private static OminalHarnessManifest fromJson(@NonNull JSONObject object,
                                                   @Nullable File sourceFile)
        throws JSONException {
        if (object.getInt("schemaVersion") != SCHEMA_VERSION)
            throw new JSONException("Unsupported harness manifest schema");

        String harnessId = requirePattern(object.getString("harness"),
            "harness", HARNESS_ID);
        String binaryVersion = requireText(object.getString("binaryVersion"),
            "binaryVersion", 128);

        JSONObject identity = object.optJSONObject("identity");
        String displayName = identity == null ? ""
            : optionalSafeText(identity, "name", 80);
        String publisher = identity == null ? ""
            : optionalSafeText(identity, "publisher", 120);
        String providerId = identity == null ? harnessId
            : optionalSafeText(identity, "provider", 80);
        if (providerId.isEmpty()) providerId = harnessId;

        JSONObject transport = object.getJSONObject("transport");
        String outputFormat = transport.optString("outputFormat", "text")
            .trim().toLowerCase(Locale.ROOT);
        if (!OUTPUT_FORMATS.contains(outputFormat))
            throw new JSONException("Unsupported output format");
        String adapterCommand = optionalAdapterCommand(
            transport.optString("adapterCommand", ""));
        String transportId = optionalSafeText(transport, "id", 120);
        if (transportId.isEmpty()) transportId = adapterCommand.isEmpty()
            ? outputFormat : "monopot-stdio:" + adapterCommand;
        if (!adapterCommand.isEmpty() && !"monopot-jsonl".equals(outputFormat))
            throw new JSONException("Runtime adapters must emit monopot-jsonl");
        String resumeFlag = optionalFlag(transport.optString("resumeFlag", ""),
            "resumeFlag");
        String modelFlag = optionalFlag(transport.optString("modelFlag", ""),
            "modelFlag");
        String effortFlag = optionalFlag(transport.optString("effortFlag", ""),
            "effortFlag");

        JSONObject autonomy = object.getJSONObject("autonomy");
        String autonomyFlag = optionalFlag(autonomy.optString("flag", ""),
            "autonomy flag");
        boolean autonomyEnabled = autonomy.optBoolean("enabledByDefault", false);
        if (autonomyEnabled && autonomyFlag.isEmpty())
            throw new JSONException("Default autonomy requires a verified flag");

        List<Model> models = parseModels(object.optJSONArray("models"));
        List<Command> commands = parseCommands(object.optJSONArray("commands"));
        JSONObject presentation = object.optJSONObject("presentation");
        JSONObject icon = presentation == null ? null : presentation.optJSONObject("icon");
        String iconFileName = optionalIconFile(icon, "file");
        String monochromeIconFileName = optionalIconFile(icon, "monochrome");
        String iconSha256 = optionalSha256(icon, "sha256");
        return new OminalHarnessManifest(harnessId, binaryVersion, displayName, publisher,
            providerId, outputFormat, adapterCommand, transportId,
            resumeFlag, modelFlag, effortFlag, autonomyFlag, autonomyEnabled,
            models, commands, iconFileName, monochromeIconFileName, iconSha256, sourceFile);
    }

    @Nullable
    public static OminalHarnessManifest load(@NonNull String requestedHarnessId) {
        if (!HARNESS_ID.matcher(requestedHarnessId).matches()) return null;
        File file = packageManifestFile(requestedHarnessId);
        if (!file.isFile()) file = manifestFile(requestedHarnessId);
        if (!file.isFile() || file.length() <= 0 || file.length() > MAX_FILE_BYTES) return null;
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            OminalHarnessManifest manifest = fromJson(new JSONObject(json), file);
            return requestedHarnessId.equals(manifest.harnessId) ? manifest : null;
        } catch (IOException | JSONException | RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Ignoring invalid harness manifest for " + requestedHarnessId, e);
            return null;
        }
    }

    @NonNull
    public static List<OminalHarnessManifest> installed() {
        LinkedHashSet<String> harnessIds = new LinkedHashSet<>();
        File packageDirectory = registryDirectory();
        File[] packages = packageDirectory.listFiles(File::isDirectory);
        if (packages != null) {
            Arrays.sort(packages, (left, right) -> left.getName().compareTo(right.getName()));
            for (File directory : packages) {
                if (HARNESS_ID.matcher(directory.getName()).matches()
                    && new File(directory, "manifest.json").isFile()) {
                    harnessIds.add(directory.getName());
                }
            }
        }
        File directory = legacyDirectory();
        File[] files = directory.listFiles((parent, name) ->
            name.endsWith(".json") && name.length() > 5);
        if (files != null) {
            Arrays.sort(files, (left, right) -> left.getName().compareTo(right.getName()));
            for (File file : files) {
                String name = file.getName();
                harnessIds.add(name.substring(0, name.length() - 5));
            }
        }
        if (harnessIds.isEmpty()) return Collections.emptyList();
        ArrayList<OminalHarnessManifest> manifests = new ArrayList<>();
        for (String harnessId : harnessIds) {
            OminalHarnessManifest manifest = load(harnessId);
            if (manifest != null) manifests.add(manifest);
        }
        return Collections.unmodifiableList(manifests);
    }

    @NonNull
    public List<String> commandNames() {
        ArrayList<String> names = new ArrayList<>(commands.size());
        for (Command command : commands) names.add(command.name);
        return Collections.unmodifiableList(names);
    }

    @NonNull
    static File manifestFile(@NonNull String harnessId) {
        return new File(legacyDirectory(), harnessId + ".json");
    }

    @NonNull
    static File packageManifestFile(@NonNull String harnessId) {
        return new File(new File(registryDirectory(), harnessId), "manifest.json");
    }

    @NonNull
    static File resolvedManifestFile(@NonNull String harnessId) {
        File packaged = packageManifestFile(harnessId);
        return packaged.isFile() ? packaged : manifestFile(harnessId);
    }

    @NonNull
    static File registryDirectory() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH,
            ".ominal/harness-registry");
    }

    @NonNull
    static File legacyDirectory() {
        return new File(OminalConstants.OMINAL_HOME_DIR_PATH,
            ".ominal/harness-capabilities");
    }

    @Nullable
    public File iconFile(boolean preferMonochrome) {
        if (sourceFile == null) return null;
        String requested = preferMonochrome && !monochromeIconFileName.isEmpty()
            ? monochromeIconFileName : iconFileName;
        if (requested.isEmpty()) return null;
        File file = new File(sourceFile.getParentFile(), requested);
        if (!file.isFile() || file.length() <= 0 || file.length() > MAX_FILE_BYTES) return null;
        if (!iconSha256.isEmpty() && requested.equals(iconFileName)
            && !iconSha256.equals(sha256(file))) return null;
        return file;
    }

    @NonNull
    private static List<Model> parseModels(@Nullable JSONArray array) throws JSONException {
        if (array == null) return new ArrayList<>();
        if (array.length() > MAX_MODELS) throw new JSONException("Too many harness models");
        ArrayList<Model> models = new ArrayList<>(array.length());
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject object = array.getJSONObject(index);
            String id = requireText(object.getString("id"), "model id", 160);
            if (!isSafeText(id) || !ids.add(id))
                throw new JSONException("Invalid or duplicate model id");
            String label = requireText(object.optString("label", id), "model label", 160);
            if (!isSafeText(label)) throw new JSONException("Invalid model label");
            JSONArray effortArray = object.optJSONArray("efforts");
            ArrayList<String> efforts = new ArrayList<>();
            if (effortArray != null) {
                if (effortArray.length() > 16) throw new JSONException("Too many effort levels");
                LinkedHashSet<String> uniqueEfforts = new LinkedHashSet<>();
                for (int effortIndex = 0; effortIndex < effortArray.length(); effortIndex++) {
                    String effort = requireText(effortArray.getString(effortIndex),
                        "effort", 32);
                    if (!isSafeText(effort) || !uniqueEfforts.add(effort))
                        throw new JSONException("Invalid or duplicate effort");
                    efforts.add(effort);
                }
            }
            models.add(new Model(id, label, Collections.unmodifiableList(efforts)));
        }
        return models;
    }

    @NonNull
    private static List<Command> parseCommands(@Nullable JSONArray array) throws JSONException {
        if (array == null) return new ArrayList<>();
        if (array.length() > MAX_COMMANDS) throw new JSONException("Too many harness commands");
        ArrayList<Command> commands = new ArrayList<>(array.length());
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject object = array.getJSONObject(index);
            String name = requirePattern(object.getString("name"),
                "command name", COMMAND);
            if (!names.add(name)) throw new JSONException("Duplicate harness command");
            String type = object.optString("type", "command")
                .trim().toLowerCase(Locale.ROOT);
            if (!COMMAND_TYPES.contains(type))
                throw new JSONException("Unsupported harness command type");
            commands.add(new Command(name, type));
        }
        return commands;
    }

    private static String optionalFlag(String value, String field) throws JSONException {
        String flag = value == null ? "" : value.trim();
        if (flag.isEmpty()) return "";
        return requirePattern(flag, field, FLAG);
    }

    private static String optionalAdapterCommand(String value) throws JSONException {
        String command = value == null ? "" : value.trim();
        if (command.isEmpty()) return "";
        if (!ADAPTER_COMMAND.matcher(command).matches())
            throw new JSONException("Invalid adapter command");
        return command;
    }

    private static String optionalIconFile(@Nullable JSONObject icon, String field)
        throws JSONException {
        if (icon == null || !icon.has(field) || icon.isNull(field)) return "";
        String file = icon.optString(field, "").trim();
        if (file.isEmpty()) return "";
        if (!ICON_FILE.matcher(file).matches())
            throw new JSONException("Invalid harness icon " + field);
        return file;
    }

    private static String optionalSha256(@Nullable JSONObject icon, String field)
        throws JSONException {
        if (icon == null || !icon.has(field) || icon.isNull(field)) return "";
        String digest = icon.optString(field, "").trim();
        if (digest.isEmpty()) return "";
        if (!SHA256.matcher(digest).matches())
            throw new JSONException("Invalid harness icon checksum");
        return digest.toLowerCase(Locale.ROOT);
    }

    private static String optionalSafeText(JSONObject object, String field, int maxLength)
        throws JSONException {
        if (!object.has(field) || object.isNull(field)) return "";
        String text = object.optString(field, "").trim();
        if (text.isEmpty() || text.length() > maxLength || !isSafeText(text))
            throw new JSONException("Invalid identity " + field);
        return text;
    }

    private static String requirePattern(String value, String field, Pattern pattern)
        throws JSONException {
        String text = requireText(value, field, 160);
        if (!pattern.matcher(text).matches()) throw new JSONException("Invalid " + field);
        return text;
    }

    private static String requireText(String value, String field, int maxLength)
        throws JSONException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > maxLength)
            throw new JSONException("Invalid " + field);
        return text;
    }

    private static boolean isSafeText(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character)) return false;
        }
        return true;
    }

    private static Set<String> setOf(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        Collections.addAll(set, values);
        return Collections.unmodifiableSet(set);
    }

    private static String sha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] value = digest.digest(bytes);
            StringBuilder encoded = new StringBuilder(value.length * 2);
            for (byte current : value) encoded.append(String.format(Locale.ROOT, "%02x", current));
            return encoded.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            return "";
        }
    }
}
