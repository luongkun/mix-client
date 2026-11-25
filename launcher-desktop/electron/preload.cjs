/* CommonJS on purpose: this package sets "type": "module", and an Electron
   preload script still has to be CommonJS to run with context isolation. */

const { contextBridge, ipcRenderer } = require('electron');

function subscribe(channel, callback) {
  const listener = (_event, payload) => callback(payload);
  ipcRenderer.on(channel, listener);
  return () => ipcRenderer.removeListener(channel, listener);
}

contextBridge.exposeInMainWorld('mixclient', {
  state: () => ipcRenderer.invoke('app:state'),
  save: (patch) => ipcRenderer.invoke('app:save', patch),

  beginLogin: () => ipcRenderer.invoke('auth:begin'),
  loginOffline: (username) => ipcRenderer.invoke('auth:offline', username),
  accounts: {
    list: () => ipcRenderer.invoke('accounts:list'),
    addOffline: (name) => ipcRenderer.invoke('accounts:add-offline', name),
    select: (id) => ipcRenderer.invoke('accounts:select', id),
    remove: (id) => ipcRenderer.invoke('accounts:remove', id)
  },
  cancelLogin: () => ipcRenderer.invoke('auth:cancel'),
  signOut: () => ipcRenderer.invoke('auth:signout'),

  launch: (options) => ipcRenderer.invoke('game:launch', options),
  ensureInstalled: () => ipcRenderer.invoke('install:ensure'),

  mods: {
    list: () => ipcRenderer.invoke('mods:list'),
    add: () => ipcRenderer.invoke('mods:add'),
    toggle: (file, enabled) => ipcRenderer.invoke('mods:toggle', { file, enabled }),
    remove: (file) => ipcRenderer.invoke('mods:remove', file),
    reveal: (file) => ipcRenderer.invoke('mods:reveal', file)
  },
  forge: {
    status: () => ipcRenderer.invoke('forge:status'),
    ensure: () => ipcRenderer.invoke('forge:ensure')
  },

  checkUpdate: () => ipcRenderer.invoke('update:check'),
  applyUpdate: (payload) => ipcRenderer.invoke('update:apply', payload),
  restartForUpdate: () => ipcRenderer.invoke('update:restart'),
  accountSkin: (username) => ipcRenderer.invoke('skin:account', username),
  localSkins: () => ipcRenderer.invoke('skin:local'),
  addSkin: () => ipcRenderer.invoke('skin:add'),

  openDir: (target) => ipcRenderer.invoke('app:open-dir', target),
  openUrl: (url) => ipcRenderer.invoke('app:open-url', url),
  window: (action) => ipcRenderer.invoke('app:window', action),

  onLog: (callback) => subscribe('game:log', callback),
  onExit: (callback) => subscribe('game:exit', callback),
  onAuthCode: (callback) => subscribe('auth:code', callback),
  onAuthStatus: (callback) => subscribe('auth:status', callback),
  onAuthDone: (callback) => subscribe('auth:done', callback),
  onAuthError: (callback) => subscribe('auth:error', callback),
  onProgress: (callback) => subscribe('install:progress', callback),
  onUpdateProgress: (callback) => subscribe('update:progress', callback),
  onUpdateStatus: (callback) => subscribe('update:status', callback)
});
