package mix.client.mixui.management.music;

import mix.client.mixui.Mix;
import mix.client.mixui.integration.dango.DangoMusicApi;

/**
 * 音乐播放的时机闸门：进主菜单暂停，进世界才恢复。
 *
 * <p>为什么需要单独一个类：原来这套逻辑写在 {@code GuiMixMainMenu} 里，
 * 恢复点是 {@code onGuiClosed()}。但「关掉主菜单」并不等于「开始玩游戏」——
 * 点一下「单人游戏」进的是存档列表，点「多人游戏」进的是服务器列表，
 * 玩家还在挑，音乐就已经响起来了。真正该开始的时机是<b>世界加载</b>
 * （{@code EventLoadWorld}，出自 {@code GuiDownloadTerrain.initGui}），
 * 也就是单人存档 / 服务器连接都已经选定之后。</p>
 *
 * <p>所以这里把「暂停」和「恢复」拆成两个独立入口：
 * {@link #pauseForMenu()} 由主菜单调，{@link #resumeOnWorldJoin()} 由世界加载调。
 * 主菜单关掉时<b>什么都不做</b>——欠着的那一路继续欠着，一直留到真的进世界为止。</p>
 *
 * <p>为什么用位掩码而不是布尔：本地歌单（JavaFX {@code MediaPlayer}）与
 * 网易云（Dango 的 {@code MusicPlayer}）是两条完全独立的播放器，
 * 完全可能「本地在播、网易云被用户手动暂停」。单个布尔会把后者一起恢复掉。
 * 掩码只记「哪一路是本闸门按下去的」，恢复时也只动那几路。</p>
 */
public final class MusicGate {

    /** 本地音乐文件夹那一路。 */
    private static final int LOCAL = 1;

    /** 网易云那一路。 */
    private static final int DANGO = 1 << 1;

    /**
     * 欠着的恢复位。
     *
     * <p>不带 volatile：只被渲染线程（主菜单 {@code initGui}）与客户端线程
     * （世界加载事件）访问，两者在 Minecraft 里本来就是同一个线程。</p>
     */
    private static int pending;

    private MusicGate() {
    }

    /**
     * 主菜单露出时把正在播的音乐按下去，并记下「欠哪几路」。
     *
     * <p>只认「正在播」：用户自己手动暂停的那一路本来就不是 PLAYING，
     * 既不会被再按一次，也不会占到对应的位，将来自然也不会被复活。</p>
     *
     * <p>用 {@code |=} 累加而不是覆盖：窗口 resize 会重走 {@code initGui}
     * 但没有配对的关闭回调，直接赋值会把先前记下的位抹掉。</p>
     */
    public static void pauseForMenu() {

        try {
            MusicManager musicManager = Mix.getInstance().getMusicManager();
            if(musicManager != null && musicManager.isPlaying()) {
                musicManager.pause();
                pending |= LOCAL;
            }
        } catch (Throwable t) {
            // 音乐模块还没初始化时不该影响菜单打开
        }

        try {
            if(DangoMusicApi.isAvailable() && DangoMusicApi.isPlaying()) {
                DangoMusicApi.togglePlayPause();
                pending |= DANGO;
            }
        } catch (Throwable t) {
            // 同上
        }
    }

    /**
     * 世界加载完成时把欠下的那几路恢复回来。
     *
     * <p>幂等：恢复一次就把欠账清零，同一次进入世界不会被
     * {@code EventLoadWorld} 与 {@code EventJoinServer} 各触发一次而重复 play。</p>
     *
     * <p>恢复前重新判状态：就算占了位，只要用户在这期间把播放状态改掉了
     * （本地被 stop、网易云被切到播放中），这里就不会再动它。</p>
     */
    public static void resumeOnWorldJoin() {

        if(pending == 0) {
            return;
        }

        int mask = pending;
        pending = 0;

        if((mask & LOCAL) != 0) {
            try {
                Mix.getInstance().getMusicManager().resume();
            } catch (Throwable t) {
                // 同上
            }
        }

        if((mask & DANGO) != 0) {
            try {
                if(DangoMusicApi.isAvailable() && DangoMusicApi.isPaused()) {
                    DangoMusicApi.togglePlayPause();
                }
            } catch (Throwable t) {
                // 同上
            }
        }
    }
}
