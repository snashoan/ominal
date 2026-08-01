package com.ominal.app;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/** Imports a narrowly allowlisted configuration archive from a user-controlled Termux export. */
final class OminalTermuxConfigMigration {

    private static final int TAR_BLOCK_SIZE = 512;
    private static final long MAX_ARCHIVE_BYTES = 32L * 1024L * 1024L;
    private static final int MAX_ARCHIVE_ENTRIES = 256;
    private static final String[] TOP_LEVEL_ENTRIES = {
        ".termux", ".bashrc", ".bash_profile", ".profile", ".zshrc", ".zprofile",
        ".gitconfig", ".ssh", ".config"
    };

    private OminalTermuxConfigMigration() {}

    static Result importArchive(Context context, Uri archiveUri, File homeDirectory) throws IOException {
        if (!homeDirectory.isDirectory())
            throw new IOException("Ominal home directory is not ready yet.");

        File importRoot = new File(homeDirectory, ".ominal/imports");
        File stage = new File(importRoot, "termux-" + Long.toString(System.currentTimeMillis(), 36));
        if (!stage.mkdirs()) throw new IOException("Could not create the import staging area.");

        try {
            int extractedEntries = extractArchive(context.getContentResolver(), archiveUri, stage);
            int copiedEntries = mergeStage(stage, homeDirectory);
            if (copiedEntries == 0) throw new IOException("The archive did not contain supported settings.");
            return new Result(extractedEntries, copiedEntries);
        } finally {
            deleteRecursively(stage);
        }
    }

    private static int extractArchive(ContentResolver resolver, Uri archiveUri, File stage) throws IOException {
        InputStream raw = resolver.openInputStream(archiveUri);
        if (raw == null) throw new IOException("The selected archive could not be opened.");

        long extractedBytes = 0L;
        int extractedEntries = 0;
        try (InputStream input = new BufferedInputStream(new GZIPInputStream(raw))) {
            byte[] header = new byte[TAR_BLOCK_SIZE];
            while (true) {
                int headerBytes = readFully(input, header, 0, header.length);
                if (headerBytes == -1 || isZeroBlock(header)) break;
                if (headerBytes != TAR_BLOCK_SIZE) throw new IOException("The archive header is incomplete.");
                if (++extractedEntries > MAX_ARCHIVE_ENTRIES)
                    throw new IOException("The archive contains too many entries.");

                String path = archivePath(header);
                if (!isAllowedPath(path)) throw new IOException("Unsupported archive entry: " + path);
                long size = readTarNumber(header, 124, 12);
                if (size < 0 || size > MAX_ARCHIVE_BYTES - extractedBytes)
                    throw new IOException("The archive is too large.");

                int type = header[156] & 0xff;
                File target = safeTarget(stage, path);
                if (type == '5') {
                    if (!target.isDirectory() && !target.mkdirs())
                        throw new IOException("Could not create settings directory: " + path);
                    skipFully(input, size);
                } else if (type == 0 || type == '0') {
                    File parent = target.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs())
                        throw new IOException("Could not create settings directory: " + path);
                    copyExactly(input, target, size);
                    extractedBytes += size;
                } else {
                    throw new IOException("Archive links and special files are not supported.");
                }
                skipFully(input, (TAR_BLOCK_SIZE - (size % TAR_BLOCK_SIZE)) % TAR_BLOCK_SIZE);
            }
        }
        return extractedEntries;
    }

    private static int mergeStage(File stage, File homeDirectory) throws IOException {
        File backupRoot = new File(homeDirectory, ".ominal/termux-config-backups/"
            + Long.toString(System.currentTimeMillis(), 36));
        int copiedEntries = 0;
        for (String entry : TOP_LEVEL_ENTRIES) {
            File source = new File(stage, entry);
            if (!source.exists()) continue;

            File destination = new File(homeDirectory, entry);
            if (destination.exists()) copyRecursively(destination, new File(backupRoot, entry));
            deleteRecursively(destination);
            copyRecursively(source, destination);
            restrictToOwner(destination);
            copiedEntries++;
        }
        return copiedEntries;
    }

    private static String archivePath(byte[] header) throws IOException {
        String name = readTarString(header, 0, 100);
        String prefix = readTarString(header, 345, 155);
        if (!prefix.isEmpty()) name = prefix + "/" + name;
        while (name.startsWith("./")) name = name.substring(2);
        if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        if (name.isEmpty() || name.startsWith("/") || name.indexOf('\\') >= 0)
            throw new IOException("The archive contains an unsafe path.");
        for (String segment : name.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment))
                throw new IOException("The archive contains an unsafe path.");
        }
        return name;
    }

    private static boolean isAllowedPath(String path) {
        return ".bashrc".equals(path) || ".bash_profile".equals(path) || ".profile".equals(path)
            || ".zshrc".equals(path) || ".zprofile".equals(path) || ".gitconfig".equals(path)
            || ".termux".equals(path) || path.startsWith(".termux/")
            || ".ssh".equals(path) || path.startsWith(".ssh/")
            || ".config".equals(path) || ".config/git".equals(path) || path.startsWith(".config/git/");
    }

    private static File safeTarget(File stage, String path) throws IOException {
        File target = new File(stage, path);
        String stagePath = stage.getCanonicalPath() + File.separator;
        String targetPath = target.getCanonicalPath();
        if (!targetPath.startsWith(stagePath)) throw new IOException("The archive escapes its staging area.");
        return target;
    }

    private static String readTarString(byte[] value, int offset, int length) {
        int end = offset;
        int limit = offset + length;
        while (end < limit && value[end] != 0) end++;
        return new String(value, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    private static long readTarNumber(byte[] value, int offset, int length) throws IOException {
        long result = 0L;
        boolean sawDigit = false;
        for (int index = offset; index < offset + length; index++) {
            int current = value[index] & 0xff;
            if (current == 0 || current == ' ') break;
            if (current < '0' || current > '7') throw new IOException("The archive has an invalid size.");
            sawDigit = true;
            result = (result << 3) + current - '0';
            if (result < 0) throw new IOException("The archive size overflowed.");
        }
        return sawDigit ? result : 0L;
    }

    private static boolean isZeroBlock(byte[] block) {
        for (byte value : block) if (value != 0) return false;
        return true;
    }

    private static int readFully(InputStream input, byte[] buffer, int offset, int length) throws IOException {
        int total = 0;
        while (total < length) {
            int read = input.read(buffer, offset + total, length - total);
            if (read < 0) return total == 0 ? -1 : total;
            total += read;
        }
        return total;
    }

    private static void copyExactly(InputStream input, File target, long length) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        long remaining = length;
        try (FileOutputStream output = new FileOutputStream(target)) {
            while (remaining > 0) {
                int request = (int) Math.min(buffer.length, remaining);
                int read = input.read(buffer, 0, request);
                if (read < 0) throw new IOException("The archive ended before a file was complete.");
                output.write(buffer, 0, read);
                remaining -= read;
            }
            output.getFD().sync();
        }
    }

    private static void skipFully(InputStream input, long length) throws IOException {
        long remaining = length;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            if (input.read() < 0) throw new IOException("The archive ended unexpectedly.");
            remaining--;
        }
    }

    private static void copyRecursively(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.isDirectory() && !destination.mkdirs())
                throw new IOException("Could not create settings directory.");
            File[] children = source.listFiles();
            if (children == null) throw new IOException("Could not read settings directory.");
            for (File child : children) copyRecursively(child, new File(destination, child.getName()));
            return;
        }
        File parent = destination.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Could not create settings directory.");
        try (InputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        }
    }

    private static void restrictToOwner(File target) {
        if (target.isDirectory()) {
            target.setReadable(false, false);
            target.setWritable(false, false);
            target.setExecutable(false, false);
            target.setReadable(true, true);
            target.setWritable(true, true);
            target.setExecutable(true, true);
            File[] children = target.listFiles();
            if (children != null) for (File child : children) restrictToOwner(child);
            return;
        }
        target.setReadable(false, false);
        target.setWritable(false, false);
        target.setExecutable(false, false);
        target.setReadable(true, true);
        target.setWritable(true, true);
    }

    private static void deleteRecursively(File target) {
        if (target == null || !target.exists()) return;
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        if (!target.delete()) target.deleteOnExit();
    }

    static final class Result {
        final int archiveEntries;
        final int copiedEntries;

        Result(int archiveEntries, int copiedEntries) {
            this.archiveEntries = archiveEntries;
            this.copiedEntries = copiedEntries;
        }
    }
}
