#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform vec2 BlurXY;
uniform vec2 BlurCoord;
uniform float Radius;

// ── 渐变磨砂遮罩 ────────────────────────────────────────────────────────────
// MaskY.x / MaskY.y = 屏幕纵向比例（0 顶 / 1 底），遮罩从 top 到 bottom 由强到弱。
// MaskStrength <= 0 表示不启用遮罩，行为退回原来的「矩形内模糊、矩形外原图」。
uniform vec2 MaskY;
uniform float MaskStrength;

// 1 = 上下翻转（用来纠正不同 vertex shader 的 texCoord 朝向，不用改着色器重编）。
uniform float MaskInvert;

float SCurve (float x) {

    // ---- by CeeJayDK
    x = x * 2.0 - 1.0;
    return -x * abs(x) * 0.5 + x + 0.5;
}

vec4 BlurH (sampler2D source, vec2 size, vec2 uv, float radius) {

    if (uv.x / oneTexel.x >= BlurXY.x * 2 && uv.y / oneTexel.y >= BlurXY.y * 2 && uv.x / oneTexel.x <= (BlurCoord.x + BlurXY.x) * 2 && uv.y / oneTexel.y <= (BlurCoord.y + BlurXY.y) * 2)
    {
        vec4 A = vec4(0.0);
        vec4 C = vec4(0.0);

        float divisor = 0.0;
        float weight = 0.0;

        float radiusMultiplier = 1.0 / radius;

        for (float x = -radius; x <= radius; x++)
        {
            A = texture2D(source, uv + vec2(x * size) * BlurDir);

            weight = SCurve(1.0 - (abs(x) * radiusMultiplier));

            C += A * weight;

            divisor += weight;
        }

        return vec4(C.r / divisor, C.g / divisor, C.b / divisor, 1.0);
    }

    return texture2D(source, uv);
}

void main() {

    vec4 source = texture2D(DiffuseSampler, texCoord);
    vec4 blurred = BlurH(DiffuseSampler, oneTexel, texCoord, Radius);

    // 没有遮罩：整块直接换成模糊结果（等价旧行为）。
    if (MaskStrength <= 0.0001) {
        gl_FragColor = blurred;
        return;
    }

    // 有遮罩：按屏幕纵向位置把「原图」与「模糊图」插值。
    // fromTop = 0 在屏幕顶，1 在屏幕底。
    float fromTop = texCoord.y;

    if (MaskInvert < 0.5) {
        fromTop = 1.0 - fromTop;
    }

    // smoothstep 两端斜率为 0 —— 遮罩本身没有任何硬边。
    float t = smoothstep(MaskY.x, MaskY.y, fromTop);

    // t = 0 → 全模糊（顶部磨砂最强），t = 1 → 原图（磨砂完全化开）。
    gl_FragColor = vec4(mix(blurred.rgb, source.rgb, clamp(t * MaskStrength, 0.0, 1.0)), 1.0);
}
