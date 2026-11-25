# HANDOFF — Mix Client

> Cập nhật: **25/11/2026** · Trạng thái: **MixUI-only**, hệ native cũ đã được loại bỏ khỏi source.
> Client: **1.8.2** · Launcher Electron: **1.9.7** · Minecraft: **1.8.9 Forge / MCP `stable_22`**.

## 1. Trạng thái hiện tại

- Client chỉ còn hệ **MixUI** dưới `src/main/java/mix/client/mixui/`; nhãn hiệu hiện tại là **Mix Client**.
- Rebrand zero-trace: source, ba jar client, launcher jar, Dango jar và `app.asar` không còn chuỗi `soar`; màn Home hiển thị “Welcome to Mix Client!”.
- MixUI đăng ký **136 mod**, có event bus, profile, HUD, account, music, quick-play và profile riêng.
- Mixin active: **106 mixin MixUI** trong `mixins.mixui.json` / `mixins.mixui-forge.json`.
- Đã xóa: `MixClient` singleton native, 82 module native, HUD/event/config native, 8 mixin legacy, engine `mix.ui.engine`, GL redirect, MSDF tooling, multi-test và profiler native.
- `compileJava`, `launcherClasses`, `dist` và build renderer Electron đã chạy xanh.
- Smoke runtime đã xanh ở cả **plain / Forge / OptiFine** với `MIXCLIENT_SMOKE_OK`.
- Smoke fullscreen 1920×1200 đã xanh ở cả **FBO bật** và **Forge + OptiFine Fast Render (FBO tắt)**, `glGetError() = 0`, không còn `GL_INVALID_VALUE`.
- Probe 8 frame liên tiếp cho thấy NanoVG giữ đúng FBO tường minh (`target=2`), hầu hết frame giống hệt; Esc đóng menu trong **4–5 ms**.
- OptiFine smoke có cảnh báo Dango bridge `available=false`; UI vẫn chạy và smoke hoàn tất.

## 2. Chạy nhanh

```bash
# compile client + launcher JAR
./gradlew compileJava launcherClasses --offline

# build toàn bộ vào build/dist
./gradlew dist --offline

# smoke MixUI qua UiCapture
./gradlew smokeTest --offline

# smoke Forge (cần profile Forge đã cài)
./gradlew smokeForge --offline

# smoke riêng cho Cosmetics + bốn module visual
JAVA_TOOL_OPTIONS="-Dmixclient.uicapture.visuals=true" ./gradlew smokeTest --offline
```

Launcher Electron:

```bash
cd launcher-desktop
npm run dev
npm run app
npm run dist:win
```

Yêu cầu: JDK 8 (`JAVA_HOME`), Node 20+. Gradle dùng wrapper `gradlew.bat`.

## 3. Entry point và bootstrap

- `mix.client.mixui.injection.mixin.mixins.client.MixinMinecraft` gọi `Mix.getInstance().start()` ở `startGame`.
- `Mix.start()` dựng toàn bộ manager, đăng ký 136 mod, MixHandler, Dango bridge và bật `GlobalSettingsMod`.
- `MixinGuiMainMenu` thay `GuiMainMenu` bằng `GuiMixMainMenu`.
- `GlobalSettingsMod` mở `GuiModMenu` bằng RShift.
- `mix.client.bootstrap.MixTweaker` là tweaker duy nhất:
  - plain: `mixins.mixui.json`, game args từ `mixclient.gameArgs`;
  - OptiFine: context `notch`, `mixins.mixui-forge.json`, không nhận game args;
  - Forge: context `searge`, `mixins.mixui-forge.json`, FML giữ quyền launch target;
  - đăng ký `LwjglTransformer` và mở khoá package `org.lwjgl`.

Manifest JAR khai báo `TweakClass` và `mixclient.bootstrap.json` rỗng để FML nhận diện jar Mixin; config thật được nạp động trong tweaker theo context.

## 4. Bản đồ thư mục

```text
src/main/java/mix/client/
  bootstrap/MixTweaker.java       tweaker plain / OptiFine / Forge
  mixui/
    Mix.java                     singleton, start/stop, manager
    MixAPI.java                  API menu/HUD
    debug/UiCapture.java         smoke screenshot sequence
    gui/                         main menu, mod menu, HUD editor, splash
    management/                  mods, profiles, events, accounts, music...
    injection/mixin/mixins/      106 mixin active (Forge HUD event included)
    injection/transformer/       LwjglTransformer

src/main/resources/
  mixclient.bootstrap.json      config Mixin rỗng cho manifest/FML
  mixins.mixui.json              plain / MCP
  mixins.mixui-forge.json        OptiFine / Forge + refmap
  assets/minecraft/mixclient/    font, language, cape, icon, sound MixUI
  assets/minecraft/shaders/      blur / color / motion shader
  assets/mixclient/targetinfo/   ảnh target info
  assets/dangomusic/             DangoMusic

src/launcher/java/mix/launcher/  Java launcher JAR
launcher-desktop/                Electron + React + Vite + Tailwind
tools/                           mapping, remap, update, capture tooling
libs/                            NanoVG/LWJGL3, JCEF, ViaVersion, Dango...
```

## 5. Build pipeline

Quy trình phát hành chuẩn: xem [RELEASING.md](RELEASING.md). `npm run pack` và
`npm run dist:win` hiện build lại client trước khi đóng gói; không dùng lại
`build/dist` của lần build trước. `npm run release:prepare -- --offline` chỉ
chuẩn bị và kiểm tra đầu ra, ghi hash vào `build/release-receipt.json`.

`build.gradle` dùng ForgeGradle 2.1, Mixingradle 0.6, Java 8.

| Task | Việc |
|---|---|
| `compileJava` | compile client MCP |
| `launcherClasses` / `launcherJar` | compile và đóng `MixClient-Launcher.jar` |
| `deobfClient` / `widenClientJar` | tạo MCP client jar và nới access |
| `remapClientNotch` | tạo `MixClient-notch.jar` |
| `remapShadowsSrg` / `remapShadowsNotch` | remap shadow cho MixUI |
| `remapDangoSrg` | tạo bản Dango cho Forge |
| `dist` | gom client, launcher, libs, OptiFine, Dango, `version.json` |
| `smokeTest` | chạy `LauncherMain --smoke-test` |
| `smokeForge` | chạy `LauncherMain --forge-smoke` |

`ShadowRemapper` chỉ xử lý prefix `mix/client/mixui/injection/`.

## 6. Smoke test MixUI

Kết quả kiểm tra ngày 25/11/2026 và các giới hạn hiện tại: xem
[SMOKE-REPORT.md](SMOKE-REPORT.md). Chạy lại cả ba chế độ bằng
`npm run smoke:matrix` từ `launcher-desktop` sau khi build `dist`.

`GameLauncher` bật:

```text
-Dmixclient.uicapture=true
-Dmixclient.uicapture.world=true
```

`UiCapture` tạo world phẳng, mở menu, chụp các màn Home/Modules/Settings/Music/Frost/Dango/HUD, in `MIXCLIENT_SMOKE_OK` rồi tắt game.

Dấu hiệu thành công:

```text
[MixClient] MIXCLIENT_SMOKE_OK
BUILD SUCCESSFUL
```

## 7. Launcher

- Electron dùng OAuth PKCE loopback → Xbox/XSTS/Minecraft, hỗ trợ offline/refresh và nhiều tài khoản.
- Renderer không được cấp Node trực tiếp; IPC đi qua `preload.cjs`.
- `LauncherMain --launch-file` đọc cấu hình rồi `GameLauncher` dựng classpath MCP/Notch/SRG.
- Forge chạy `FMLTweaker` trước `MixTweaker`; OptiFine chạy `MixTweaker` trước `OptiFineTweaker`.
- Swing launcher vẫn được giữ nhưng đã bỏ hoàn toàn UI profile native.

## 8. Lưu ý runtime

- MixUI lưu dữ liệu trong `<gameDir>/mixclient/`, không phải `config/mixclient/`.
- Khi OptiFine Fast Render tắt FBO, `BlurUtils` bỏ qua blur để tránh bind texture `-1`; launcher không còn ép `ofFastRender=true`.
- `NanoVGManager` không bind đè FBO mà `ScreenAnimation`/`ScreenAlpha`/`ScreenStencil` đã tạo; Esc đóng `GuiModMenu` ngay lập tức.
- Forge dùng `GuiIngameForge` override `renderGameOverlay`; `MixinGuiIngameForge` phát lại `EventRender2D` để HUD không bị mất hoàn toàn.
- Dango Island/Song Card dùng chung token màu, font, padding và chiều cao với HUD chính; Script HUD vẫn giữ màu do JSON người dùng định nghĩa.
- DynamicIsland/SongCard gốc của Dango bị chặn render bằng mixin; chỉ còn HUD do Mix tự vẽ. Time Changer không xuất hiện trong Array List HUD.
- Home lấy head từ skin account/session và launcher skin-cache theo nick; avatar dưới nút Edit HUD đã được xóa.
- `build/dist` là output cứng mà cả Java launcher và Electron cùng dùng.
- Dango bytecode đã được remap sang `mix.client.mixui.integration.dango.DangoI18n`; không còn package/shim upstream.
- Build lần đầu cần mạng để tải ForgeGradle/Minecraft/mapping; sau đó có thể dùng `--offline`.
- `mix.client.bootstrap.MixTweaker` phải nằm ngoài package `mix.client.mixui.injection.mixin` vì FML loại trừ package chứa tweaker khỏi classloader.

## 9. Cosmetics visual hub

- `CosmeticsCategory` có bảy tab: **Cape, Kill Effect, Hit Effect, Trail, Landing, Ambience, Particles**. Tab Cape giữ lại bộ lọc All/Minecon/Flag/Custom và hệ cape cũ.
- Bốn module wrapper: `TrailsMod`, `LandingImpactMod`, `AmbienceMod`, `ParticlesMod`; Kill Effect dùng `KillEffectsMod`, Hit Effect dùng `HitColorMod` + `DamageParticlesMod`.
- Runtime Furiah được port trong `mix.client.mixui.visuals.furiah`: đủ **24 Trail / 24 Landing / 12 Ambience** style, shared FX/trail/kit/render/material/stroke, texture thủ tục và sáu shader. Phần style/spline/simulation/render math giữ nguyên source; chỉ module/settings/event/bootstrap và GUI preview được thay bằng adapter MixUI.
- `FuriahSettingsPanel` đọc toàn bộ setting của style đang chọn; JSON gốc được lưu trong `TextSetting` `text.furiahsettings` của profile Mix.
- Render đi qua `Vfx`, `RenderState`, `Textures`, `FxPasses`, `MeshBatch` và shader của Furiah. `FuriahRuntime` giữ đúng thứ tự `nextFrame → prepare → solid → render → translucent/glow`.
- Particle hook dùng `ParticleSwaps.FvEffectRenderer/FvParticleEmitter` gán trực tiếp vào `Minecraft.effectRenderer`; không dùng Unsafe. Reflection có cả bảng `furiah_srg.txt` và `furiah_notch.txt`.
- Smoke visual chụp cả bảy tab, panel settings, trail, landing và particle burst; assert 24/24/12 style, bốn module bật, particle hook/renderer hoạt động, `glGetError() = 0` và profile thực sự chứa JSON settings.

Nguồn tham chiếu: Furiah Visuals tag `1.0`, commit `16b64075a7e8abfef593a5ac4aba7fae64a6f387` (`https://github.com/4everdies/furiahvisuals/releases/tag/1.0`). Đây là port runtime/style gần như nguyên bản, không phải bản dựng lại theo screenshot; GUI/preview và hook framework của Furiah được thay bằng MixUI. Repo tag không có file license được GitHub nhận diện; cần giữ attribution và không mặc định coi đây là MIT nếu sau này muốn phân phối lại các asset/style nguyên bản.

## 10. Nợ kỹ thuật còn lại

1. Kiểm tra Dango bridge trong OptiFine (`available=false` trong smoke).
2. Rà soát token plaintext trong `launch.json` và chữ ký manifest update.
3. Hai launcher Java Swing và Electron vẫn trùng logic; có thể archive Swing sau.
4. Working tree có nhiều thay đổi chưa commit; cần rà diff trước khi commit.
