import { lazy, Suspense, useEffect, useState } from 'react';
import TopBar from './ui/TopBar.tsx';
import Sidebar from './ui/Sidebar.tsx';
import HomeView from './views/HomeView.tsx';
import ModsView from './views/ModsView.tsx';
import AccountsView from './views/AccountsView.tsx';
import VersionsView from './views/VersionsView.tsx';
import SettingsView from './views/SettingsView.tsx';

const WardrobeView = lazy(() => import('./views/WardrobeView.tsx'));

/* App shell: top bar + floating sidebar + the current view. All launcher state
   and IPC lives here; the views only receive props. */

const api = window.mixclient;

const FALLBACK_STATE = {
  ramMb: 2048,
  javaPath: 'java',
  closeOnLaunch: false,
  /* one loader per launch, picked in Versions; OptiFine ships inside the build */
  loader: 'optifine',
  activeProfile: 'Default',
  username: 'Player',
  msClientId: '9b19dce6-ef75-4294-8640-00ad95dd7e35',
  updateUrl: '',
  autoUpdate: true,
  /* a fresh install carries exactly one profile; the player's settings are
     saved into it as they change them */
  profiles: [{ name: 'Default', note: 'Balanced settings, every visual module available.' }]
};

const TITLES = {
  home: 'Home',
  mods: 'Modpack Builder',
  wardrobe: 'Wardrobe',
  versions: 'Versions',
  account: 'Accounts',
  settings: 'Settings'
};

export default function App() {
  const [page, setPage] = useState('home');
  const [state, setState] = useState(null);
  const [account, setAccount] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [skinCache, setSkinCache] = useState({});
  const [selectedAccountId, setSelectedAccountId] = useState(null);
  const [info, setInfo] = useState({ distDir: '', version: '1.0.0', clientVersion: 'unknown', pendingLauncher: null });
  const [mods, setMods] = useState({ dir: '', mods: [] });
  const [status, setStatus] = useState({ tone: 'idle', text: 'Ready when you are' });
  const [launching, setLaunching] = useState(false);
  const [progress, setProgress] = useState(null);
  const [modBusy, setModBusy] = useState(false);
  const [skin, setSkin] = useState(null);
  const [accountSlim, setAccountSlim] = useState(false);
  const [slimOverride, setSlimOverride] = useState(null);
  const [localSkins, setLocalSkins] = useState([]);
  const [selectedSkin, setSelectedSkin] = useState('account');

  useEffect(() => {
    if (!api) {
      setState(FALLBACK_STATE);
      return undefined;
    }
    api.state().then((payload) => {
      setState({ ...FALLBACK_STATE, ...payload.state });
      setAccount(payload.account);
      setInfo({ ...payload, pendingLauncher: payload.pendingLauncher ?? null });
      setMods(payload.mods ?? { dir: '', mods: [] });
    });

    const offs = [
      api.onExit((code) => {
        setLaunching(false);
        setStatus({ tone: code === 0 ? 'ok' : 'error', text: code === 0 ? 'Game closed' : `Game exited with ${code}` });
      }),
      api.onAuthCode(() => setStatus({ tone: 'busy', text: 'Waiting for Microsoft approval…' })),
      api.onAuthDone((payload) => {
        setAccount(payload);
        setStatus({ tone: 'ok', text: payload ? `Signed in as ${payload.name}` : 'Signed out' });
        /* the main process already wrote the account to accounts.json; re-read it
   so the new entry shows up without a restart */
        refreshAccounts();
      }),
      api.onAuthError((message) => setStatus({ tone: 'error', text: String(message) })),
      api.onProgress((payload) => setProgress(payload)),
      api.onUpdateStatus((event) => {
        if (event.phase === 'checking') {
          setStatus({ tone: 'busy', text: 'Checking for updates…' });
        } else if (event.phase === 'ready') {
          setStatus({ tone: 'ok', text: `Client ${event.version} downloaded` });
        } else if (event.phase === 'error') {
          setStatus({ tone: 'error', text: `Update failed: ${event.message}` });
        }
      })
    ];
    return () => offs.forEach((off) => off?.());
  }, []);

  useEffect(() => {
    if (!api) {
      return undefined;
    }
    refreshAccounts();
    return undefined;
  }, [api]);

  useEffect(() => {
    if (!api) {
      return undefined;
    }
    let cancelled = false;
    api
      .accountSkin(account?.name)
      .then((result) => {
        if (cancelled) {
          return;
        }
        setSkin(result?.url ?? null);
        setAccountSlim(Boolean(result?.slim));
      })
      .catch(() => {});
    api
      .localSkins()
      .then((list) => {
        if (!cancelled) {
          setLocalSkins(list ?? []);
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [account?.name]);

  /* one skin lookup per account, cached by name */
  useEffect(() => {
    if (!api || accounts.length === 0) {
      return;
    }
    let cancelled = false;
    accounts.forEach((entry) => {
      if (Object.prototype.hasOwnProperty.call(skinCache, entry.name)) {
        return;
      }
      api
        .accountSkin(entry.name)
        .then((result) => {
          if (!cancelled) {
            setSkinCache((previous) => ({
              ...previous,
              [entry.name]: { url: result?.url ?? null, slim: Boolean(result?.slim) }
            }));
          }
        })
        .catch(() => {
          if (!cancelled) {
            setSkinCache((previous) => ({ ...previous, [entry.name]: { url: null, slim: false } }));
          }
        });
    });
    return () => {
      cancelled = true;
    };
  }, [accounts, api]);

  /* Offline names have no Mojang profile, so accountSkin yields nothing. Fall
     back to the first local skin so every account still previews. */
  const fallbackSkin = localSkins[0] ?? null;
  const skinForName = (name) => {
    if (!name) return null;
    return skinCache[name]?.url ?? fallbackSkin?.url ?? null;
  };
  /* Which model to render: the one Mojang reports for the account skin, or the
     variant detected from the local skin file. */
  const slimForName = (name) => {
    if (!name) return false;
    const cached = skinCache[name];
    if (cached && cached.url) return Boolean(cached.slim);
    return fallbackSkin?.variant === 'slim';
  };

  if (!state) {
    return <div className="grid h-full place-items-center text-[13px] text-ink-3">Loading Mix Client…</div>;
  }

  /* imported skins carry the model detected from their texture; the account skin
     uses the one Mojang reports. Picking Classic/Slim by hand wins. */
  const selectedLocal = localSkins.find((entry) => entry.id === selectedSkin);
  const slim = slimOverride ?? (selectedLocal ? selectedLocal.variant === 'slim' : accountSlim);

  const update = (patch) => {
    setState((prev) => ({ ...prev, ...patch }));
    api?.save(patch);
  };

  const launch = async () => {
    if (!api) {
      return;
    }
    setLaunching(true);
    setStatus({ tone: 'busy', text: 'Preparing Minecraft…' });
    const result = await api.launch({
      profile: state.activeProfile,
      ramMb: state.ramMb,
      loader: state.loader ?? 'optifine'
    });
    if (!result?.ok) {
      setLaunching(false);
      setStatus({ tone: 'error', text: result?.message ?? 'Could not start the game' });
    } else {
      setStatus({ tone: 'ok', text: 'Minecraft is starting' });
    }
  };

  const signIn = async () => {
    if (!api) {
      return;
    }
    setStatus({ tone: 'busy', text: 'Opening Microsoft sign-in…' });
    const result = await api.beginLogin();
    if (!result?.ok) {
      setStatus({ tone: 'error', text: result?.message ?? 'Sign-in failed' });
    }
  };

  async function refreshAccounts() {
    const payload = await api?.accounts?.list();
    if (payload) {
      setAccounts(payload.accounts ?? []);
      setSelectedAccountId(payload.selectedId ?? null);
    }
  };

  async function addOfflineAccount(username) {
    const result = await api?.accounts?.addOffline(username);
    if (result?.ok) {
      await refreshAccounts();
      setAccount({ name: username.trim(), mode: 'offline' });
      setStatus({ tone: 'ok', text: `Selected ${username.trim()}` });
    }
    return result ?? { ok: false, message: 'Launcher bridge unavailable' };
  };

  async function selectAccount(id) {
    const result = await api?.accounts?.select(id);
    if (result?.ok === false) {
      setStatus({ tone: 'error', text: result.message ?? 'Could not select account' });
      return;
    }
    if (result?.ok) {
      await refreshAccounts();
      const picked = result.accounts?.find((entry) => entry.id === id);
      if (picked) {
        setAccount({ name: picked.name, uuid: picked.uuid, mode: picked.type });
      }
    }
  };

  async function removeAccount(id) {
    const result = await api?.accounts?.remove(id);
    if (result?.ok) {
      await refreshAccounts();
    }
  };

  const signOut = async () => {
    await api?.signOut();
    setAccount(null);
    setStatus({ tone: 'idle', text: 'Signed out' });
  };

  const refreshMods = async () => {
    const payload = await api?.mods.list();
    setMods(payload ?? { dir: '', mods: [] });
  };

  const runModAction = async (action, successMessage) => {
    setModBusy(true);
    try {
      const result = await action();
      if (result?.canceled) {
        return;
      }
      if (Array.isArray(result?.mods)) {
        setMods((previous) => ({ dir: result.dir ?? previous.dir, mods: result.mods }));
      } else {
        await refreshMods();
      }
      if (result?.ok === false) {
        const skipped = result.skipped?.length ? ` (${result.skipped.slice(0, 2).join(', ')})` : '';
        setStatus({ tone: 'error', text: `${result.message ?? 'Mod operation failed.'}${skipped}` });
        return;
      }
      setStatus({ tone: 'ok', text: typeof successMessage === 'function' ? successMessage(result) : successMessage });
    } catch (error) {
      setStatus({ tone: 'error', text: `Mod operation failed: ${String(error?.message ?? error)}` });
    } finally {
      setModBusy(false);
    }
  };

  const addMods = async () => {
    if (!api) return;
    await runModAction(() => api.mods.add(), (result) => {
      const added = result?.added?.length ?? 0;
      const skipped = result?.skipped?.length ?? 0;
      return `Added ${added} mod${added === 1 ? '' : 's'}${skipped ? `; skipped ${skipped}` : ''}`;
    });
  };

  const toggleMod = async (file) => {
    const entry = mods.mods.find((mod) => mod.file === file);
    if (!entry || !api) {
      return;
    }
    await runModAction(
      () => api.mods.toggle(file, !entry.enabled),
      `${entry.enabled ? 'Disabled' : 'Enabled'} ${entry.title || entry.name}`
    );
  };

  const removeMod = async (file) => {
    if (!api) {
      return;
    }
    const entry = mods.mods.find((mod) => mod.file === file);
    await runModAction(() => api.mods.remove(file), `Moved ${entry?.title || entry?.name || file} to trash`);
  };

  const addSkin = async () => {
    const result = await api?.addSkin();
    if (result?.skins) {
      setLocalSkins(result.skins);
      const added = result.skins[result.skins.length - 1];
      if (added) {
        setSelectedSkin(added.id);
        setSlimOverride(null);
        setStatus({
          tone: 'ok',
          text: `Skin added — detected ${added.variant === 'slim' ? 'Slim' : 'Classic'} model`
        });
      }
    }
  };

  const checkUpdate = async () => {
    setStatus({ tone: 'busy', text: 'Checking for updates…' });
    const result = await api?.checkUpdate();
    if (result?.ok === false) {
      setStatus({ tone: 'error', text: result.message ?? 'Update check failed' });
    }
  };

  const openDir = (target) => api?.openDir(target);

  return (
    <div className="flex h-full flex-col bg-canvas">
      <TopBar
        title={TITLES[page] ?? 'Home'}
        onMinimize={() => api?.window('minimize')}
        onClose={() => api?.window('close')}
        onQuickLaunch={launch}
        onCreate={signIn}
        onSettings={() => setPage('settings')}
      />

      <div className="flex min-h-0 flex-1">
        <Sidebar page={page} onNavigate={setPage} />

        <main className="ml-4 min-h-0 flex-1 pt-2">
          {page === 'home' ? (
            <HomeView
              account={account}
              skin={skin}
              localSkins={localSkins}
              slim={slim}
              profileName={state.activeProfile}
              version={state.version ?? '1.8.9'}
              ramMb={state.ramMb}
              launching={launching}
              status={status.text}
              statusTone={status.tone}
              progress={progress}
              onLaunch={launch}
              onOpenAccounts={() => setPage('account')}
              onOpenWardrobe={() => setPage('wardrobe')}
            />
          ) : null}

          {page === 'mods' ? (
            <ModsView
              mods={mods}
              busy={modBusy}
              onAdd={addMods}
              onToggle={toggleMod}
              onRemove={removeMod}
              onUseAsProfile={(name) => {
                update({ activeProfile: name });
                setStatus({ tone: 'ok', text: `Profile ${name} is ready` });
              }}
              profileName={state.activeProfile}
              version={state.version ?? '1.8.9'}
              loader={state.loader ?? 'optifine'}
            />
          ) : null}

          {page === 'versions' ? (
            <VersionsView
              version={state.version ?? '1.8.9'}
              branch={state.branch ?? 'RELEASE 1.8.0'}
              loader={state.loader ?? 'optifine'}
              launching={launching}
              onVersion={(id) => update({ version: id })}
              onBranch={(branchName) => update({ branch: branchName })}
              onLoader={(kind) => update({ loader: kind })}
              onLaunch={launch}
            />
          ) : null}

          {page === 'account' ? (
            <AccountsView
              accounts={accounts}
              selectedId={selectedAccountId}
              skinFor={(name) => skinForName(name)}
              slimFor={(name) => slimForName(name)}
              busy={launching}
              onAddOffline={addOfflineAccount}
              onAddMicrosoft={signIn}
              onSelect={selectAccount}
              onRemove={removeAccount}
            />
          ) : null}

          {page === 'wardrobe' ? (
            <Suspense fallback={<div className="grid h-full place-items-center text-sm text-ink-3">Loading wardrobe…</div>}>
              <WardrobeView
                skin={skin}
                slim={slim}
                localSkins={localSkins}
                selected={selectedSkin}
                onSelect={(id) => {
                  setSelectedSkin(id);
                  setSlimOverride(null);
                }}
                onVariant={(variant) => setSlimOverride(variant === 'slim')}
                onAdd={addSkin}
              />
            </Suspense>
          ) : null}

          {page === 'settings' ? (
            <SettingsView
              state={state}
              account={account}
              info={info}
              update={update}
              onSignIn={signIn}
              onSignOut={signOut}
              onCheckUpdate={checkUpdate}
              onOpenDir={openDir}
              status={status.text}
            />
          ) : null}
        </main>
      </div>
    </div>
  );
}
