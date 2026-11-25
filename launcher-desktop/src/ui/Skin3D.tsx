import { useEffect, useRef, useState } from 'react';
import SkinPreview from '../SkinPreview.jsx';

/* skinview3d wrapper: real 3D, idle animation, optional spin. If WebGL is not
   available it falls back to the flat canvas renderer. */

type Props = {
  skin: string | null;
  slim?: boolean;
  spin?: boolean;
  width?: number;
  height?: number;
  zoom?: number;
  className?: string;
};

export default function Skin3D({ skin, slim = false, spin = true, width = 320, height = 420, zoom = 0.82, className = '' }: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const viewerRef = useRef(null);
  const skinRef = useRef(skin);
  const slimRef = useRef(slim);
  const spinRef = useRef(spin);
  const [failed, setFailed] = useState(false);

  skinRef.current = skin;
  slimRef.current = slim;
  spinRef.current = spin;

  useEffect(() => {
    let disposed = false;
    let viewer = null;

    import('skinview3d').then(({ IdleAnimation, SkinViewer }) => {
      if (disposed || !canvasRef.current) {
        return;
      }
      try {
        viewer = new SkinViewer({ canvas: canvasRef.current, width, height });
        viewer.autoRotate = spinRef.current;
        viewer.autoRotateSpeed = 0.6;
        viewer.zoom = zoom;
        viewer.controls.enableZoom = false;
        viewer.controls.enablePan = false;
        viewer.animation = new IdleAnimation();
        viewerRef.current = viewer;
        if (skinRef.current) {
          viewer.loadSkin(skinRef.current, { model: slimRef.current ? 'slim' : 'classic' })
            .catch(() => {
              if (!disposed) setFailed(true);
            });
        }
      } catch {
        setFailed(true);
      }
    }).catch(() => {
      if (!disposed) setFailed(true);
    });

    return () => {
      disposed = true;
      try {
        viewer?.dispose();
      } catch {
        /* the GL context may already be gone */
      }
      if (viewerRef.current === viewer) {
        viewerRef.current = null;
      }
    };
  }, [width, height, zoom]);

  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer || !skin) {
      return;
    }
    viewer.loadSkin(skin, { model: slim ? 'slim' : 'classic' }).catch(() => setFailed(true));
  }, [skin, slim]);

  useEffect(() => {
    if (viewerRef.current) {
      viewerRef.current.autoRotate = spin;
    }
  }, [spin]);

  if (failed) {
    return <SkinPreview skin={skin} slimModel={slim} scale={Math.max(6, Math.round(height / 34))} />;
  }
  return <canvas ref={canvasRef} style={{ width, height }} className={`max-w-full ${className}`} aria-label="Skin preview" />;
}
