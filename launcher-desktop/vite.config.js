import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

/* The renderer is loaded from disk by Electron, so every asset path has to be
   relative instead of rooted at "/". */
export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss()],
  server: {
    port: 5183,
    strictPort: true,
    watch: {
      /* electron-builder writes the unpacked app next to the sources and keeps
         its temp files locked while packing, which killed the dev server with
         EBUSY: resource busy or locked. None of these are inputs. */
      ignored: ['**/release/**', '**/dist/**', '**/jre/**']
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
});
