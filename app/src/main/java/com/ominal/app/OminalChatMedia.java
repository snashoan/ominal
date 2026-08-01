package com.ominal.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class OminalChatMedia {
    private static final int MAX_SCAN_DEPTH = 8;
    private static final int MAX_SCANNED_FILES = 10000;
    private static final int MAX_CHANGED_ITEMS = 64;
    private static final Set<String> SKIPPED_DIRECTORIES = new HashSet<>(Arrays.asList(
        ".git", ".gradle", ".ominal", "node_modules"
    ));
    private static final Map<String, String> MIME_TYPES = createMimeTypes();

    private OminalChatMedia() {
    }

    static final class Item {
        final String path;
        final String mimeType;
        final String name;

        Item(String path, String mimeType, String name) {
            this.path = normalizeRelativePath(path);
            this.name = name == null || name.trim().isEmpty()
                ? new File(this.path).getName() : name.trim();
            String requestedMimeType = mimeType == null
                ? "" : mimeType.trim().toLowerCase(Locale.US);
            this.mimeType = requestedMimeType.isEmpty()
                || "application/octet-stream".equals(requestedMimeType)
                ? mimeTypeForName(this.name) : requestedMimeType;
        }

        boolean isImage() {
            return mimeType.startsWith("image/");
        }

        JSONObject toJson() throws JSONException {
            return new JSONObject()
                .put("path", path)
                .put("mimeType", mimeType)
                .put("name", name);
        }

        static Item fromJson(JSONObject object) {
            if (object == null) return null;
            String path = normalizeRelativePath(object.optString("path", ""));
            if (path.isEmpty() || path.startsWith("../") || path.contains("/../")) return null;
            return new Item(path, object.optString("mimeType", ""),
                object.optString("name", ""));
        }
    }

    static Item fromRelativePath(File workspace, String relativePath, String explicitMimeType) {
        File file = resolve(workspace, relativePath);
        if (file == null || !file.isFile()) return null;
        String path = relativePath(workspace, file);
        if (path.isEmpty()) return null;
        String mimeType = explicitMimeType == null || explicitMimeType.trim().isEmpty()
            ? mimeTypeForName(file.getName()) : explicitMimeType;
        return new Item(path, mimeType, file.getName());
    }

    static HashMap<String, String> snapshot(File workspace) {
        HashMap<String, String> result = new HashMap<>();
        scan(workspace, workspace, 0, new int[]{0}, (path, file) ->
            result.put(path, fileState(file)));
        return result;
    }

    static ArrayList<Item> changedSince(File workspace, Map<String, String> before) {
        HashMap<String, String> current = snapshot(workspace);
        ArrayList<String> changedPaths = new ArrayList<>();
        for (Map.Entry<String, String> entry : current.entrySet()) {
            if (!entry.getValue().equals(before == null ? null : before.get(entry.getKey())))
                changedPaths.add(entry.getKey());
        }
        Collections.sort(changedPaths);

        ArrayList<Item> result = new ArrayList<>();
        for (String path : changedPaths) {
            Item item = fromRelativePath(workspace, path, "");
            if (item != null) result.add(item);
            if (result.size() >= MAX_CHANGED_ITEMS) break;
        }
        return result;
    }

    static JSONArray toJson(List<Item> items) throws JSONException {
        JSONArray array = new JSONArray();
        if (items == null) return array;
        for (Item item : items) {
            if (item != null) array.put(item.toJson());
        }
        return array;
    }

    static ArrayList<Item> fromJson(JSONArray array) {
        ArrayList<Item> result = new ArrayList<>();
        if (array == null) return result;
        for (int index = 0; index < array.length(); index++) {
            Item item = Item.fromJson(array.optJSONObject(index));
            if (item != null) result.add(item);
        }
        return result;
    }

    static File resolve(File workspace, String relativePath) {
        if (workspace == null || relativePath == null || relativePath.trim().isEmpty()) return null;
        try {
            File canonicalWorkspace = workspace.getCanonicalFile();
            File candidate = new File(canonicalWorkspace,
                normalizeRelativePath(relativePath)).getCanonicalFile();
            String root = canonicalWorkspace.getPath();
            String path = candidate.getPath();
            if (!path.equals(root) && !path.startsWith(root + File.separator)) return null;
            return candidate;
        } catch (IOException e) {
            return null;
        }
    }

    static String mimeTypeForName(String name) {
        String extension = extension(name);
        String mimeType = MIME_TYPES.get(extension);
        return mimeType == null ? "application/octet-stream" : mimeType;
    }

    private static void scan(File workspace, File directory, int depth, int[] scanned,
                             MediaVisitor visitor) {
        if (workspace == null || directory == null || depth > MAX_SCAN_DEPTH
            || scanned[0] >= MAX_SCANNED_FILES) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) return;
        Arrays.sort(children, (left, right) ->
            left.getName().compareToIgnoreCase(right.getName()));
        for (File child : children) {
            if (scanned[0] >= MAX_SCANNED_FILES) return;
            if (child.isDirectory()) {
                if (!SKIPPED_DIRECTORIES.contains(child.getName()))
                    scan(workspace, child, depth + 1, scanned, visitor);
                continue;
            }
            scanned[0]++;
            if (!child.isFile() || !isMediaName(child.getName())) continue;
            String path = relativePath(workspace, child);
            if (!path.isEmpty()) visitor.visit(path, child);
        }
    }

    private static boolean isMediaName(String name) {
        return MIME_TYPES.containsKey(extension(name));
    }

    private static String relativePath(File workspace, File file) {
        try {
            String root = workspace.getCanonicalFile().getPath();
            String path = file.getCanonicalFile().getPath();
            if (!path.startsWith(root + File.separator)) return "";
            return normalizeRelativePath(path.substring(root.length() + 1));
        } catch (IOException e) {
            return "";
        }
    }

    private static String fileState(File file) {
        return file.length() + ":" + file.lastModified();
    }

    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1
            ? "" : name.substring(dot + 1).toLowerCase(Locale.US);
    }

    private static String normalizeRelativePath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private static Map<String, String> createMimeTypes() {
        HashMap<String, String> types = new HashMap<>();
        types.put("png", "image/png");
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("gif", "image/gif");
        types.put("webp", "image/webp");
        types.put("bmp", "image/bmp");
        types.put("heic", "image/heic");
        types.put("heif", "image/heif");
        types.put("avif", "image/avif");
        types.put("svg", "image/svg+xml");
        types.put("mp4", "video/mp4");
        types.put("m4v", "video/x-m4v");
        types.put("mov", "video/quicktime");
        types.put("webm", "video/webm");
        types.put("mkv", "video/x-matroska");
        types.put("mp3", "audio/mpeg");
        types.put("m4a", "audio/mp4");
        types.put("wav", "audio/wav");
        types.put("ogg", "audio/ogg");
        types.put("flac", "audio/flac");
        types.put("pdf", "application/pdf");
        return types;
    }

    private interface MediaVisitor {
        void visit(String path, File file);
    }
}
