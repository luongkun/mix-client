/* Screenshot harness for the Electron launcher UI.

   Runs the built renderer (launcher-desktop/dist) with a stubbed
   `window.mixclient`, so a page can be inspected without the real main process
   and without touching the player's mods folder.

   Usage (Git Bash):
     env -u ELECTRON_RUN_AS_NODE -u NODE_OPTIONS \
       ./launcher-desktop/node_modules/.bin/electron tools/ui-shot/shot.cjs mods build/ui-mods.png
*/

const { app, BrowserWindow } = require('electron');
const os = require('node:os');
const path = require('node:path');

const [, , page = 'play', out = 'build/ui-shot.png'] = process.argv;
const root = path.resolve(__dirname, '..', '..');
const renderer = path.join(root, 'launcher-desktop', 'dist', 'index.html');

/* a throwaway profile keeps the harness from inheriting (or persisting) the
   real launcher's zoom level and window state */
app.setPath('userData', path.join(os.tmpdir(), 'mixclient-ui-shot'));
/* the OS runs at 250% here; pinning the scale makes the window size equal the
   CSS viewport, so a shot at 1240×760 is the layout the player actually sees */
app.commandLine.appendSwitch('force-device-scale-factor', '1');
app.disableHardwareAcceleration();
app.whenReady().then(async () => {
  const win = new BrowserWindow({
    width: Number(process.env.SHOT_WIDTH || 1180),
    height: Number(process.env.SHOT_HEIGHT || 740),
    show: false,
    backgroundColor: '#08080A',
    webPreferences: {
      preload: path.join(__dirname, 'stub.cjs'),
      contextIsolation: false,
      nodeIntegration: false,
      /* the stub reads a skin PNG from disk, which a sandboxed preload cannot */
      sandbox: false
    }
  });
  win.webContents.on('console-message', (_event, level, message) => {
    if (level >= 2) {
      console.log('renderer:', message);
    }
  });
  win.webContents.on('render-process-gone', (_event, details) => console.log('gone:', JSON.stringify(details)));
  await win.loadFile(renderer);
  const zoom = Number(process.env.SHOT_ZOOM || 1);
  win.webContents.setZoomFactor(zoom);
  await new Promise((resolve) => setTimeout(resolve, 700));
  const clicked = await win.webContents.executeJavaScript(`
    (() => {
      const label = ${JSON.stringify(page)};
      const buttons = [...document.querySelectorAll('nav button[title]')];
      const button = buttons.find((entry) => (entry.title || '').trim().toLowerCase() === label.toLowerCase());
      if (button) button.click();
      return { found: Boolean(button), labels: buttons.map((entry) => entry.title || entry.textContent.trim()) };
    })()
  `);
  console.log('rail:', JSON.stringify(clicked));
  console.log(
    'heading:',
    await win.webContents.executeJavaScript(`document.querySelector('.section-title')?.textContent ?? '(none)'`)
  );
  await new Promise((resolve) => setTimeout(resolve, 600));
  if (process.env.SHOT_ACTION) {
    const action = process.env.SHOT_ACTION;
    const clickedAction = await win.webContents.executeJavaScript(`
      (() => {
        const wanted = ${JSON.stringify(action.toLowerCase())};
        const buttons = [...document.querySelectorAll('button')];
        const button = buttons.find((entry) => entry.textContent.trim().toLowerCase() === wanted);
        if (button) button.click();
        return { found: Boolean(button), labels: buttons.map((entry) => entry.textContent.trim()) };
      })()
    `);
    console.log('action:', JSON.stringify(clickedAction));
    await new Promise((resolve) => setTimeout(resolve, 600));
    console.log(
      'rows:',
      await win.webContents.executeJavaScript(
        `[...document.querySelectorAll('.setting-row')].map((row) => row.innerText.replace(/\\n/g, ' | ')).slice(0, 6)`
      )
    );
  }
  if (process.env.SHOT_SCROLL) {
    await win.webContents.executeJavaScript(`
      (() => {
        const scroller = document.querySelector('.content');
        if (scroller) scroller.scrollTop = scroller.scrollHeight;
        return Boolean(scroller);
      })()
    `);
    await new Promise((resolve) => setTimeout(resolve, 300));
  }
  /* a hidden window can hand back the frame from before the last React
     commit; a first capture flushes it */
  await win.webContents.capturePage();
  await new Promise((resolve) => setTimeout(resolve, 300));
  const image = await win.webContents.capturePage();
  const fs = require('node:fs');
  fs.mkdirSync(path.dirname(path.resolve(root, out)), { recursive: true });
  fs.writeFileSync(path.resolve(root, out), image.toPNG());
  console.log(`shot: ${path.resolve(root, out)}`);
  app.quit();
});
