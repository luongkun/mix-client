import type { CSSProperties } from 'react';

/* A 1.8 skin stores the head as an 8x8 tile at (8,8) and the hat layer as a
   second 8x8 tile at (40,8). Both are drawn with background-position rather than
   a canvas crop, so the pixels stay crisp at any size and no image decoding is
   needed. */

type Props = {
  skin: string | null;
  size?: number;
  className?: string;
};

function layer(skin: string, size: number, x: number, y: number): CSSProperties {
  const scale = size / 8;
  const full = 64 * scale;
  return {
    width: size,
    height: size,
    backgroundImage: `url(${skin})`,
    backgroundSize: `${full}px ${full}px`,
    backgroundPosition: `-${x * scale}px -${y * scale}px`,
    imageRendering: 'pixelated',
    backgroundRepeat: 'no-repeat',
  };
}

export default function SkinAvatar({ skin, size = 38, className = '' }: Props) {
  if (!skin) {
    return (
      <span
        className={`inline-block flex-none bg-elevated ${className}`}
        style={{ width: size, height: size }}
        aria-hidden
      />
    );
  }
  return (
    <span
      className={`relative inline-block flex-none overflow-hidden ${className}`}
      style={{ width: size, height: size }}
      aria-hidden
    >
      <span className="absolute inset-0" style={layer(skin, size, 8, 8)} />
      <span className="absolute inset-0" style={layer(skin, size, 40, 8)} />
    </span>
  );
}