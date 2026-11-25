package mix.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class GameLauncher {
    public static final String MC_VERSION = "1.8.9";
    /* kept in sync with launcher-desktop/electron/forge.js — the profile the
       launcher installs and the id this class looks up have to match */
    public static final String FORGE_ID = "1.8.9-forge1.8.9-11.15.1.2318-1.8.9";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class LaunchConfig {
        public String username = "Player";
        public int ramMb = 2048;
        /** Window size; the interface scales itself to whatever this is. */
        public int width = 1600;
        public int height = 900;
        public boolean smokeTest;
        public String server;
        public int port = 25565;
        public File gameDir;
        public File nativesDir;
        public String logName;
        public boolean optifine;
        /** Run FML: the only mode in which `mods/` is scanned at all. */
        public boolean forge;
        public boolean fpsBoost;
        public String accountMode = "offline";
        public String msClientId;
        public String msAccessToken;
        public String msRefreshToken;
        public long msExpiresAt;
        public String msUuid;
        public String msName;
        public String updateUrl;
        public boolean autoUpdate;
        public String accessToken;
        public String uuid;
        public String userType = "legacy";
    }

    private GameLauncher() {
    }

    public static int launch(LaunchConfig config, Consumer<String> log) {
        try {
            return launchInternal(config, log);
        } catch (Exception exception) {
            log.accept("Launch failed: " + exception.getMessage());
            exception.printStackTrace();
            return -1;
        }
    }

    private static int launchInternal(LaunchConfig config, Consumer<String> log) throws Exception {
        File minecraftDir = findMinecraftDir();
        File versionDir = new File(minecraftDir, "versions/" + MC_VERSION);
        File versionJson = new File(versionDir, MC_VERSION + ".json");
        File clientJar = new File(versionDir, MC_VERSION + ".jar");
        File assetsDir = new File(minecraftDir, "assets");

        if (!versionJson.isFile()) {
            throw new IllegalStateException("Missing " + versionJson + " (install 1.8.9 once with any launcher)");
        }
        if (!clientJar.isFile()) {
            throw new IllegalStateException("Missing " + clientJar);
        }

        File distDir = findDistDir();
        File clientJarDist = new File(distDir, "MixClient.jar");
        File deobfClientJar = new File(distDir, "minecraft-1.8.9-mcp.jar");
        File notchClientJar = new File(distDir, "MixClient-notch.jar");
        File libDir = new File(distDir, "lib");
        if (!clientJarDist.isFile()) {
            throw new IllegalStateException("Missing client jar: " + clientJarDist + " (run gradlew dist)");
        }
        if (!deobfClientJar.isFile()) {
            throw new IllegalStateException("Missing MCP client jar: " + deobfClientJar + " (run gradlew dist)");
        }
        if (config.optifine && !notchClientJar.isFile()) {
            throw new IllegalStateException("Missing notch client jar: " + notchClientJar + " (run gradlew dist)");
        }

        File dataDir = new File(findAppData(), ".mixclient");
        File nativesDir = config.nativesDir != null ? config.nativesDir : new File(dataDir, "natives");
        File logsDir = new File(dataDir, "logs");
        /* Mix Client plays in its own game folder — like the other clients do —
           so its mods, config and saves are not mixed with a vanilla or Forge
           install sitting in .minecraft. Only the shared files (client jar,
           libraries, assets) stay where every launcher expects them. */
        File gameDir = config.gameDir != null ? config.gameDir : new File(dataDir, "game");
        if (config.smokeTest) {
            gameDir = new File(dataDir, "smoke");
        }
        if (!gameDir.exists() && !gameDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + gameDir);
        }
        File modsFolder = new File(gameDir, "mods");
        if (!modsFolder.exists() && !modsFolder.mkdirs()) {
            log.accept("Could not create " + modsFolder);
        }
        if (!config.smokeTest) {
            migrateGameData(minecraftDir, gameDir, log);
        }
        if (!nativesDir.exists() && !nativesDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + nativesDir);
        }
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + logsDir);
        }

        JsonObject root;
        try (FileReader reader = new FileReader(versionJson)) {
            root = new JsonParser().parse(reader).getAsJsonObject();
        }

        String assetIndex = root.has("assetIndex") ? root.getAsJsonObject("assetIndex").get("id").getAsString() : "1.8";
        File librariesDir = new File(minecraftDir, "libraries");
        List<File> libraryRoots = new ArrayList<>();
        libraryRoots.add(librariesDir);
        libraryRoots.add(new File(findAppData(), ".tlauncher/legacy/Minecraft/game/libraries"));
        libraryRoots.add(new File(minecraftDir, "versions/" + MC_VERSION + "/libraries"));

        List<File> minecraftLibraries = new ArrayList<>();
        List<File> nativeJars = new ArrayList<>();
        if (root.has("libraries")) {
            JsonArray libraries = root.getAsJsonArray("libraries");
            for (JsonElement element : libraries) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject library = element.getAsJsonObject();
                if (!isAllowed(library)) {
                    continue;
                }
                String name = library.get("name").getAsString();
                if (library.has("natives")) {
                    JsonObject natives = library.getAsJsonObject("natives");
                    if (natives.has("windows")) {
                        String classifier = natives.get("windows").getAsString()
                                .replace("${arch}", System.getProperty("os.arch").contains("64") ? "64" : "32");
                        File nativeJar = resolveLibrary(libraryRoots, name, classifier);
                        if (nativeJar != null) {
                            nativeJars.add(nativeJar);
                        } else {
                            log.accept("Missing native: " + name + ":" + classifier);
                        }
                    }
                } else {
                    File jar = resolveLibrary(libraryRoots, name, null);
                    if (jar != null) {
                        minecraftLibraries.add(jar);
                    } else {
                        log.accept("Missing library: " + name);
                    }
                }
            }
        }

        for (File nativeJar : nativeJars) {
            extractNatives(nativeJar, nativesDir, log);
        }
        /* The Mix UI draws through lwjgl3 + NanoVG, which ships its own native
           set. It is a plain client library, not a version-JSON entry with a
           natives classifier, so pull it out here. lwjgl3 packs its libraries
           under <os>/<arch>/org/lwjgl/... while java.library.path points at the
           root, so they are flattened on the way out. */
        File lwjgl3Natives = new File(distDir, "lib/lwjgl-mix-natives.jar");
        File lwjgl3NativesDir = null;
        if (lwjgl3Natives.isFile()) {
            /* lwjgl3's native is also called lwjgl.dll, which is the name lwjgl2
               64-bit... no: lwjgl2 uses lwjgl64.dll on 64-bit, but the two
               builds still ship several identically named libraries. Keep them in
               separate directories so neither load path can pick up the other's
               binary, and put both on java.library.path. */
            File lwjgl3Dir = new File(nativesDir, "lwjgl3");
            extractNativesFlattened(lwjgl3Natives, lwjgl3Dir, log);
            lwjgl3NativesDir = lwjgl3Dir;
        }

        File optifineJar = null;
        File optifineLaunchwrapper = null;
        String optifineVersion = MC_VERSION;
        /* Forge replaces the bundled OptiFine launch profile: under FML,
           OptiFine has to be a normal mod in the mods folder. */
        if (config.forge && config.optifine) {
            log.accept("Forge mode: OptiFine is read from mods/ instead of the bundled profile");
            config.optifine = false;
        }
        List<File> forgeLibraries = new ArrayList<>();
        if (config.forge) {
            File forgeVersionDir = new File(minecraftDir, "versions/" + FORGE_ID);
            File forgeJson = new File(forgeVersionDir, FORGE_ID + ".json");
            if (!forgeJson.isFile()) {
                throw new IllegalStateException("Forge profile missing: " + forgeJson
                        + " (turn Forge on once in the launcher so it can install)");
            }
            JsonObject forgeRoot;
            try (FileReader reader = new FileReader(forgeJson)) {
                forgeRoot = new JsonParser().parse(reader).getAsJsonObject();
            }
            if (forgeRoot.has("libraries")) {
                for (JsonElement element : forgeRoot.getAsJsonArray("libraries")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject library = element.getAsJsonObject();
                    if (library.has("natives") || !isAllowed(library)) {
                        continue;
                    }
                    String name = library.get("name").getAsString();
                    File jar = resolveLibrary(libraryRoots, name, null);
                    if (jar != null) {
                        forgeLibraries.add(jar);
                    } else {
                        log.accept("Missing Forge library: " + name);
                    }
                }
            }
            if (forgeLibraries.isEmpty()) {
                throw new IllegalStateException("Forge libraries are missing from " + librariesDir
                        + " (turn Forge off and on again in the launcher)");
            }
            File[] modFiles = modsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            log.accept("Forge: " + FORGE_ID + " (" + forgeLibraries.size() + " libraries, "
                    + (modFiles == null ? 0 : modFiles.length) + " mods in " + modsFolder + ")");
            /* The Forge loader carries OptiFine as a mod, so say which copy is
               in play — a missing one means shaders and the extra render
               options silently fall back to vanilla. */
            File[] optifineMods = modsFolder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.startsWith("optifine") && lower.endsWith(".jar");
            });
            if (optifineMods != null && optifineMods.length > 0) {
                log.accept("OptiFine mod: " + optifineMods[0].getName());
            } else {
                log.accept("OptiFine mod: none in mods/ (Forge runs without shaders)");
            }
        }
        if (config.optifine) {
            /* The build carries its own copy of OptiFine and of the launch wrapper
               it needs, so the client no longer depends on an OptiFine profile
               installed in .minecraft. The versions folder is only a fallback for
               a build made before the jars were bundled. */
            File bundledDir = new File(distDir, "optifine");
            File[] bundled = bundledDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (bundled != null) {
                Arrays.sort(bundled, Comparator.comparing(File::getName));
                for (File jar : bundled) {
                    String lower = jar.getName().toLowerCase();
                    if (lower.contains("launchwrapper")) {
                        optifineLaunchwrapper = jar;
                    } else if (lower.startsWith("optifine")) {
                        optifineJar = jar;
                    }
                }
            }
            if (optifineJar != null && optifineLaunchwrapper != null) {
                log.accept("OptiFine: bundled " + optifineJar.getName());
            }
        }
        if (config.optifine && (optifineJar == null || optifineLaunchwrapper == null)) {
            File versionsDir = new File(minecraftDir, "versions");
            File[] versionDirs = versionsDir.listFiles(File::isDirectory);
            if (versionDirs != null) {
                Arrays.sort(versionDirs, Comparator.comparing(File::getName));
                for (File optifineDir : versionDirs) {
                    String id = optifineDir.getName();
                    if (!id.startsWith("1.8.9-OptiFine")) {
                        continue;
                    }
                    File json = new File(optifineDir, id + ".json");
                    if (!json.isFile()) {
                        continue;
                    }
                    try (FileReader reader = new FileReader(json)) {
                        JsonElement parsed = new JsonParser().parse(reader);
                        if (!parsed.isJsonObject()) {
                            continue;
                        }
                        JsonObject version = parsed.getAsJsonObject();
                        if (version.has("libraries")) {
                            for (JsonElement element : version.getAsJsonArray("libraries")) {
                                String name = element.getAsJsonObject().get("name").getAsString();
                                File jar = resolveLibrary(libraryRoots, name, null);
                                if (jar == null) {
                                    continue;
                                }
                                if (name.startsWith("optifine:OptiFine")) {
                                    optifineJar = jar;
                                } else if (name.startsWith("optifine:launchwrapper-of")) {
                                    optifineLaunchwrapper = jar;
                                }
                            }
                        }
                        if (optifineJar != null && optifineLaunchwrapper != null) {
                            optifineVersion = id;
                            break;
                        }
                    }
                }
            }
            if (optifineJar == null || optifineLaunchwrapper == null) {
                throw new IllegalStateException("OptiFine not found: expected the bundled jars in "
                        + new File(distDir, "optifine") + " (run gradlew dist) or an OptiFine HD U M5 install in "
                        + new File(minecraftDir, "versions"));
            }
            log.accept("OptiFine: " + optifineJar.getAbsolutePath());
            log.accept("OptiFine launchwrapper: " + optifineLaunchwrapper.getName());
        }

        List<File> clientLibs = new ArrayList<>();
        File[] libs = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (libs != null) {
            Arrays.sort(libs, Comparator.comparing(File::getName));
            List<File> ordered = new ArrayList<>();
            addFirst(ordered, libs, "launchwrapper");
            addFirst(ordered, libs, "mixin");
            addFirst(ordered, libs, "asm");
            for (File file : libs) {
                if (!ordered.contains(file)) {
                    ordered.add(file);
                }
            }
            clientLibs.addAll(ordered);
        }
        if (clientLibs.isEmpty()) {
            throw new IllegalStateException("No launch libraries found in " + libDir);
        }

        List<File> classpath = new ArrayList<>();
        if (config.forge) {
            /* Forge deobfuscates vanilla Minecraft to SRG, so it needs the copy of
               the client whose mixins carry func_* member names. */
            File forgeClientJar = new File(distDir, "MixClient-srg.jar");
            if (!forgeClientJar.isFile()) {
                throw new IllegalStateException("Missing SRG client jar: " + forgeClientJar + " (run gradlew dist)");
            }
            File mixinJar = null;
            File[] distLibs = libDir.listFiles((dir, name) -> name.toLowerCase().contains("mixin") && name.endsWith(".jar"));
            if (distLibs != null && distLibs.length > 0) {
                mixinJar = distLibs[0];
            }
            if (mixinJar == null) {
                throw new IllegalStateException("Missing Mixin library in " + libDir);
            }
            classpath.add(forgeClientJar);
            classpath.add(mixinJar);
            /* The Mix UI ships its own runtime set in lib/ -- lwjgl3 + NanoVG,
               the music/Dango libraries, JavaFX, JCEF, joml, caffeine. Only the
               mixin jar was being added here, so every one of those was missing
               and the UI died on its first NanoVG or JavaFX call. ASM is skipped:
               Forge needs its own set, see below. */
            for (File file : clientLibs) {
                if (file.getName().toLowerCase().startsWith("asm")) {
                    continue;
                }
                File entry = file;
                /* Dango is built with MCP member names, which Forge's runtime
                   (deobfuscated to SRG) cannot link: Minecraft.getMinecraft()
                   threw NoSuchMethodError and the music module never started.
                   dist ships the matching func_/field_ copy for this mode. */
                if ("dango-mix.jar".equalsIgnoreCase(file.getName())) {
                    entry = new File(distDir, "dango-mix-srg.jar");
                    if (!entry.isFile()) {
                        throw new IllegalStateException("Missing SRG Dango jar: " + entry + " (run gradlew dist)");
                    }
                }
                if (!classpath.contains(entry)) {
                    classpath.add(entry);
                }
            }
            /* FML's deobfuscation transformer is built against the asm-all that
               ships with Forge. The client's ASM 7 copy would replace it on the
               classpath and FML needs RemappingClassAdapter, which ASM 7 dropped
               — so Forge mode uses the Forge set and nothing else. */
            classpath.addAll(forgeLibraries);
            classpath.add(clientJar);
            log.accept("Client: " + forgeClientJar.getName() + " (searge, Forge " + FORGE_ID + ")");
        } else if (config.optifine) {
            File mixinJar = null;
            File[] distLibs = libDir.listFiles((dir, name) -> name.toLowerCase().contains("mixin") && name.endsWith(".jar"));
            if (distLibs != null && distLibs.length > 0) {
                mixinJar = distLibs[0];
            }
            if (mixinJar == null) {
                throw new IllegalStateException("Missing Mixin library in " + libDir);
            }
            classpath.add(notchClientJar);
            classpath.add(mixinJar);
            /* MixUI's LWJGL transformer is ASM-based, and this branch does not
               take the full lib folder, so keep the ASM runtime beside it. */
            File[] asmLibs = libDir.listFiles((dir, name) -> name.toLowerCase().contains("asm") && name.endsWith(".jar"));
            if (asmLibs != null) {
                Arrays.sort(asmLibs, Comparator.comparing(File::getName));
                classpath.addAll(Arrays.asList(asmLibs));
            }
            /* ...and the rest of the Mix UI runtime (lwjgl3 + NanoVG, JavaFX,
               JCEF, Dango, joml, caffeine) is needed just the same */
            for (File file : clientLibs) {
                if (file.getName().toLowerCase().contains("asm")) {
                    continue;
                }
                if (!classpath.contains(file)) {
                    classpath.add(file);
                }
            }
            classpath.add(optifineJar);
            classpath.add(optifineLaunchwrapper);
            classpath.addAll(minecraftLibraries);
            classpath.add(clientJar);
            log.accept("Client: " + notchClientJar.getName() + " (notch, OptiFine profile)");
        } else {
            classpath.add(clientJarDist);
            classpath.addAll(clientLibs);
            classpath.addAll(minecraftLibraries);
            classpath.add(deobfClientJar);
        }

        StringBuilder classpathBuilder = new StringBuilder();
        for (File file : classpath) {
            if (classpathBuilder.length() > 0) {
                classpathBuilder.append(File.pathSeparatorChar);
            }
            classpathBuilder.append(file.getAbsolutePath());
        }

        String javaExecutable = findJava8();
        log.accept("Java: " + javaExecutable);
        log.accept("Client: " + clientJarDist.getAbsolutePath());
        log.accept("Libraries: " + clientLibs.size() + " client, " + minecraftLibraries.size() + " minecraft");

        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + config.username).getBytes(StandardCharsets.UTF_8)).toString();
        String sessionUuid = config.uuid != null && !config.uuid.isEmpty() ? config.uuid : offlineUuid;
        String sessionToken = config.accessToken != null && !config.accessToken.isEmpty() ? config.accessToken : "0";
        String userType = config.userType != null && !config.userType.isEmpty() ? config.userType : "legacy";

        List<String> gameArgs = new ArrayList<>();
        gameArgs.add("--username");
        gameArgs.add(config.username);
        gameArgs.add("--version");
        gameArgs.add(config.forge ? FORGE_ID : config.optifine ? optifineVersion : MC_VERSION);
        gameArgs.add("--gameDir");
        gameArgs.add(gameDir.getAbsolutePath());
        gameArgs.add("--assetsDir");
        gameArgs.add(assetsDir.getAbsolutePath());
        gameArgs.add("--assetIndex");
        gameArgs.add(assetIndex);
        gameArgs.add("--width");
        gameArgs.add(config.width > 0 ? String.valueOf(config.width) : "1280");
        gameArgs.add("--height");
        gameArgs.add(config.height > 0 ? String.valueOf(config.height) : "720");
        gameArgs.add("--uuid");
        gameArgs.add(sessionUuid);
        gameArgs.add("--accessToken");
        gameArgs.add(sessionToken);
        gameArgs.add("--userProperties");
        gameArgs.add("{}");
        gameArgs.add("--userType");
        gameArgs.add(userType);
        if (config.server != null && !config.server.isEmpty()) {
            gameArgs.add("--server");
            gameArgs.add(config.server);
            gameArgs.add("--port");
            gameArgs.add(String.valueOf(config.port));
        }

        StringBuilder joined = new StringBuilder();
        for (String arg : gameArgs) {
            if (joined.length() > 0) {
                joined.append('\u0000');
            }
            joined.append(arg);
        }
        String encodedArgs = java.util.Base64.getEncoder().encodeToString(joined.toString().getBytes(StandardCharsets.UTF_8));

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-Xmx" + config.ramMb + "M");
        command.add("-Xms512M");
        command.add("-Dfile.encoding=UTF-8");
        /* lwjgl3's directory goes first: both builds ship a library named
           lwjgl.dll, and lwjgl3 resolves its own by that name. lwjgl2 on 64-bit
           asks for lwjgl64.dll, so it is unaffected by the order. */
        StringBuilder libraryPath = new StringBuilder();
        if (lwjgl3NativesDir != null) {
            libraryPath.append(lwjgl3NativesDir.getAbsolutePath());
        }
        if (libraryPath.length() > 0) {
            libraryPath.append(File.pathSeparator);
        }
        libraryPath.append(nativesDir.getAbsolutePath());
        command.add("-Djava.library.path=" + libraryPath);
        if (!config.optifine && !config.forge) {
            command.add("-Dmixclient.gameArgs=" + encodedArgs);
        }
        if (config.optifine) {
            command.add("-Dmixclient.optifine=true");
        }
        if (config.forge) {
            /* FML owns the argument list and the class loader; the tweaker only
               has to bring Mixin up in the searge namespace FML renames into */
            command.add("-Dmixclient.forge=true");
            command.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
            command.add("-Dfml.ignorePatchDiscrepancies=true");
        }
        if (config.smokeTest) {
            command.add("-Dmixclient.uicapture=true");
            command.add("-Dmixclient.uicapture.world=true");
        }
        if (Boolean.getBoolean("mixclient.uicapture.fullscreen")) {
            command.add("-Dmixclient.uicapture.fullscreen=true");
        }
        if (Boolean.getBoolean("mixclient.uicapture.target")) {
            command.add("-Dmixclient.uicapture.target=true");
        }
        if (Boolean.getBoolean("mixclient.uicapture.frames")) {
            command.add("-Dmixclient.uicapture.frames=true");
        }
        if (Boolean.getBoolean("mixclient.uicapture.visuals")) {
            command.add("-Dmixclient.uicapture.visuals=true");
        }
        if (Boolean.getBoolean("mixclient.dango.diag")) {
            command.add("-Dmixclient.dango.diag=true");
        }
        command.add("-cp");
        command.add(classpathBuilder.toString());
        command.add("net.minecraft.launchwrapper.Launch");
        /* Tweak classes run in the order they are named here, and Mixin has to
           be up before a loader starts loading and transforming game classes:
           when OptiFine went first the subsystem booted after pre-init and
           logged "Some mixins may be skipped", which silently dropped our
           render hooks. Launch wrapper concatenates the arguments every
           tweaker returns, so MixTweaker can go first without stealing the
           game arguments from the loader. Forge stays first: FML owns the
           launch target. */
        if (config.forge) {
            command.add("--tweakClass");
            command.add("net.minecraftforge.fml.common.launcher.FMLTweaker");
        }
        if (config.optifine) {
            command.add("--tweakClass");
            command.add("mix.client.bootstrap.MixTweaker");
            command.add("--tweakClass");
            command.add("optifine.OptiFineTweaker");
        } else {
            command.add("--tweakClass");
            command.add("mix.client.bootstrap.MixTweaker");
        }
        if (config.optifine || config.forge) {
            command.addAll(gameArgs);
        }

        File logFile = new File(logsDir, config.logName != null ? config.logName : "latest.log");
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        try (Writer writer = new FileWriter(logFile, true)) {
            writer.write("\n===== Mix Client launch " + timestamp + " =====\n");
        }

        if (config.fpsBoost) {
            applyFpsOptions(gameDir, log);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(gameDir);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

        log.accept("Starting game...");
        Process process = builder.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            log.accept("Game closed normally.");
        } else {
            log.accept("Game exited with code " + exitCode + " — see " + logFile.getAbsolutePath());
        }

        if (config.smokeTest) {
            dumpLogTail(logFile, log, 40);
            if (exitCode == 0 && !containsLineSinceLastLaunch(logFile, "[MixClient] MIXCLIENT_SMOKE_OK")) {
                log.accept("Smoke test exited before MIXCLIENT_SMOKE_OK.");
                return 1;
            }
        }
        return exitCode;
    }

    private static boolean containsLineSinceLastLaunch(File file, String marker) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("===== Mix Client launch ")) {
                    found = false;
                }
                if (line.contains(marker)) {
                    found = true;
                }
            }
            return found;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void addFirst(List<File> target, File[] libs, String keyword) {
        for (File file : libs) {
            if (file.getName().toLowerCase().contains(keyword) && !target.contains(file)) {
                target.add(file);
            }
        }
    }

    /**
     * Extracts native libraries straight into {@code destination}, dropping the
     * {@code <os>/<arch>/...} prefix lwjgl3 uses, so that
     * {@code -Djava.library.path=destination} can find them.
     */
    private static void extractNativesFlattened(File jar, File destination, Consumer<String> log) throws Exception {
        if (!destination.isDirectory() && !destination.mkdirs() && !destination.isDirectory()) {
            throw new IOException("Could not create " + destination);
        }
        int extracted = 0;
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                String name = entry.getName();
                if (!(name.endsWith(".dll") || name.endsWith(".so")
                        || name.endsWith(".jnilib") || name.endsWith(".dylib"))) {
                    continue;
                }
                File target = new File(destination, new File(name).getName());
                try (InputStream input = zip.getInputStream(entry);
                     FileOutputStream output = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                extracted++;
            }
        }
        log.accept("Natives: " + jar.getName() + " (" + extracted + " libraries)");
    }

    private static void extractNatives(File jar, File destination, Consumer<String> log) throws Exception {
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                File target = new File(destination, entry.getName());
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    continue;
                }
                try (InputStream input = zip.getInputStream(entry);
                     FileOutputStream output = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
        log.accept("Natives: " + jar.getName());
    }

    private static File resolveLibrary(List<File> roots, String name, String classifier) {
        String[] parts = name.split(":");
        if (parts.length < 3) {
            return null;
        }
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String extra = classifier != null ? classifier : (parts.length > 3 ? parts[3] : null);
        String fileName = artifact + "-" + version + (extra != null ? "-" + extra : "") + ".jar";
        for (File root : roots) {
            if (root == null) {
                continue;
            }
            File file = new File(root, group + "/" + artifact + "/" + version + "/" + fileName);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private static boolean isAllowed(JsonObject library) {
        if (!library.has("rules")) {
            return true;
        }
        JsonArray rules = library.getAsJsonArray("rules");
        boolean allowed = false;
        String osName = System.getProperty("os.name").toLowerCase();
        String os = osName.contains("win") ? "windows" : osName.contains("mac") ? "osx" : "linux";
        for (JsonElement element : rules) {
            JsonObject rule = element.getAsJsonObject();
            boolean applies = true;
            if (rule.has("os")) {
                JsonObject osRule = rule.getAsJsonObject("os");
                if (osRule.has("name") && !os.equals(osRule.get("name").getAsString())) {
                    applies = false;
                }
                if (osRule.has("arch") && !System.getProperty("os.arch").contains(osRule.get("arch").getAsString())) {
                    applies = false;
                }
                if (osRule.has("version")) {
                    String pattern = osRule.get("version").getAsString();
                    if (!System.getProperty("os.version").matches(pattern)) {
                        applies = false;
                    }
                }
            }
            if (applies) {
                allowed = "allow".equals(rule.get("action").getAsString());
            }
        }
        return allowed;
    }

    /* One-time move of options out of the shared .minecraft. Saves and resource
       packs stay where they are to avoid copying gigabytes during launch. */
    private static void migrateGameData(File minecraftDir, File gameDir, Consumer<String> log) {
        log.accept("Game folder: " + gameDir.getAbsolutePath() + " (mods, config, saves)");
        File marker = new File(gameDir, ".migrated");
        if (marker.isFile()) {
            return;
        }
        int copied = 0;
        File oldOptions = new File(minecraftDir, "options.txt");
        File newOptions = new File(gameDir, "options.txt");
        if (oldOptions.isFile() && !newOptions.isFile()) {
            try {
                Files.copy(oldOptions.toPath(), newOptions.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                copied += 1;
            } catch (Exception ignored) {
            }
        }
        try {
            marker.createNewFile();
        } catch (Exception ignored) {
        }
        if (copied > 0) {
            log.accept("Moved " + copied + " setting file(s) out of " + minecraftDir.getAbsolutePath());
        }
    }

    private static void applyFpsOptions(File gameDir, Consumer<String> log) {
        java.util.Map<String, String> values = readOptions(gameDir);
        values.put("renderDistance", "8");
        values.put("fancyGraphics", "false");
        values.put("ambientOcclusion", "0");
        values.put("particles", "2");
        values.put("clouds", "false");
        values.put("mipmapLevels", "0");
        values.put("ofFastMath", "true");
        values.put("ofSmoothFps", "true");
        values.put("ofSmoothWorld", "true");
        values.put("ofLagometer", "false");
        values.put("ofClouds", "1");
        values.put("ofTrees", "0");
        values.put("ofCustomSky", "false");
        values.put("ofConnectedTextures", "0");
        values.put("ofDynamicLights", "0");
        values.put("ofLoadChunkUpdates", "1");
        if (writeOptions(gameDir, values)) {
            log.accept("FPS pack: options.txt optimised");
        }
    }

    private static java.util.Map<String, String> readOptions(File gameDir) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        File options = new File(gameDir, "options.txt");
        if (options.isFile()) {
            try {
                for (String line : java.nio.file.Files.readAllLines(options.toPath())) {
                    int split = line.indexOf(':');
                    if (split > 0) {
                        values.put(line.substring(0, split), line.substring(split + 1));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return values;
    }

    private static boolean writeOptions(File gameDir, java.util.Map<String, String> values) {
        try {
            File options = new File(gameDir, "options.txt");
            StringBuilder builder = new StringBuilder();
            for (java.util.Map.Entry<String, String> entry : values.entrySet()) {
                builder.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
            }
            java.nio.file.Files.write(options.toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static File findDistDir() throws Exception {
        String override = System.getProperty("mixclient.dist");
        if (override != null) {
            return new File(override);
        }
        File source = new File(GameLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File dir = source.isDirectory() ? source : source.getParentFile();
        if (dir != null) {
            if (new File(dir, "MixClient.jar").isFile() || new File(dir, "lib").isDirectory()) {
                return dir;
            }
        }
        return new File("dist").getAbsoluteFile();
    }

    public static File findMinecraftDir() {
        File appdata = new File(findAppData(), ".minecraft");
        if (appdata.isDirectory()) {
            return appdata;
        }
        return new File(System.getProperty("user.home"), ".minecraft");
    }

    private static String findAppData() {
        String appdata = System.getenv("APPDATA");
        return appdata != null ? appdata : System.getProperty("user.home");
    }

    public static File findDataDir() {
        return new File(findAppData(), ".mixclient");
    }

    public static String findJava8() {
        /* the launcher passes the runtime it started with, which is the one bundled
           with the installer on a machine that never had a JDK */
        String override = System.getenv("MIXCLIENT_JAVA");
        if (override != null && !override.isEmpty() && new File(override).isFile()) {
            return override;
        }
        String current = System.getProperty("java.home");
        if (isJava8(current)) {
            return javaExecutable(current);
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && isJava8(javaHome)) {
            return javaExecutable(javaHome);
        }
        File[] roots = {
                new File("C:/Program Files/Java"),
                new File("C:/Program Files/Eclipse Adoptium"),
                new File("C:/Program Files/AdoptOpenJDK"),
                new File("C:/Program Files (x86)/Java")
        };
        for (File root : roots) {
            File[] children = root.listFiles(File::isDirectory);
            if (children == null) {
                continue;
            }
            Arrays.sort(children, Comparator.comparing(File::getName).reversed());
            for (File child : children) {
                if (isJava8(child.getAbsolutePath())) {
                    return javaExecutable(child.getAbsolutePath());
                }
            }
        }
        return javaExecutable(current);
    }

    private static String javaExecutable(String home) {
        File javaw = new File(home, "bin/javaw.exe");
        if (javaw.isFile()) {
            return javaw.getAbsolutePath();
        }
        File java = new File(home, "bin/java.exe");
        if (java.isFile()) {
            return java.getAbsolutePath();
        }
        return new File(home, "bin/java").getAbsolutePath();
    }

    private static boolean isJava8(String home) {
        File release = new File(home, "release");
        if (release.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(release))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("JAVA_VERSION=") && line.contains("\"1.8")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return home.contains("jdk-8") || home.contains("jdk8") || home.contains("1.8");
    }

    private static void dumpLogTail(File file, Consumer<String> log, int lines) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> all = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                all.add(line);
            }
            log.accept("----- log tail -----");
            for (int i = Math.max(0, all.size() - lines); i < all.size(); i++) {
                log.accept(all.get(i));
            }
        } catch (Exception exception) {
            log.accept("Could not read log: " + exception.getMessage());
        }
    }

    public static LaunchConfig loadConfig(File dataDir) {
        LaunchConfig config = new LaunchConfig();
        File file = new File(dataDir, "launcher.json");
        if (file.isFile()) {
            try (FileReader reader = new FileReader(file)) {
                LaunchConfig loaded = GSON.fromJson(reader, LaunchConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (Exception ignored) {
            }
        }
        return config;
    }

    public static void saveConfig(File dataDir, LaunchConfig config) {
        try {
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                return;
            }
            try (Writer writer = new FileWriter(new File(dataDir, "launcher.json"))) {
                GSON.toJson(config, writer);
            }
        } catch (Exception ignored) {
        }
    }
}
