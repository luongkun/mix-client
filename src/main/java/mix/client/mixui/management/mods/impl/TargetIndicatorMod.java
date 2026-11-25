package mix.client.mixui.management.mods.impl;

import java.awt.Color;

import mix.client.mixui.Mix;
import mix.client.mixui.management.color.AccentColor;
import mix.client.mixui.management.event.EventTarget;
import mix.client.mixui.management.event.impl.EventRender3D;
import mix.client.mixui.management.language.TranslateText;
import mix.client.mixui.management.mods.Mod;
import mix.client.mixui.management.mods.ModCategory;
import mix.client.mixui.management.mods.settings.impl.BooleanSetting;
import mix.client.mixui.management.mods.settings.impl.ColorSetting;
import mix.client.mixui.utils.ColorUtils;
import mix.client.mixui.utils.Render3DUtils;
import mix.client.mixui.utils.TargetUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * 目标指示器：锁定的目标玩家周围那一圈上下浮动的淡渐变环。
 *
 * <p><b>淡入淡出</b>：{@code TargetUtils.getTarget()} 一旦失去目标（目标离开视野 2.5 秒、
 * 走远超过 12 格、隐身、或自己或对方死亡）会立刻返回 {@code null}，环就「啪」地消失。
 * 这里用一个 0→1 的乘子把两种边界都做成渐变：</p>
 * <ul>
 *   <li><b>淡入</b>：拿到目标的那一帧从 0 开始涨；</li>
 *   <li><b>淡出</b>：目标变成 null 之后<b>仍然继续画最后那个实体</b>，把乘子降到 0 才真正停手。</li>
 * </ul>
 *
 * <p>为什么必须记住「最后一个目标」：失去目标是瞬间的，但玩家看到的应该是「环慢慢灭掉」
 * 而不是「环直接不见」。所以 {@code fadeOutTarget} 缓存住最后一个受影响的实体，
 * 乘子没归零之前都用它算坐标 —— 目标就算走出了视野，环也会留在它最后的位置上自然淡掉。</p>
 *
 * <p><b>为什么用固定时长而不是指数靠拢</b>：指数衰减（{@code 1 - exp(-k*dt)}）在头 20% 的
 * 时间里就走完 63%，剩下的长尾肉眼几乎看不出来，观感仍是「一瞬间就没了」。
 * 现在改成「进度条 + smoothstep」：进度按真实时间线性推进，走完 {@link #FADE_MS} 毫秒到端点，
 * 再用 {@code t*t*(3-2t)} 把两端斜率抹平。这样淡入淡出是真的能看见的，而且时长是一个
 * 直白的数字，想再慢一点只改这一个常量。</p>
 *
 * <p>乘子直接乘进颜色的 alpha 通道，不额外传参：{@link Render3DUtils#drawTargetIndicator}
 * 本来就用 {@code color.getAlpha()} 去缩放上/下沿的 alpha，所以这里改 alpha 就等于整体调透明度，
 * 而且几何、GL 状态、绘制通路一行都不用动。</p>
 */
public class TargetIndicatorMod extends Mod {

	/** 单程淡入或淡出的时长（毫秒）。380 ≈ 0.38 秒，够看清渐变又不会显得拖沓。 */
	private static final float FADE_MS = 380.0F;

	/** 进度小于这个值就吸附到 0（避免长尾的「永远差一点点」导致每帧都在画几乎透明的东西）。 */
	private static final float PROGRESS_EPSILON = 0.002F;

	/**
	 * 被方块挡住时的 alpha 乘数。0.38 与 nametags 的 {@code OCCLUDED_ALPHA} 同一个值 ——
	 * 这两处观感要一致，玩家不应该觉得"名字标签会淡、指示器不会"。
	 */
	private static final float OCCLUDED_ALPHA = 0.38F;

	/** 遮挡检测的间隔（毫秒）。120ms 与 nametags 一致：射线的开销不值得每帧都打，8 帧一次足够跟手。 */
	private static final long OCCLUSION_INTERVAL = 120L;

	/** 遮掩的渐变速度，与 nametags 的 FADE_SPEED 同值，避免两个功能的淡入淡出节奏不一样。 */
	private static final float OCCLUSION_FADE_SPEED = 4.5F;

	private BooleanSetting customColorSetting = new BooleanSetting(TranslateText.CUSTOM_COLOR, this, false);
	private ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, Color.RED, false);

	/** 线性进度：0 = 完全透明，1 = 完全不透明。真正的透明度再经 smoothstep 整形。 */
	private float progress = 0.0F;

	/** 淡出期间继续沿用的实体（目标已经变成 null 了）。 */
	private Entity fadeOutTarget;

	/** 上一帧的 System.currentTimeMillis()，用来算与帧率无关的增量。 */
	private long lastFrameMs;

	/** 遮挡检测的下一次时间戳。射线的开销不值得每帧都打，按 {@link #OCCLUSION_INTERVAL} 节流。 */
	private long nextOcclusionCheck;

	/** 上一次射线检测的结论：true = 被方块挡住。 */
	private boolean occluded;

	/** 遮挡遮掩的实际乘数，朝目标值（{@link #OCCLUDED_ALPHA} 或 1.0）平滑靠拢。 */
	private float occlusionAlpha = 1.0F;

	/** 上一帧的时间戳，用来算遮掩的 delta（与 nametags 的 lastUpdate 同口径）。 */
	private long lastOcclusionMs;

	public TargetIndicatorMod() {
		super(TranslateText.TARGET_INDICATOR, TranslateText.TARGET_INDICATOR_DESCRIPTION, ModCategory.RENDER);
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {

		Entity target = TargetUtils.getTarget();

		// 自己不该被自己的指示器罩住
		if (target != null && target.equals(mc.thePlayer)) {
			target = null;
		}

		if (target != null) {
			fadeOutTarget = target;
		}

		// 目标丢了但进度还没归零 → 用最后那个实体继续画，让它自然淡掉
		Entity render = target != null ? target : fadeOutTarget;

		if (render == null) {
			progress = 0.0F;
			lastFrameMs = 0L;
			return;
		}

		stepProgress(target != null);

		if (progress <= PROGRESS_EPSILON) {
			progress = 0.0F;
			// 淡出结束，忘掉那个实体，别一直攥着引用
			if (target == null) {
				fadeOutTarget = null;
			}
			lastFrameMs = 0L;
			return;
		}

		AccentColor currentColor = Mix.getInstance().getColorManager().getCurrentColor();
		Color base = customColorSetting.isToggled()
				? ColorUtils.applyAlpha(colorSetting.getColor(), 255)
				: currentColor.getInterpolateColor();

		// smoothstep：两端斜率都是 0，所以起步和收尾都不会有「突然跳一下」的感觉
		float eased = progress * progress * (3.0F - 2.0F * progress);

		// ── 遮挡遮掩 ────────────────────────────────────────────────────────────
		// 之前这条环是「穿墙可见」的：几何上它挂在世界坐标系里，但绘制时把 GL_DEPTH_TEST
		// 关掉了，于是不管前面挡着什么方块都照样糊在屏幕上。nametags 早就不是这个行为，
		// 两处摆在一起看就显得不统一。
		//
		// 这里做两件事，缺一不可：
		//   1. {@link Render3DUtils#drawTargetIndicator} 里改成 enable(DEPTH_TEST) +
		//      glDepthMask(false)，让 GPU 自己把被挡住的像素丢掉 —— 这是「遮掩」的本体；
		//   2. 这里再用射线判断整条环是不是完全躲在方块后面，是的话整体压到 0.38 的 alpha。
		// 光有 1 会出现「只露出一小条边，但那一小条亮得像没被挡」，光有 2 则挡一半的时候
		// 环会整条突然变淡，都不好看。两个一起才和 nametags 的观感一致。
		updateOcclusion(render);

		// 把淡入淡出 × 遮挡遮掩乘进 alpha：Render3DUtils 用 color.getAlpha() 缩放环的上/下沿
		// alpha，所以这一处改动等于整体调透明度，几何与 GL 状态都不用碰。
		int alpha = Math.round(base.getAlpha() * eased * occlusionAlpha);

		if (alpha <= 0) {
			return;
		}

		Render3DUtils.drawTargetIndicator(render, 0.67, ColorUtils.applyAlpha(base, alpha));
		GlStateManager.enableBlend();
	}

	/**
	 * 推进淡入淡出进度。
	 *
	 * <p>按真实时间增量线性推进（{@code delta / FADE_MS}），与帧率无关：
	 * 用固定步长的话，高帧率下每一次推进都很小、但次数更多，实际时长仍然正确 ——
	 * 而用「每帧固定比例」则会在高帧率下把过渡压缩到几十毫秒。</p>
	 */
	private void stepProgress(boolean hasTarget) {

		long now = System.currentTimeMillis();

		if (lastFrameMs == 0L) {
			lastFrameMs = now;
		}

		float deltaMs = (float) (now - lastFrameMs);
		lastFrameMs = now;

		// 掉帧或刚恢复时夹一下，免得一帧跳完整个渐变
		if (deltaMs <= 0.0F || deltaMs > 250.0F) {
			deltaMs = 16.0F;
		}

		float step = deltaMs / FADE_MS;
		float goal = hasTarget ? 1.0F : 0.0F;

		if (progress < goal) {
			progress += step;
			if (progress > goal) {
				progress = goal;
			}
		} else if (progress > goal) {
			progress -= step;
			if (progress < goal) {
				progress = goal;
			}
		}

		if (progress < PROGRESS_EPSILON) {
			progress = 0.0F;
		}
	}

	/**
	 * 更新遮挡结论，并把 {@link #occlusionAlpha} 朝目标值平滑推进。
	 *
	 * <p>射线按 {@link #OCCLUSION_INTERVAL} 节流：从自己眼睛打到目标碰撞箱六个面的中心，
	 * 任意一条没被方块挡住就算「没遮」——只打一个点的话，玩家站在掩体边缘时会把
	 * 「露出一半的目标」误判成完全被挡。</p>
	 */
	private void updateOcclusion(Entity target) {

		long now = System.currentTimeMillis();

		if (now >= nextOcclusionCheck) {
			occluded = isOccluded(target);
			nextOcclusionCheck = now + OCCLUSION_INTERVAL;
		}

		float targetAlpha = occluded ? OCCLUDED_ALPHA : 1.0F;

		// 与 nametags 完全同一套靠拢公式（含它的 delta 口径）：
		// 首次调用按 16ms 起步，之后按真实经过的秒数、上限 250ms，乘以 FADE_SPEED 后夹在 1 以内。
		// 用真实时间而不是固定 1/60，掉帧时不会让遮掩慢半拍；夹上限是防止切屏回来一次跳完。
		float delta = lastOcclusionMs == 0L
				? 0.016F
				: Math.min(0.25F, (now - lastOcclusionMs) / 1000.0F);

		lastOcclusionMs = now;

		occlusionAlpha += (targetAlpha - occlusionAlpha) * Math.min(1.0F, delta * OCCLUSION_FADE_SPEED);

		if (Math.abs(occlusionAlpha - targetAlpha) < 0.002F) {
			occlusionAlpha = targetAlpha;
		}
	}

	/**
	 * 目标是否被方块挡住。判据与 {@code NametagsMod.isOccluded} 同一套：
	 * 从自己眼睛出发，打到目标身上几个采样点，全被挡才算遮挡。
	 */
	private boolean isOccluded(Entity target) {

		if (mc.thePlayer == null || mc.theWorld == null || target == null) {
			return false;
		}

		Vec3 eye = new Vec3(mc.thePlayer.posX,
				mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

		AxisAlignedBB box = target.getEntityBoundingBox();

		// 六个面中心 + 几何中心，共七个采样点。只打中心点的话，半个身子露在掩体外的目标
		// 会被误判成完全被挡；只打头顶的话，蹲在矮墙后的目标又永远算「没挡」。
		double[][] samples = new double[][] {
				{ (box.minX + box.maxX) / 2D, (box.minY + box.maxY) / 2D, (box.minZ + box.maxZ) / 2D },
				{ (box.minX + box.maxX) / 2D, box.maxY, (box.minZ + box.maxZ) / 2D },
				{ (box.minX + box.maxX) / 2D, box.minY, (box.minZ + box.maxZ) / 2D },
				{ box.minX, (box.minY + box.maxY) / 2D, (box.minZ + box.maxZ) / 2D },
				{ box.maxX, (box.minY + box.maxY) / 2D, (box.minZ + box.maxZ) / 2D },
				{ (box.minX + box.maxX) / 2D, (box.minY + box.maxY) / 2D, box.minZ },
				{ (box.minX + box.maxX) / 2D, (box.minY + box.maxY) / 2D, box.maxZ },
		};

		for (double[] sample : samples) {

			Vec3 point = new Vec3(sample[0], sample[1], sample[2]);

			// false, true, false = 不忽略液体、检测可碰撞方块、不忽略无碰撞方块 —— 与 nametags 同参
			MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eye, point, false, true, false);

			if (hit == null) {
				// 有一条射线通畅，就说明至少露出一部分，不算完全被遮
				return false;
			}
		}

		return true;
	}
}
