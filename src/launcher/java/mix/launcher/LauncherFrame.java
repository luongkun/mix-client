package mix.launcher;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Launcher shell matching Badlion Client 4.5.13.
 *
 * Metrics and colours are the ones measured from the shipping Electron UI:
 * 200px sidebar (#161616), #141414 canvas, 25px-radius card, #2B2B2B header pill,
 * #1F1F1F inputs and the #1DC3FF accent.
 */
public final class LauncherFrame extends JFrame {
    private static final Color CANVAS = new Color(0x141414);
    private static final Color SIDEBAR = new Color(0x161616);
    private static final Color RAISED = new Color(0x2B2B2B);
    private static final Color INPUT = new Color(0x1F1F1F);
    private static final Color INPUT_HOVER = new Color(0x262626);
    private static final Color BORDER = new Color(0x40, 0x40, 0x40, 0x80);
    private static final Color BORDER_SOFT = new Color(0x2A2A2A);
    private static final Color ACCENT = new Color(0x1DC3FF);
    private static final Color ACCENT_HOVER = new Color(0x4FD2FF);
    private static final Color TEXT = new Color(0xEEEEEE);
    private static final Color MUTED = new Color(0x8A8A8A);
    private static final Color DIM = new Color(0x5E5E5E);
    private static final Color SUCCESS = new Color(0x3DD68C);

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 600;
    private static final int SIDEBAR_WIDTH = 200;
    private static final int CARD_WIDTH = 500;
    private static final int CARD_X = SIDEBAR_WIDTH + (WINDOW_WIDTH - SIDEBAR_WIDTH - CARD_WIDTH) / 2;
    private static final int CARD_Y = 60;
    private static final int CARD_HEIGHT = 480;
    private static final int INNER_X = CARD_X + 24;
    private static final int INNER_W = CARD_WIDTH - 48;

    private final GameLauncher.LaunchConfig config = GameLauncher.loadConfig(GameLauncher.findDataDir());
    private final JTextField usernameField = new JTextField();
    private final JSlider ramSlider = new JSlider(1024, 8192, 2048);
    private final JLabel ramLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Ready");
    private final PlayButton playButton = new PlayButton();
    private DarkToggle optifineToggle;
    private DarkToggle fpsToggle;
    private AccountButton accountButton;
    private NavButton navPlay;
    private NavButton navUpdates;
    private NavButton navSettings;

    private static String resolvedFamily;

    public LauncherFrame() {
        setTitle("Mix Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);
        setBackground(CANVAS);

        JPanel root = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g.setColor(CANVAS);
                g.fillRect(0, 0, getWidth(), getHeight());

                /* sidebar */
                g.setColor(SIDEBAR);
                g.fillRect(0, 0, SIDEBAR_WIDTH, getHeight());

                /* brand mark */
                g.setColor(ACCENT);
                g.fillRoundRect(16, 22, 40, 40, 12, 12);
                g.setColor(new Color(0x0E1A20));
                g.setFont(ui(Font.BOLD, 20));
                g.drawString("M", 28, 50);

                g.setColor(TEXT);
                g.setFont(ui(Font.BOLD, 15));
                g.drawString("MIX CLIENT", 66, 40);
                g.setColor(DIM);
                g.setFont(ui(Font.PLAIN, 11));
                g.drawString("v" + MixClientProtocol.VERSION + "  \u00b7  1.8.9", 66, 56);

                /* sidebar footer */
                g.setColor(BORDER_SOFT);
                g.fillRect(16, getHeight() - 140, SIDEBAR_WIDTH - 32, 1);
                g.setColor(DIM);
                g.setFont(ui(Font.PLAIN, 10));
                g.drawString("Standalone \u00b7 Mixin \u00b7 LaunchWrapper", 16, getHeight() - 118);
                g.drawString("\u00a9 Mix Client", 16, getHeight() - 104);

                /* card */
                g.setColor(CANVAS);
                g.fillRoundRect(CARD_X, CARD_Y, CARD_WIDTH, CARD_HEIGHT, 25, 25);
                g.setColor(BORDER);
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(CARD_X, CARD_Y, CARD_WIDTH, CARD_HEIGHT, 25, 25);

                /* header pill inside the card */
                g.setColor(RAISED);
                g.fillRoundRect(INNER_X, CARD_Y + 20, INNER_W, 50, 25, 25);

                g.dispose();
            }
        };
        setContentPane(root);

        /* ---------- navigation ---------- */
        navPlay = new NavButton("Play", NavButton.PLAY);
        navPlay.setBounds(16, 96, SIDEBAR_WIDTH - 32, 36);
        navPlay.addActionListener(event -> playButton.doClick());
        navPlay.setActive(true);
        root.add(navPlay);

        navUpdates = new NavButton("Updates", NavButton.UPDATES);
        navUpdates.setBounds(16, 136, SIDEBAR_WIDTH - 32, 36);
        navUpdates.addActionListener(event -> checkUpdates(true));
        root.add(navUpdates);

        navSettings = new NavButton("Settings", NavButton.SETTINGS);
        navSettings.setBounds(16, 176, SIDEBAR_WIDTH - 32, 36);
        navSettings.addActionListener(event -> showSettings());
        root.add(navSettings);

        accountButton = new AccountButton(
                "microsoft".equals(config.accountMode) ? config.msName : "OFFLINE",
                "microsoft".equals(config.accountMode));
        accountButton.setBounds(16, getHeight() - 46 - 44, SIDEBAR_WIDTH - 32, 44);
        accountButton.addActionListener(event -> onAccountClick());
        root.add(accountButton);

        /* ---------- card header ---------- */
        JLabel header = new JLabel("Ready to play", SwingConstants.CENTER);
        header.setBounds(INNER_X, CARD_Y + 20, INNER_W, 50);
        header.setOpaque(false);
        header.setBackground(RAISED);
        header.setForeground(Color.WHITE);
        header.setFont(ui(Font.BOLD, 20));
        root.add(header);

        /* ---------- username ---------- */
        JLabel userLabel = label("USERNAME", INNER_X, CARD_Y + 92);
        root.add(userLabel);

        usernameField.setText(config.username == null ? "Player" : config.username);
        usernameField.setBounds(INNER_X, CARD_Y + 108, INNER_W, 36);
        usernameField.setBackground(INPUT);
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(ACCENT);
        usernameField.setSelectionColor(new Color(0x55, 0x1D, 0xC3, 0xFF));
        usernameField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(0x60, 0x60, 0x60), 1),
                new EmptyBorder(6, 10, 6, 10)));
        usernameField.setFont(ui(Font.PLAIN, 14));
        usernameField.addActionListener(event -> playButton.doClick());
        root.add(usernameField);

        /* ---------- memory ---------- */
        JLabel ramTitle = label("MEMORY", INNER_X, CARD_Y + 160);
        root.add(ramTitle);

        ramSlider.setBounds(INNER_X, CARD_Y + 176, 300, 24);
        ramSlider.setOpaque(false);
        ramSlider.setMinimum(1024);
        ramSlider.setMaximum(8192);
        ramSlider.setMajorTickSpacing(1024);
        ramSlider.setPaintTicks(false);
        ramSlider.setValue(Math.max(1024, Math.min(8192, config.ramMb)));
        ramSlider.setForeground(ACCENT);
        ramSlider.setUI(new DarkSliderUI(ramSlider));
        ramSlider.addChangeListener(event -> updateRamLabel());
        root.add(ramSlider);

        ramLabel.setBounds(INNER_X + 320, CARD_Y + 170, INNER_W - 320, 28);
        ramLabel.setForeground(TEXT);
        ramLabel.setFont(ui(Font.BOLD, 15));
        ramLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        root.add(ramLabel);
        updateRamLabel();

        /* ---------- toggles ---------- */
        optifineToggle = new DarkToggle("OptiFine", config.optifine);
        optifineToggle.setBounds(INNER_X, CARD_Y + 214, 158, 36);
        optifineToggle.setToolTipText("Run the notch client with OptiFine 1.8.9 (HD U M5)");
        root.add(optifineToggle);

        fpsToggle = new DarkToggle("FPS pack", config.fpsBoost);
        fpsToggle.setBounds(INNER_X + 170, CARD_Y + 214, 158, 36);
        fpsToggle.setToolTipText("Apply a fast-render / low-detail options.txt preset");
        root.add(fpsToggle);

        /* ---------- play ---------- */
        playButton.setBounds(INNER_X, CARD_Y + 282, INNER_W, 52);
        playButton.addActionListener(event -> launch());
        root.add(playButton);

        /* ---------- status ---------- */
        statusLabel.setBounds(INNER_X, CARD_Y + 348, INNER_W, 100);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(ui(Font.PLAIN, 11));
        statusLabel.setVerticalAlignment(SwingConstants.TOP);
        root.add(statusLabel);

        JLabel closeLabel = new JLabel("x");
        closeLabel.setForeground(DIM);
        closeLabel.setFont(ui(Font.BOLD, 15));
        closeLabel.setBounds(WINDOW_WIDTH - 34, 14, 20, 20);
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                System.exit(0);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                closeLabel.setForeground(TEXT);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                closeLabel.setForeground(DIM);
            }
        });
        root.add(closeLabel);

        if (config.autoUpdate && config.updateUrl != null && !config.updateUrl.trim().isEmpty()) {
            Timer timer = new Timer(1500, event -> checkUpdates(false));
            timer.setRepeats(false);
            timer.start();
        }

        getRootPane().setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    /* ---------- typography ---------- */

    private static String family() {
        if (resolvedFamily == null) {
            Set<String> available = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            String picked = "Segoe UI";
            for (String candidate : new String[]{"Segoe UI", "Inter", "Ubuntu", "Dialog"}) {
                if (available.contains(candidate)) {
                    picked = candidate;
                    break;
                }
            }
            resolvedFamily = picked;
        }
        return resolvedFamily;
    }

    private static Font ui(int style, int size) {
        return new Font(family(), style, size);
    }

    private JLabel label(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, 300, 14);
        label.setForeground(MUTED);
        label.setFont(ui(Font.BOLD, 10));
        return label;
    }

    private void updateRamLabel() {
        ramLabel.setText(ramSlider.getValue() + " MB");
    }

    /* ---------- launch flow ---------- */

    private void launch() {
        String offlineName = usernameField.getText().trim().replaceAll("[^A-Za-z0-9_]", "");
        if (offlineName.isEmpty()) {
            offlineName = "Player";
        }
        if (offlineName.length() > 16) {
            offlineName = offlineName.substring(0, 16);
        }

        final String fallbackName = offlineName;
        config.ramMb = ramSlider.getValue();
        config.optifine = optifineToggle.isOn();
        config.fpsBoost = fpsToggle.isOn();
        if (!"microsoft".equals(config.accountMode)) {
            config.username = offlineName;
        }
        GameLauncher.saveConfig(GameLauncher.findDataDir(), config);

        playButton.setEnabled(false);
        accountButton.setEnabled(false);
        statusLabel.setText("Preparing 1.8.9...");

        Thread thread = new Thread(() -> {
            GameLauncher.LaunchConfig launchConfig = new GameLauncher.LaunchConfig();
            launchConfig.ramMb = config.ramMb;
            launchConfig.optifine = config.optifine;
            launchConfig.fpsBoost = config.fpsBoost;

            if ("microsoft".equals(config.accountMode)) {
                if (config.msClientId == null || config.msRefreshToken == null || config.msAccessToken == null) {
                    failLaunch("Microsoft account is not ready. Log in again.");
                    return;
                }
                try {
                    if (config.msExpiresAt <= (System.currentTimeMillis() / 1000L) + 120L) {
                        updateStatus("Refreshing Microsoft session...");
                        MicrosoftAuth.Session old = new MicrosoftAuth.Session();
                        old.name = config.msName;
                        old.uuid = config.msUuid;
                        old.accessToken = config.msAccessToken;
                        old.refreshToken = config.msRefreshToken;
                        old.expiresAt = config.msExpiresAt;
                        MicrosoftAuth.Session refreshed = MicrosoftAuth.refresh(old, config.msClientId);
                        config.msAccessToken = refreshed.accessToken;
                        config.msRefreshToken = refreshed.refreshToken;
                        config.msExpiresAt = refreshed.expiresAt;
                        config.msUuid = refreshed.uuid;
                        config.msName = refreshed.name;
                        GameLauncher.saveConfig(GameLauncher.findDataDir(), config);
                    }
                } catch (Exception exception) {
                    failLaunch("Microsoft session expired: " + exception.getMessage() + "\nLog in again.");
                    return;
                }
                launchConfig.username = config.msName != null ? config.msName : "Player";
                launchConfig.accessToken = config.msAccessToken;
                launchConfig.uuid = config.msUuid;
                launchConfig.userType = "msa";
            } else {
                launchConfig.username = fallbackName;
                launchConfig.userType = "legacy";
            }

            int exitCode = GameLauncher.launch(launchConfig, this::updateStatus);
            SwingUtilities.invokeLater(() -> {
                playButton.setEnabled(true);
                accountButton.setEnabled(true);
                if (exitCode != 0) {
                    statusLabel.setText("<html>Launch failed. See logs at "
                            + escape(new File(GameLauncher.findDataDir(), "logs/latest.log").getAbsolutePath())
                            + "</html>");
                }
            });
        }, "MixClient-Launcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("<html>" + escape(message) + "</html>"));
    }

    private void failLaunch(String message) {
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(true);
            accountButton.setEnabled(true);
            statusLabel.setText("<html>" + escape(message) + "</html>");
            JOptionPane.showMessageDialog(this, message, "Mix Client", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void onAccountClick() {
        if ("microsoft".equals(config.accountMode)) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Log out " + (config.msName == null ? "Microsoft account" : config.msName) + "?",
                    "Microsoft Account", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                config.accountMode = "offline";
                config.msAccessToken = null;
                config.msRefreshToken = null;
                config.msExpiresAt = 0L;
                config.msUuid = null;
                config.msName = null;
                GameLauncher.saveConfig(GameLauncher.findDataDir(), config);
                accountButton.setAccount("OFFLINE", false);
                statusLabel.setText("Logged out");
            }
            return;
        }
        loginMicrosoft();
    }

    private void loginMicrosoft() {
        String clientId = config.msClientId;
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = JOptionPane.showInputDialog(this,
                    "Azure Application (client) ID for Microsoft login:", "");
            if (clientId == null || clientId.trim().isEmpty()) {
                return;
            }
            clientId = clientId.trim();
            config.msClientId = clientId;
            GameLauncher.saveConfig(GameLauncher.findDataDir(), config);
        }

        final String finalClientId = clientId;
        accountButton.setEnabled(false);
        playButton.setEnabled(false);
        statusLabel.setText("Requesting Microsoft device code...");

        Thread thread = new Thread(() -> {
            try {
                MicrosoftAuth.DeviceCode code = MicrosoftAuth.requestDeviceCode(finalClientId);
                SwingUtilities.invokeLater(() -> {
                    openBrowser(code.verificationUri);
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(code.userCode), null);
                    JOptionPane.showMessageDialog(this,
                            "Open: " + code.verificationUri + "\nCode (copied): " + code.userCode
                                    + "\n\nEnter the code in the browser, then wait for the login to finish.",
                            "Microsoft Login", JOptionPane.INFORMATION_MESSAGE);
                });
                MicrosoftAuth.Session session = MicrosoftAuth.loginWithDeviceCode(
                        finalClientId, code, this::updateStatus);
                SwingUtilities.invokeLater(() -> {
                    config.accountMode = "microsoft";
                    config.msAccessToken = session.accessToken;
                    config.msRefreshToken = session.refreshToken;
                    config.msExpiresAt = session.expiresAt;
                    config.msUuid = session.uuid;
                    config.msName = session.name;
                    config.username = session.name;
                    GameLauncher.saveConfig(GameLauncher.findDataDir(), config);
                    accountButton.setAccount(session.name, true);
                    accountButton.setEnabled(true);
                    playButton.setEnabled(true);
                    usernameField.setText(session.name);
                    statusLabel.setText("Logged in as " + session.name);
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    accountButton.setEnabled(true);
                    playButton.setEnabled(true);
                    statusLabel.setText("<html>Microsoft login failed: " + escape(exception.getMessage()) + "</html>");
                    JOptionPane.showMessageDialog(this, "Microsoft login failed:\n" + exception.getMessage(),
                            "Microsoft Login", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "Microsoft-Auth");
        thread.setDaemon(true);
        thread.start();
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    private void showSettings() {
        JTextField clientIdField = new JTextField(config.msClientId == null ? "" : config.msClientId, 34);
        JTextField updateField = new JTextField(config.updateUrl == null ? "" : config.updateUrl, 34);
        JCheckBox autoUpdate = new JCheckBox("Auto-check for updates", config.autoUpdate);
        autoUpdate.setBackground(Color.WHITE);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.add(new JLabel("Microsoft Azure Application (client) ID"));
        panel.add(clientIdField);
        panel.add(new JLabel("Update manifest URL (HTTPS)"));
        panel.add(updateField);
        panel.add(autoUpdate);
        panel.add(new JLabel("<html><small>Azure app: personal Microsoft accounts, public client flows enabled,<br>"
                + "scope <code>XboxLive.signin offline_access</code>.</small></html>"));

        int result = JOptionPane.showConfirmDialog(this, panel, "Launcher Settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            config.msClientId = clientIdField.getText().trim();
            config.updateUrl = updateField.getText().trim();
            config.autoUpdate = autoUpdate.isSelected();
            GameLauncher.saveConfig(GameLauncher.findDataDir(), config);
            statusLabel.setText("Settings saved");
        }
    }

    public void checkUpdates(boolean manual) {
        String url = config.updateUrl;
        if (url == null || url.trim().isEmpty()) {
            if (manual) {
                JOptionPane.showMessageDialog(this, "Set an update manifest URL in Settings first.",
                        "Updates", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        statusLabel.setText("Checking for updates...");
        Thread thread = new Thread(() -> {
            try {
                File dist = GameLauncher.findDistDir();
                Updater.Update update = Updater.check(url.trim(), dist);
                String local = Updater.localVersion(dist);
                if (!Updater.isNewer(local, update.version)) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Up to date: v" + local);
                        if (manual) {
                            JOptionPane.showMessageDialog(this, "You are on the latest version (v" + local + ").",
                                    "Updates", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                    return;
                }
                final boolean[] accepted = {false};
                SwingUtilities.invokeAndWait(() -> {
                    String changelog = update.changelog == null || update.changelog.isEmpty()
                            ? "" : "\n\n" + update.changelog;
                    accepted[0] = JOptionPane.showConfirmDialog(this,
                            "Update " + update.version + " is available (installed: " + local + ")." + changelog
                                    + "\n\nDownload and install now?",
                            "Mix Client Update", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                });
                if (!accepted[0]) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Update skipped"));
                    return;
                }
                updateStatus("Updating to " + update.version + "...");
                Updater.apply(update, dist, this::updateStatus);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Updated to v" + update.version + " \u2014 restart the client");
                    JOptionPane.showMessageDialog(this,
                            "Updated to v" + update.version + ". Restart the client to apply changes.",
                            "Updates", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("<html>Update failed: " + escape(exception.getMessage()) + "</html>");
                    if (manual) {
                        JOptionPane.showMessageDialog(this, "Update failed:\n" + exception.getMessage(),
                                "Updates", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        }, "MixClient-Updater");
        thread.setDaemon(true);
        thread.start();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /* ---------- widgets ---------- */

    private static final class NavButton extends JButton {
        static final int PLAY = 0;
        static final int UPDATES = 1;
        static final int SETTINGS = 2;

        private final String label;
        private final int icon;
        private boolean active;
        private boolean hovered;

        NavButton(String label, int icon) {
            this.label = label;
            this.icon = icon;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || hovered) {
                g.setColor(active ? RAISED : INPUT_HOVER);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            }

            int cx = 18;
            int cy = getHeight() / 2;
            g.setColor(active ? Color.WHITE : MUTED);
            g.setStroke(new BasicStroke(1.6f));
            switch (icon) {
                case PLAY:
                    g.fillPolygon(new Polygon(
                            new int[]{cx - 4, cx - 4, cx + 5},
                            new int[]{cy - 6, cy + 6, cy}, 3));
                    break;
                case UPDATES:
                    g.drawOval(cx - 6, cy - 6, 12, 12);
                    g.drawLine(cx, cy - 3, cx, cy + 3);
                    g.drawLine(cx - 3, cy, cx, cy + 3);
                    g.drawLine(cx + 3, cy, cx, cy + 3);
                    break;
                default:
                    g.drawOval(cx - 5, cy - 5, 10, 10);
                    g.fillOval(cx - 2, cy - 2, 4, 4);
                    break;
            }

            g.setFont(ui(Font.PLAIN, 15));
            g.setColor(active ? Color.WHITE : MUTED);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(label, 36, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class DarkSliderUI extends BasicSliderUI {
        DarkSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public Dimension getThumbSize() {
            return new Dimension(14, 14);
        }

        @Override
        public void paintTrack(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = trackRect.y + trackRect.height / 2 - 2;
            g.setColor(RAISED);
            g.fillRoundRect(trackRect.x, y, trackRect.width, 4, 4, 4);
            int filled = Math.max(2, thumbRect.x + thumbRect.width / 2 - trackRect.x);
            g.setColor(ACCENT);
            g.fillRoundRect(trackRect.x, y, Math.min(filled, trackRect.width), 4, 4, 4);
            g.dispose();
        }

        @Override
        public void paintThumb(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillOval(thumbRect.x, thumbRect.y + thumbRect.height / 2 - 6, 12, 12);
            g.dispose();
        }
    }

    private static final class CycleButton extends JButton {
        private final String prefix;
        private final List<String> values;
        private int index;
        private boolean hovered;

        CycleButton(String prefix, List<String> values, String selected) {
            this.prefix = prefix;
            this.values = values;
            this.index = Math.max(0, values.indexOf(selected));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(event -> {
                index = (index + 1) % values.size();
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        String value() {
            return values.get(index);
        }

        void setValues(List<String> newValues, String selected) {
            values.clear();
            values.addAll(newValues);
            index = Math.max(0, values.indexOf(selected));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered ? INPUT_HOVER : INPUT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
            g.setColor(hovered ? new Color(0x80, 0x80, 0x80) : new Color(0x60, 0x60, 0x60));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
            g.setFont(ui(Font.BOLD, 9));
            g.setColor(DIM);
            g.drawString(prefix, 10, 14);
            g.setFont(ui(Font.PLAIN, 13));
            g.setColor(TEXT);
            g.drawString(value(), 10, 28);
            g.setColor(ACCENT);
            g.setFont(ui(Font.BOLD, 13));
            g.drawString(">", getWidth() - 18, 24);
            g.dispose();
        }
    }

    private static final class DarkToggle extends JButton {
        private final String label;
        private boolean on;
        private boolean hovered;

        DarkToggle(String label, boolean on) {
            this.label = label;
            this.on = on;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(event -> {
                this.on = !this.on;
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        boolean isOn() {
            return on;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered ? INPUT_HOVER : INPUT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
            g.setColor(hovered ? new Color(0x80, 0x80, 0x80) : new Color(0x60, 0x60, 0x60));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);

            int toggleWidth = 30;
            int toggleX = getWidth() - toggleWidth - 10;
            int toggleY = (getHeight() - 16) / 2;
            g.setColor(on ? ACCENT : RAISED);
            g.fillRoundRect(toggleX, toggleY, toggleWidth, 16, 8, 8);
            g.setColor(on ? new Color(0x0E1A20) : MUTED);
            g.fillOval(toggleX + (on ? toggleWidth - 13 : 3), toggleY + 3, 10, 10);

            g.setFont(ui(Font.PLAIN, 13));
            g.setColor(TEXT);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(label, 12, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class AccountButton extends JButton {
        private String value;
        private boolean microsoft;
        private boolean hovered;

        AccountButton(String value, boolean microsoft) {
            this.value = value == null ? "OFFLINE" : value;
            this.microsoft = microsoft;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to log in or log out of Microsoft");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setAccount(String value, boolean microsoft) {
            this.value = value == null ? "OFFLINE" : value;
            this.microsoft = microsoft;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(hovered ? INPUT_HOVER : INPUT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g.setColor(new Color(0x33, 0x33, 0x33));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

            g.setColor(microsoft ? ACCENT : RAISED);
            g.fillOval(12, 12, 20, 20);
            g.setColor(new Color(0x0E1A20));
            g.setFont(ui(Font.BOLD, 11));
            g.drawString(microsoft ? "MS" : "OF", 15, 26);

            g.setFont(ui(Font.PLAIN, 13));
            g.setColor(TEXT);
            String label = value;
            FontMetrics metrics = g.getFontMetrics();
            while (metrics.stringWidth(label) > getWidth() - 62 && label.length() > 3) {
                label = label.substring(0, label.length() - 1);
            }
            g.drawString(label, 40, 26);

            g.setColor(microsoft ? SUCCESS : DIM);
            g.fillOval(getWidth() - 16, 19, 6, 6);
            g.dispose();
        }
    }

    private static final class PlayButton extends JButton {
        private boolean hovered;

        PlayButton() {
            super("PLAY");
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(ui(Font.BOLD, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = isEnabled() ? (hovered ? ACCENT_HOVER : ACCENT) : new Color(0x2B2B2B);
            g.setColor(base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);

            g.setColor(isEnabled() ? Color.WHITE : DIM);
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            String text = getText();
            g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class MixClientProtocol {
        static final String VERSION = "1.0.0";
    }
}
