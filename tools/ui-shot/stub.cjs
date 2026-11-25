/* Fake IPC surface for the screenshot harness: enough of the real preload for
   any page to render, with a mods list that exercises every row state. */

const dir = 'C:\\Users\\player\\AppData\\Roaming\\.minecraft\\mods';

/* a real 64×64 skin so the wardrobe renders a character, not an empty box */
const skinUrl = `data:image/png;base64,${require('node:fs')
  .readFileSync(require('node:path').join(__dirname, 'fixtures', 'steve.png'))
  .toString('base64')}`;

const mods = [
  {
    file: 'OptiFine_1.8.9_HD_U_M5.jar',
    name: 'OptiFine_1.8.9_HD_U_M5.jar',
    enabled: true,
    size: 2_402_111,
    title: 'OptiFine',
    version: 'HD U M5',
    author: 'sp614x'
  },
  {
    file: 'notenoughanimations-1.0.2.jar',
    name: 'notenoughanimations-1.0.2.jar',
    enabled: true,
    size: 184_320,
    title: 'NotEnoughAnimations',
    version: '1.0.2',
    author: 'tr7zw'
  },
  {
    file: 'oldanimationsmod-2.4.jar',
    name: 'oldanimationsmod-2.4.jar',
    enabled: true,
    size: 96_256,
    title: 'Old Animations Mod',
    version: '2.4',
    author: 'Lmix'
  },
  {
    file: 'keystrokesmod-1.8.jar',
    name: 'keystrokesmod-1.8.jar',
    enabled: false,
    size: 71_680,
    title: 'Keystrokes',
    version: '1.8',
    author: ''
  },
  {
    file: 'chromahud-3.1.jar',
    name: 'chromahud-3.1.jar',
    enabled: false,
    size: 512_000,
    title: 'ChromaHUD',
    version: '3.1',
    author: 'Mojang'
  }
];

const state = {
  ramMb: 4096,
  javaPath: 'java',
  closeOnLaunch: false,
  optifine: true,
  forge: process.env.SHOT_FORGE !== 'off',
  activeProfile: 'PvP',
  username: 'Player',
  msClientId: '',
  updateUrl: '',
  autoUpdate: true,
  profiles: [
    { name: 'Default', note: 'Balanced settings, every visual module available.' },
    { name: 'PvP', note: 'Lowest input latency, minimised HUD.' }
  ]
};

const noop = () => () => {};

window.mixclient = {
  state: () =>
    Promise.resolve({
      state,
      account: { name: 'Notch', uuid: '069a79f444e94726a5befca90e38aaf5', expiresAt: 0 },
      distDir: 'C:\\Mix Client\\resources\\dist',
      clientVersion: '1.2.1',
      launcherVersion: '1.2.1',
      launcherPackaged: true,
      pendingLauncher: process.env.SHOT_PENDING || null,
      version: '1.2.1',
      updateConfigured: false,
      java: 'C:\\Mix Client\\resources\\jre\\bin\\javaw.exe',
      install: { minecraftDir: 'C:\\Users\\player\\AppData\\Roaming\\.minecraft', jar: true, assets: true },
      forge: {
        version: '1.8.9-11.15.1.2318-1.8.9',
        id: '1.8.9-forge1.8.9-11.15.1.2318-1.8.9',
        dir: 'C:\\Users\\player\\AppData\\Roaming\\.minecraft\\versions\\1.8.9-forge1.8.9-11.15.1.2318-1.8.9',
        installed: true
      },
      mods: { dir, mods }
    }),
  save: () => Promise.resolve(state),
  launch: () => Promise.resolve({ ok: true, message: 'Launching as Notch' }),
  ensureInstalled: () => Promise.resolve({ ok: true, message: 'ready' }),
  beginLogin: () => Promise.resolve({ ok: true }),
  cancelLogin: () => Promise.resolve({ ok: true }),
  signOut: () => Promise.resolve({ ok: true }),
  accountSkin: () => Promise.resolve({ url: skinUrl, slim: false, source: 'account' }),
  localSkins: () =>
    Promise.resolve([
      { id: 'slim-test.png', name: 'Mix_Dz', url: skinUrl, variant: 'slim', source: 'local' }
    ]),
  addSkin: () => Promise.resolve({ ok: false, skins: [] }),
  checkUpdate: () =>
    Promise.resolve({
      ok: true,
      local: '1.2.1',
      remote: '1.3.0',
      newer: true,
      changelog: 'Forge mode, mods tab, launcher self-update.',
      files: 12,
      update: { version: '1.3.0', files: [] },
      launcher: { local: '1.2.1', remote: '1.3.0', newer: true, packaged: true, files: 1 }
    }),
  applyUpdate: () => Promise.resolve({ ok: true, message: 'Updated client 1.3.0 · launcher 1.3.0 ready', version: '1.3.0' }),
  restartForUpdate: () => Promise.resolve({ ok: true }),
  openDir: () => Promise.resolve(),
  openUrl: () => Promise.resolve(),
  window: () => Promise.resolve(),
  mods: {
    list: () => Promise.resolve({ dir, mods }),
    add: () => Promise.resolve({ ok: true, added: ['newmod.jar'], mods }),
    toggle: () => Promise.resolve({ ok: true }),
    remove: () => Promise.resolve({ ok: true, message: 'Moved to trash' }),
    reveal: () => Promise.resolve()
  },
  forge: {
    status: () => Promise.resolve({ installed: true }),
    ensure: () => Promise.resolve({ ok: true, message: 'Forge ready' })
  },
  onLog: noop,
  onExit: noop,
  onAuthCode: noop,
  onAuthStatus: noop,
  onAuthDone: noop,
  onAuthError: noop,
  onProgress: noop,
  onUpdateProgress: noop,
  onUpdateStatus: noop
};
