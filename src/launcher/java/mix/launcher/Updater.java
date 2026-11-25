package mix.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Updater {
    private Updater() {
    }

    public static final class Entry {
        public String path;
        public String url;
        public String sha256;
        public String sha1;
    }

    public static final class Update {
        public String version;
        public String changelog;
        public String manifestUrl;
        public final List<Entry> files = new ArrayList<>();
    }

    public static Update check(String manifestUrl, File distDir) throws Exception {
        validateUrl(manifestUrl);
        JsonObject root = readJson(manifestUrl);
        if (!root.has("version")) {
            throw new IllegalStateException("Update manifest has no version");
        }
        Update update = new Update();
        update.version = root.get("version").getAsString();
        update.manifestUrl = manifestUrl;
        update.changelog = root.has("changelog") ? root.get("changelog").getAsString() : "";
        if (root.has("files")) {
            JsonArray files = root.getAsJsonArray("files");
            for (JsonElement element : files) {
                JsonObject file = element.getAsJsonObject();
                Entry entry = new Entry();
                entry.path = file.get("path").getAsString();
                entry.url = file.get("url").getAsString();
                entry.sha256 = file.has("sha256") ? file.get("sha256").getAsString() : null;
                entry.sha1 = file.has("sha1") ? file.get("sha1").getAsString() : null;
                validatePath(entry.path);
                validateUrl(entry.url);
                update.files.add(entry);
            }
        }
        return update;
    }

    public static String localVersion(File distDir) {
        File file = new File(distDir, "version.json");
        if (file.isFile()) {
            try (java.io.Reader reader = new java.io.FileReader(file)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                return root.has("version") ? root.get("version").getAsString() : "unknown";
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    public static boolean isNewer(String local, String remote) {
        if (local == null || "unknown".equals(local)) {
            return true;
        }
        if (local.equals(remote)) {
            return false;
        }
        try {
            String[] a = local.split("\\.");
            String[] b = remote.split("\\.");
            int length = Math.max(a.length, b.length);
            for (int i = 0; i < length; i++) {
                int left = i < a.length ? Integer.parseInt(a[i].replaceAll("[^0-9]", "")) : 0;
                int right = i < b.length ? Integer.parseInt(b[i].replaceAll("[^0-9]", "")) : 0;
                if (left != right) {
                    return right > left;
                }
            }
        } catch (Exception ignored) {
        }
        return !local.equals(remote);
    }

    public static void apply(Update update, File distDir, Consumer<String> log) throws Exception {
        File tempDir = new File(distDir, ".update");
        delete(tempDir);
        if (!tempDir.mkdirs()) {
            throw new IllegalStateException("Cannot create " + tempDir);
        }

        try {
            for (Entry entry : update.files) {
                File target = new File(distDir, entry.path);
                if (target.getName().equals("MixClient-Launcher.jar")) {
                    log.accept("Skipping running launcher jar (replace manually): " + entry.path);
                    continue;
                }
                File temp = new File(tempDir, entry.path.replace('/', File.separatorChar));
                File parent = temp.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Cannot create " + parent);
                }
                log.accept("Downloading " + entry.path);
                download(entry.url, temp);
                if (entry.sha256 != null && !entry.sha256.isEmpty()) {
                    String actual = sha(temp, "SHA-256");
                    if (!actual.equalsIgnoreCase(entry.sha256)) {
                        throw new IllegalStateException("SHA-256 mismatch for " + entry.path);
                    }
                } else if (entry.sha1 != null && !entry.sha1.isEmpty()) {
                    String actual = sha(temp, "SHA-1");
                    if (!actual.equalsIgnoreCase(entry.sha1)) {
                        throw new IllegalStateException("SHA-1 mismatch for " + entry.path);
                    }
                }
            }

            for (int i = 0; i < update.files.size(); i++) {
                Entry entry = update.files.get(i);
                File target = new File(distDir, entry.path);
                if (target.getName().equals("MixClient-Launcher.jar")) {
                    continue;
                }
                File temp = new File(tempDir, entry.path.replace('/', File.separatorChar));
                if (!temp.isFile()) {
                    continue;
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Cannot create " + parent);
                }
                Files.move(temp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            JsonObject version = new JsonObject();
            version.addProperty("version", update.version);
            version.addProperty("updatedAt", System.currentTimeMillis());
            try (java.io.Writer writer = new java.io.FileWriter(new File(distDir, "version.json"))) {
                writer.write(version.toString());
            }
            log.accept("Updated to " + update.version);
        } finally {
            delete(tempDir);
        }
    }

    private static void download(String url, File target) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "MixClient/1.0");
        int code = connection.getResponseCode();
        if (code >= 400) {
            throw new IllegalStateException("HTTP " + code + " for " + url);
        }
        try (InputStream input = connection.getInputStream();
             OutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    private static JsonObject readJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "MixClient/1.0");
        int code = connection.getResponseCode();
        if (code >= 400) {
            throw new IllegalStateException("HTTP " + code + " for " + url);
        }
        StringBuilder builder = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return new JsonParser().parse(builder.toString()).getAsJsonObject();
    }

    private static String sha(File file, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (byte value : digest.digest()) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private static void validatePath(String path) {
        if (path == null || path.isEmpty() || path.contains("..")
                || path.startsWith("/") || path.startsWith("\\")
                || path.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Invalid update path: " + path);
        }
    }

    private static void validateUrl(String url) {
        if (url == null || (!url.startsWith("https://") && !url.startsWith("http://127.0.0.1"))) {
            throw new IllegalArgumentException("Update URL must be HTTPS: " + url);
        }
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        if (!file.delete()) {
            System.out.println("[Updater] Could not delete " + file);
        }
    }
}
