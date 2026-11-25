package mix.client.mixui.integration.dango;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mix.client.mixui.logger.MixLogger;
import net.minecraft.util.ResourceLocation;

/**
 * DangoMusic 内建播放器的只读/控制封装。
 *
 * <p>DangoMusic 的类名是 Kotlin 编译后的 MCP 形态（{@code me.sorihiko.dango.music.MusicPlayer}，
 * Kotlin {@code object}），Mix 侧没有它编译期的 jar，所以这里全部走反射，和
 * {@link DangoBridge} 保持同一套策略：类不在就返回安全默认值，绝不让 HUD 渲染抛异常。</p>
 *
 * <p>为什么不直接读字节码改 Dango 的模块：胶囊「没有背景」的根因是
 * {@code glass.frag} 里根本没有轮廓描边，只有一圈很弱的 rim 光，在浅色天空上
 * 与背后的磨砂糊成一片。与其在那套 canvas 管线上打补丁，不如在 Mix 侧用
 * NanoVG 重画一套 HUD（{@code DangoIslandMod} / {@code DangoSongCardMod}），
 * 顺便得到 1:1 的描边控制与统一的主题色。</p>
 */
public final class DangoMusicApi {

    private static boolean resolved;
    private static boolean available;

    private static Object player;
    private static Object coverCache;
    private static Method getState;
    private static Method getCurrent;
    private static Method getQueue;
    private static Method getIndex;
    private static Method getPositionMs;
    private static Method getDurationMs;
    private static Method getActive;
    private static Method getVolume;
    private static Method setVolume;
    private static Method togglePause;
    private static Method nextTrack;
    private static Method prevTrack;
    private static Method seek;
    private static Method playIndex;
    private static Method stopTrack;
    private static Method playQueue;
    private static Method coverUpload;
    private static Method coverTrack;
    private static Method trackName;
    private static Method trackArtists;
    private static Method trackDuration;
    private static Class<?> textureClass;
    private static Method textureLocation;

    // 网易云远端（搜索 / 登录态）。和播放器分开绑：播放器在，搜索也可能因为没登录而不可用。
    private static Object neteaseApi;
    private static Method isLoggedIn;
    private static Method nickname;
    private static Method lastSearchError;
    private static Method search;

    // 歌单（推荐 / 榜单 / 我创建的）。音乐页在「队列为空」时用它们填版面，
    // 用户不用再进 Dango 那套整屏界面才能点歌。
    private static Method recommendPlaylists;
    private static Method toplists;
    private static Method userPlaylists;
    private static Method playlistTracks;
    private static Method playlistName;
    private static Method playlistTrackCount;

    // 单曲封面：搜索结果和歌单曲目都走同一个 CoverCache，取到的 ResourceLocation 直接画。


    private DangoMusicApi() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        ClassLoader loader = DangoMusicApi.class.getClassLoader();

        // 每一段单独兜住：Dango 的类树里只要有一个方法签名和预期不同，
        // 不能让整段绑定一起失效。
        Class<?> trackClass = null;

        try {
            Class<?> playerClass = Class.forName("me.sorihiko.dango.music.MusicPlayer", true, loader);
            player = playerClass.getField("INSTANCE").get(null);
            getState = playerClass.getMethod("getState");
            getCurrent = playerClass.getMethod("getCurrent");
            getQueue = playerClass.getMethod("getQueue");
            getIndex = playerClass.getMethod("getIndex");
            getPositionMs = playerClass.getMethod("getPositionMs");
            getDurationMs = playerClass.getMethod("getDurationMs");
            getActive = playerClass.getMethod("getActive");
            getVolume = playerClass.getMethod("getVolume");
            setVolume = playerClass.getMethod("setVolume", float.class);
            togglePause = playerClass.getMethod("togglePause");
            nextTrack = playerClass.getMethod("next");
            prevTrack = playerClass.getMethod("prev");
            seek = playerClass.getMethod("seek", long.class);
            playIndex = playerClass.getMethod("playIndex", int.class);
            stopTrack = playerClass.getMethod("stop");
            playQueue = playerClass.getMethod("playQueue", java.util.List.class, int.class);
        } catch (Throwable t) {
            MixLogger.warn("[dango] 播放器绑定失败: " + t);
        }

        try {
            trackClass = Class.forName("me.sorihiko.dango.music.NeteaseTrack", true, loader);
            trackName = trackClass.getMethod("getName");
            trackArtists = trackClass.getMethod("getArtists");
            trackDuration = trackClass.getMethod("getDurationMs");
        } catch (Throwable t) {
            MixLogger.warn("[dango] 曲目绑定失败: " + t);
        }

        try {
            Class<?> coverClass = Class.forName("me.sorihiko.dango.music.CoverCache", true, loader);
            coverCache = coverClass.getField("INSTANCE").get(null);
            coverUpload = coverClass.getMethod("upload");
            coverTrack = trackClass == null ? null : coverClass.getMethod("track", trackClass);
        } catch (Throwable t) {
            MixLogger.warn("[dango] 封面绑定失败: " + t);
        }

        try {
            Class<?> apiClass = Class.forName("me.sorihiko.dango.music.NeteaseApi", true, loader);
            neteaseApi = apiClass.getField("INSTANCE").get(null);
            isLoggedIn = apiClass.getMethod("getLoggedIn");
            nickname = apiClass.getMethod("getNickname");
            lastSearchError = apiClass.getMethod("getLastSearchError");
            search = apiClass.getMethod("search", String.class, int.class);
        } catch (Throwable t) {
            MixLogger.warn("[dango] 网易云接口绑定失败: " + t);
        }

        // 歌单相关的几个方法单独兜：它们的返回类型带 NeteasePlaylist，
        // 一旦哪天签名变了，不能让搜索 / 播放一起失效。
        try {
            Class<?> apiClass = Class.forName("me.sorihiko.dango.music.NeteaseApi", true, loader);
            Class<?> playlistClass = Class.forName("me.sorihiko.dango.music.NeteasePlaylist", true, loader);
            recommendPlaylists = apiClass.getMethod("recommendPlaylists");
            toplists = apiClass.getMethod("toplists");
            userPlaylists = apiClass.getMethod("userPlaylists");
            playlistTracks = apiClass.getMethod("playlistTracks", long.class);
            playlistName = playlistClass.getMethod("getName");
            playlistTrackCount = playlistClass.getMethod("getTrackCount");
        } catch (Throwable t) {
            MixLogger.warn("[dango] 歌单接口绑定失败: " + t);
        }

        // gfx.Texture 是空接口，getLocation() 只存在于具体实现（legacy.LegacyTexture）上，
        // 所以这里不做静态解析，等 coverOf 拿到实例时再按实际类找一次并缓存。
        textureLocation = null;
        textureClass = null;

        available = player != null;
        MixLogger.info("[dango] 播放器接口已绑定 available=" + available);
    }

    /** 懒解析 {更新: Texture 的实现类} 上的 getLocation()，只试一次。 */
    private static Method textureLocationOf(Object texture) {
        if (textureLocation != null) {
            return textureLocation;
        }
        if (texture == null) {
            return null;
        }
        try {
            Method m = texture.getClass().getMethod("getLocation");
            textureLocation = m;
            return m;
        } catch (Throwable t) {
            MixLogger.warn("[dango] 纹理取地址失败: " + t);
            return null;
        }
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /** 歌曲是否处于「正在播」或「暂停」这种有内容的状态。 */
    public static boolean hasTrack() {
        return current() != null;
    }

    /** 播放器状态名：STOPPED / LOADING / PLAYING / PAUSED / ERROR。 */
    public static String stateName() {
        resolve();
        if (getState == null || player == null) {
            return "STOPPED";
        }
        try {
            Object state = getState.invoke(player);
            return state == null ? "STOPPED" : String.valueOf(state);
        } catch (Throwable t) {
            return "STOPPED";
        }
    }

    public static boolean isPlaying() {
        return "PLAYING".equals(stateName());
    }

    public static boolean isPaused() {
        return "PAUSED".equals(stateName());
    }

    /**
     * 网易云那一路的音量，0.0 ~ 1.0。
     *
     * <p>Dango 的 {@code MusicPlayer} 把音量存成一个 {@code static volatile float}，
     * 混音线程每帧比对它和上一次应用过的值，变了就重新写 {@code SourceDataLine}
     * （MusicPlayer.java:479-481）。也就是说「改电平」这一侧本来就支持实时调节，
     * 只是 Mix 的音乐页从来没有把它接进去——滑条只喂给了本地 JavaFX 那个播放器，
     * 于是网易云在播时拖动音量毫无反应，必须跑去 Dango 自己的界面才有用。</p>
     *
     * <p>返回 -1 表示这一路读不到（Dango 没绑定成功），调用方据此决定要不要回写。</p>
     */
    public static float getVolume() {

        resolve();

        if (getVolume == null || player == null) {
            return -1.0F;
        }

        try {
            Object value = getVolume.invoke(player);
            return value instanceof Number ? ((Number) value).floatValue() : -1.0F;
        } catch (Throwable t) {
            return -1.0F;
        }
    }

    /**
     * 设置网易云那一路的音量。
     *
     * <p>只写这个静态字段、不主动触发 {@code applyVolume}：混音线程本来就在跑
     * （{@code MusicPlayer.java:479}），下一次循环会自己发现差异并落下去。
     * 自己再反射去调 private 的 {@code applyVolume(SourceDataLine, float)} 反而危险——
     * 那需要一个 {@code SourceDataLine} 参数，得先反射挖出播放线程里的私有字段，
     * 挖错了就是一条 {@code IllegalArgumentException} 打在渲染线程上。</p>
     *
     * <p>钳到 [0, 1]：滑条本身不会越界，但这里被别处调时不该把播放器写进非法值。</p>
     */
    public static void setVolume(float volume) {

        resolve();

        if (setVolume == null || player == null) {
            return;
        }

        try {
            float v = Math.max(0.0F, Math.min(1.0F, volume));
            setVolume.invoke(player, v);
        } catch (Throwable t) {
            // 音量调不动不该影响界面
        }
    }

    /** 当前曲目；没有播放内容时返回 null。 */
    public static Object current() {
        resolve();
        if (getCurrent == null || player == null) {
            return null;
        }
        try {
            return getCurrent.invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String currentName() {
        return nameOf(current());
    }

    public static String currentArtists() {
        return artistsOf(current());
    }

    public static long positionMs() {
        resolve();
        try {
            return getPositionMs == null ? 0L : ((Number) getPositionMs.invoke(player)).longValue();
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static long durationMs() {
        resolve();
        try {
            return getDurationMs == null ? 0L : ((Number) getDurationMs.invoke(player)).longValue();
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static float progress() {
        long total = durationMs();
        if (total <= 0L) {
            return 0.0F;
        }
        long now = positionMs();
        if (now < 0L) {
            now = 0L;
        }
        return Math.min(1.0F, (float) now / (float) total);
    }

    public static List<Object> queue() {
        resolve();
        if (getQueue == null || player == null) {
            return Collections.emptyList();
        }
        try {
            Object value = getQueue.invoke(player);
            if (value instanceof List) {
                List<?> raw = (List<?>) value;
                List<Object> out = new ArrayList<Object>(raw.size());
                out.addAll(raw);
                return out;
            }
        } catch (Throwable t) {
            // 队列不可读时当作空，HUD 只需要显示当前曲目
        }
        return Collections.emptyList();
    }

    public static int queueIndex() {
        resolve();
        try {
            return getIndex == null ? -1 : ((Number) getIndex.invoke(player)).intValue();
        } catch (Throwable t) {
            return -1;
        }
    }

    public static void togglePlayPause() {
        resolve();
        try {
            if (togglePause != null && player != null) {
                togglePause.invoke(player);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] togglePause 失败: " + t);
        }
    }

    public static void playNext() {
        resolve();
        try {
            if (nextTrack != null && player != null) {
                nextTrack.invoke(player);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] next 失败: " + t);
        }
    }

    public static void playPrev() {
        resolve();
        try {
            if (prevTrack != null && player != null) {
                prevTrack.invoke(player);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] prev 失败: " + t);
        }
    }

    public static void playAt(int index) {
        resolve();
        try {
            if (playIndex != null && player != null) {
                playIndex.invoke(player, Integer.valueOf(index));
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] playIndex(" + index + ") 失败: " + t);
        }
    }

    /**
     * 彻底停止播放并清空当前曲目（不只是暂停）。
     *
     * <p>关掉菜单时用：用户描述「播放音乐后退出视觉进入菜单界面时还是会播放」，
     * 要的是离开界面就安静，而不是留一个能在后台继续出声的暂停态。
     * {@code MusicPlayer.stop()} 会把 state 打回 STOPPED 并关掉音频行。</p>
     */
    public static void stop() {
        resolve();
        try {
            if (stopTrack != null && player != null) {
                stopTrack.invoke(player);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] stop 失败: " + t);
        }
    }

    /**
     * 把一整队曲目塞进 Dango 的播放器并从指定下标开始播。
     *
     * <p>音乐页的「网易云」列表就是 Dango 的队列，双击某行时如果直接用
     * {@code playAt}，Dango 只会跳它自己已有的队列下标；从搜索结果里现点一首时
     * 队列是空的，必须先 playQueue 才能播。这里把两件事合成一次调用。</p>
     */
    public static void playQueue(java.util.List<?> tracks, int index) {
        resolve();
        if(tracks == null || tracks.isEmpty()) {
            return;
        }
        int safe = Math.max(0, Math.min(index, tracks.size() - 1));
        try {
            if (playQueue != null && player != null) {
                playQueue.invoke(player, new java.util.ArrayList<Object>(tracks), Integer.valueOf(safe));
                return;
            }
            if (playIndex != null && player != null) {
                playIndex.invoke(player, Integer.valueOf(safe));
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] playQueue(" + safe + ") 失败: " + t);
        }
    }

    /** 网易云是否已登录（登录态由 DangoMusic 自己维护，这里只读）。 */
    public static boolean isNeteaseLoggedIn() {
        resolve();
        try {
            return isLoggedIn != null && neteaseApi != null
                    && Boolean.TRUE.equals(isLoggedIn.invoke(neteaseApi));
        } catch (Throwable t) {
            return false;
        }
    }

    /** 登录昵称；未登录返回 null。 */
    public static String neteaseNickname() {
        resolve();
        try {
            Object value = nickname == null || neteaseApi == null ? null : nickname.invoke(neteaseApi);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 上一次搜索失败的原因（网易云自己给的文案），成功时为 null。 */
    public static String lastSearchError() {
        resolve();
        try {
            Object value = lastSearchError == null || neteaseApi == null ? null : lastSearchError.invoke(neteaseApi);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 搜歌：直接在音乐页里搜，不用再跳进 Dango 的完整界面。
     *
     * <p>返回的每一首都是 {@code NeteaseTrack}，可以直接交给
     * {@link #playQueue(List, int)} 开播——队里没有它也能播，这是「精简版并入
     * 音乐页」的关键：不搬 Dango 的控件，只搬它的数据。</p>
     */
    @SuppressWarnings("unchecked")
    public static List<Object> search(String keyword, int limit) {
        resolve();
        if (neteaseApi == null || search == null) {
            return Collections.emptyList();
        }
        String text = keyword == null ? "" : keyword.trim();
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Object value = search.invoke(neteaseApi, text, Integer.valueOf(Math.max(1, limit)));
            if (value instanceof List) {
                return new ArrayList<Object>((List<Object>) value);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] search(\"" + text + "\") 失败: " + t);
        }
        return Collections.emptyList();
    }

    /**
     * 推荐歌单 + 榜单 + 我创建的，合成一张去重的列表。
     *
     * <p>音乐页在「队列空了」的时候拿它铺版面：点一个歌单就看曲目，
     * 点曲目直接播——这就是把 Dango 那套界面「精简并入」的方式，
     * 不搬它的控件，只搬它的数据。</p>
     */
    public static List<Object> playlists() {
        resolve();
        List<Object> out = new ArrayList<Object>();
        if (neteaseApi == null) {
            return out;
        }
        collectPlaylists(out, recommendPlaylists);
        collectPlaylists(out, toplists);
        collectPlaylists(out, userPlaylists);
        return out;
    }

    private static void collectPlaylists(List<Object> out, Method method) {
        if (method == null) {
            return;
        }
        try {
            Object value = method.invoke(neteaseApi);
            if (!(value instanceof List)) {
                return;
            }
            for (Object item : (List<?>) value) {
                if (item == null || out.contains(item)) {
                    continue;
                }
                out.add(item);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] 取歌单失败: " + t);
        }
    }

    /**
     * 只有推荐歌单。首页把「推荐 / 榜单」画成两个分组，合并表分不出归属。
     *
     * <p>失败返回空表，绝不抛：这是网络操作，绘制线程不能因为网易云抽风而崩。</p>
     */
    public static List<Object> recommendedPlaylists() {
        resolve();
        List<Object> out = new ArrayList<Object>();
        if (neteaseApi == null) {
            return out;
        }
        collectPlaylists(out, recommendPlaylists);
        return out;
    }

    /** 只有榜单。首页第二个分组用，理由同上。 */
    public static List<Object> chartPlaylists() {
        resolve();
        List<Object> out = new ArrayList<Object>();
        if (neteaseApi == null) {
            return out;
        }
        collectPlaylists(out, toplists);
        return out;
    }

    /** 歌单名字；拿不到时返回空串（界面上会退化成「未命名歌单」）。 */
    public static String playlistNameOf(Object playlist) {
        resolve();
        if (playlist == null || playlistName == null) {
            return "";
        }
        try {
            Object value = playlistName.invoke(playlist);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable t) {
            return "";
        }
    }

    /** 歌单里的曲目数；拿不到时返回 0。 */
    public static int playlistTrackCount(Object playlist) {
        resolve();
        if (playlist == null || playlistTrackCount == null) {
            return 0;
        }
        try {
            Object value = playlistTrackCount.invoke(playlist);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * 网易云「我喜欢的音乐」。
     *
     * <p>它是 {@code specialType=5} 的系统歌单，但 Dango 的 {@code NeteasePlaylist}
     * 只暴露 id/name/trackCount/coverUrl/owned，specialType 在构造时就被丢掉了。
     * 所以只能退化成「我创建的歌单里按名字找」——中文是「喜欢的音乐」，
     * 英文/日文账号是 Liked / Favorite / お気に入り，几语都匹配一次。</p>
     *
     * <p>返回 null 而不是空表：调用方要区分「没登录 / 没有这条歌单」和
     * 「歌单存在但里面没歌」，前者该提示去登录。</p>
     */
    public static Object likedPlaylist() {
        resolve();
        if (neteaseApi == null || userPlaylists == null) {
            return null;
        }
        try {
            Object value = userPlaylists.invoke(neteaseApi);
            if (!(value instanceof List)) {
                return null;
            }
            for (Object item : (List<?>) value) {
                if (item == null) {
                    continue;
                }
                String name = playlistNameOf(item);
                if (name == null) {
                    continue;
                }
                if (name.contains("喜欢") || name.contains("Liked") || name.contains("liked")
                        || name.contains("Favorite") || name.contains("favorite")
                        || name.contains("お気に入り")) {
                    return item;
                }
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] 取「我喜欢的音乐」失败: " + t);
        }
        return null;
    }

    /** 「我喜欢的音乐」里的曲目；没登录或找不到这条歌单时返回空表。 */
    public static List<Object> likedTracks() {
        Object playlist = likedPlaylist();
        if (playlist == null) {
            return Collections.emptyList();
        }
        return tracksOfPlaylist(playlist);
    }

    /** 歌单 ID：拿不到返回 -1，调用方据此跳过。 */
    public static long playlistIdOf(Object playlist) {
        if (playlist == null) {
            return -1L;
        }
        try {
            Object value = playlist.getClass().getMethod("getId").invoke(playlist);
            return value instanceof Number ? ((Number) value).longValue() : -1L;
        } catch (Throwable t) {
            return -1L;
        }
    }

    /** 拉一个歌单的全部曲目。失败返回空表——这是网络操作，必须放到后台线程调。 */
    @SuppressWarnings("unchecked")
    public static List<Object> tracksOfPlaylist(Object playlist) {
        resolve();
        if (neteaseApi == null || playlistTracks == null) {
            return Collections.emptyList();
        }
        long id = playlistIdOf(playlist);
        if (id < 0L) {
            return Collections.emptyList();
        }
        try {
            Object value = playlistTracks.invoke(neteaseApi, Long.valueOf(id));
            if (value instanceof List) {
                return new ArrayList<Object>((List<Object>) value);
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] 拉歌单曲目失败: " + t);
        }
        return Collections.emptyList();
    }

    public static void seekTo(long ms) {
        resolve();
        try {
            if (seek != null && player != null) {
                seek.invoke(player, Long.valueOf(ms));
            }
        } catch (Throwable t) {
            MixLogger.warn("[dango] seek 失败: " + t);
        }
    }

    public static String nameOf(Object track) {
        resolve();
        if (track == null || trackName == null) {
            return null;
        }
        try {
            Object value = trackName.invoke(track);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static String artistsOf(Object track) {
        resolve();
        if (track == null || trackArtists == null) {
            return "";
        }
        try {
            Object value = trackArtists.invoke(track);
            if (value instanceof List) {
                List<String> names = (List<String>) value;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0) {
                        sb.append(" / ");
                    }
                    sb.append(names.get(i));
                }
                return sb.toString();
            }
        } catch (Throwable t) {
            // 忽略：没有歌手信息就画空串
        }
        return "";
    }

    public static long durationOf(Object track) {
        resolve();
        try {
            return track == null || trackDuration == null ? 0L
                    : ((Number) trackDuration.invoke(track)).longValue();
        } catch (Throwable t) {
            return 0L;
        }
    }

    /**
     * 把某一首歌的封面推进 GPU 并取出它的 {@link ResourceLocation}。
     *
     * <p>Dango 的封面是运行期上传的动态纹理（{@code LegacyTexture}），资源包里没有，
     * 所以只能拿它已经注册到 TextureManager 的 ResourceLocation，再交给 Mix 的
     * NanoVG 画。每帧先调 {@code upload()}，让 worker 线程解好的 BufferedImage
     * 落到 GL 上，否则永远只能拿到占位图。</p>
     */
    public static ResourceLocation coverOf(Object track) {
        resolve();
        if (track == null || coverCache == null || coverTrack == null) {
            return null;
        }
        try {
            coverUpload.invoke(coverCache);
            Object texture = coverTrack.invoke(coverCache, track);
            if (texture == null) {
                return null;
            }
            Method locationGetter = textureLocationOf(texture);
            if (locationGetter == null) {
                return null;
            }
            Object location = locationGetter.invoke(texture);
            return location instanceof ResourceLocation ? (ResourceLocation) location : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 让封面 worker 的产物进 GL；不调用 {@link #coverOf} 时也要在每帧调一次。 */
    public static void uploadCovers() {
        resolve();
        try {
            if (coverUpload != null && coverCache != null) {
                coverUpload.invoke(coverCache);
            }
        } catch (Throwable ignored) {
            // 封面失败不能影响 HUD 的其他部分
        }
    }

}