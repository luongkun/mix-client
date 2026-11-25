import { useEffect, useRef } from 'react';

/* Skin renderer.

   A skin is a 64×64 texture with one rectangle per body face. Drawing the front
   face plus a sheared side and top face gives the same read as a 3D preview
   without a 3D engine: the shear is a plain canvas transform, so the texture
   stays pixel-exact at any size. */

const PARTS = [
  { key: 'legR', x: -4, y: 0, w: 4, h: 12, front: [4, 20], side: [0, 20], top: [4, 16], sideW: 4, topH: 4 },
  { key: 'legL', x: 0, y: 0, w: 4, h: 12, front: [20, 52], side: [16, 52], top: [20, 48], sideW: 4, topH: 4 },
  { key: 'body', x: -4, y: 12, w: 8, h: 12, front: [20, 20], side: [16, 20], top: [20, 16], sideW: 4, topH: 4 },
  { key: 'armR', x: -8, y: 12, w: 4, h: 12, front: [44, 20], side: [40, 20], top: [44, 16], sideW: 4, topH: 4 },
  { key: 'armL', x: 4, y: 12, w: 4, h: 12, front: [36, 52], side: [32, 52], top: [36, 48], sideW: 4, topH: 4 },
  { key: 'head', x: -4, y: 24, w: 8, h: 8, front: [8, 8], side: [0, 8], top: [8, 0], sideW: 8, topH: 8,
    hat: { front: [40, 8], side: [32, 8], top: [40, 0] } }
];

const HEAD_ONLY = [PARTS[5]];
const SKEW = 0.42;

export default function SkinPreview({
  skin,
  slimModel = false,
  rotated = false,
  scale = 11,
  headOnly = false,
  className = ''
}) {
  const canvas = useRef(null);

  useEffect(() => {
    const element = canvas.current;
    if (!element) {
      return;
    }
    const ctx = element.getContext('2d');
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, element.width, element.height);
    if (!skin) {
      return;
    }

    const image = new Image();
    image.onload = () => {
      const unit = scale;
      const w = element.width;
      const h = element.height;
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, w, h);
      ctx.imageSmoothingEnabled = false;

      const parts = headOnly ? HEAD_ONLY : PARTS;
      const dx = SKEW * unit;
      const dy = SKEW * unit;
      const centerX = w / 2;
      /* head-only thumbnails are drawn around the head box (y 24..32) */
      const bottom = headOnly ? 32 * unit : h - unit * 1.5;

      const face = (sx, sy, sw, sh, px, py, pw, ph) => {
        ctx.drawImage(image, sx, sy, sw, sh, px, py, pw, ph);
      };

      /* sheared quad: unit square mapped onto a parallelogram */
      const sheared = (sx, sy, sw, sh, a, b, c, d, e, f, shade) => {
        ctx.save();
        ctx.setTransform(a, b, c, d, e, f);
        ctx.drawImage(image, sx, sy, sw, sh, 0, 0, 1, 1);
        if (shade) {
          ctx.fillStyle = shade;
          ctx.fillRect(0, 0, 1, 1);
        }
        ctx.restore();
      };

      const mirror = rotated;

      for (const part of parts) {
        const width = slimModel && part.key.startsWith('arm') ? 3 : part.w;
        const px = centerX + (mirror ? -(part.x + width) : part.x) * unit;
        const py = bottom - (part.y + part.h) * unit;
        const pw = width * unit;
        const ph = part.h * unit;

        /* right-hand side face, then the front face on top of it */
        const sideSrc = part.side;
        if (mirror) {
          sheared(sideSrc[0], sideSrc[1], part.sideW, part.h, -dx, -dy, 0, ph, px + pw, py, 'rgba(0,0,0,0.22)');
        } else {
          sheared(sideSrc[0], sideSrc[1], part.sideW, part.h, dx, -dy, 0, ph, px + pw, py, 'rgba(0,0,0,0.22)');
        }

        /* top face */
        sheared(part.top[0], part.top[1], width, part.topH, pw, 0, mirror ? -dx : dx, -dy, px, py, 'rgba(255,255,255,0.07)');

        /* front face */
        face(part.front[0], part.front[1], width, part.h, px, py, pw, ph);

        /* hat layer on the head */
        if (part.hat) {
          face(part.hat.front[0], part.hat.front[1], 8, 8, px, py, pw, ph);
          sheared(part.hat.side[0], part.hat.side[1], 8, 8, mirror ? -dx : dx, -dy, 0, ph,
            mirror ? px : px + pw, py, 'rgba(0,0,0,0.22)');
          sheared(part.hat.top[0], part.hat.top[1], 8, 8, pw, 0, mirror ? -dx : dx, -dy, px, py,
            'rgba(255,255,255,0.07)');
        }
      }
    };
    image.src = skin;
  }, [skin, slimModel, rotated, scale, headOnly]);

  const width = headOnly ? 9 * scale : 17 * scale;
  const height = headOnly ? 10 * scale : 35 * scale;
  return (
    <canvas
      ref={canvas}
      className={`skin-canvas ${className}`}
      width={Math.round(width)}
      height={Math.round(height)}
      aria-label="Skin preview"
    />
  );
}
