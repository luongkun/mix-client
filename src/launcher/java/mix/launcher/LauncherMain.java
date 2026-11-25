package mix.launcher;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public final class LauncherMain {
    public static void main(String[] args) {
        boolean smoke = false;
        boolean optifineSmoke = false;
        boolean forgeSmoke = false;
        boolean updateTest = false;
        String updateUrl = null;
        String uiShot = null;
        String launchFile = null;
        for (int i = 0; i < args.length; i++) {
            if ("--smoke-test".equalsIgnoreCase(args[i])) {
                smoke = true;
            } else if ("--optifine-smoke".equalsIgnoreCase(args[i])) {
                optifineSmoke = true;
            } else if ("--forge-smoke".equalsIgnoreCase(args[i])) {
                forgeSmoke = true;
            } else if ("--update-test".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                updateTest = true;
                updateUrl = args[++i];
            } else if ("--ui-shot".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                uiShot = args[++i];
            } else if ("--launch-file".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                launchFile = args[++i];
            }
        }

        if (updateTest) {
            try {
                java.io.File dist = GameLauncher.findDistDir();
                Updater.Update update = Updater.check(updateUrl, dist);
                String local = Updater.localVersion(dist);
                System.out.println("local=" + local + " remote=" + update.version
                        + " newer=" + Updater.isNewer(local, update.version));
                Updater.apply(update, dist, System.out::println);
                System.out.println("UPDATER_OK");
                System.exit(0);
            } catch (Exception exception) {
                exception.printStackTrace();
                System.exit(1);
            }
        }

        if (optifineSmoke) {
            GameLauncher.LaunchConfig config = new GameLauncher.LaunchConfig();
            config.username = "SmokeFine";
            config.ramMb = 2048;
            config.smokeTest = true;
            /* a smoke run must not fight the live game for .mixclient/natives:
               windows keeps the dlls mapped while the game is open */
            config.nativesDir = new java.io.File(GameLauncher.findDataDir(), "smoke/natives");
            config.optifine = true;
            config.fpsBoost = true;
            config.logName = "optifine-smoke.log";
            int code = GameLauncher.launch(config, System.out::println);
            System.exit(code == 0 ? 0 : 1);
        }

        if (forgeSmoke) {
            GameLauncher.LaunchConfig config = new GameLauncher.LaunchConfig();
            config.username = "SmokeForge";
            config.ramMb = 2048;
            config.smokeTest = true;
            config.nativesDir = new java.io.File(GameLauncher.findDataDir(), "smoke/natives");
            config.forge = true;
            config.optifine = false;
            config.fpsBoost = true;
            config.logName = "forge-smoke.log";
            int code = GameLauncher.launch(config, System.out::println);
            System.exit(code == 0 ? 0 : 1);
        }

        if (launchFile != null) {
            try (java.io.Reader reader = new java.io.InputStreamReader(
                    new java.io.FileInputStream(launchFile), java.nio.charset.StandardCharsets.UTF_8)) {
                GameLauncher.LaunchConfig config = new com.google.gson.Gson()
                        .fromJson(reader, GameLauncher.LaunchConfig.class);
                System.out.println("[MixClient] launching from " + launchFile);
                int code = GameLauncher.launch(config, System.out::println);
                System.exit(code == 0 ? 0 : 1);
            } catch (Exception exception) {
                exception.printStackTrace();
                System.exit(1);
            }
        }

        if (smoke) {
            GameLauncher.LaunchConfig config = new GameLauncher.LaunchConfig();
            config.username = "SmokeTest";
            config.ramMb = 2048;
            config.smokeTest = true;
            config.nativesDir = new java.io.File(GameLauncher.findDataDir(), "smoke/natives");
            int code = GameLauncher.launch(config, System.out::println);
            System.exit(code == 0 ? 0 : 1);
        }

        final String shotPath = uiShot;
        SwingUtilities.invokeLater(() -> {
            try {
                LauncherFrame frame = new LauncherFrame();
                frame.setVisible(true);
                if (shotPath != null) {
                    Timer timer = new Timer(1200, event -> {
                        try {
                            BufferedImage image = new BufferedImage(frame.getContentPane().getWidth(),
                                    frame.getContentPane().getHeight(), BufferedImage.TYPE_INT_ARGB);
                            Graphics2D graphics = image.createGraphics();
                            frame.getContentPane().paint(graphics);
                            graphics.dispose();
                            ImageIO.write(image, "png", new File(shotPath));
                            System.out.println("UI screenshot saved: " + shotPath);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        System.exit(0);
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }
}
