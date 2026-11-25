// vite.config.js
import { defineConfig } from "file:///C:/Users/lucif/Downloads/Mix%20Client/launcher-desktop/node_modules/vite/dist/node/index.js";
import react from "file:///C:/Users/lucif/Downloads/Mix%20Client/launcher-desktop/node_modules/@vitejs/plugin-react/dist/index.js";
var vite_config_default = defineConfig({
  base: "./",
  plugins: [react()],
  server: {
    port: 5183,
    strictPort: true,
    watch: {
      /* electron-builder writes the unpacked app next to the sources and keeps
         its temp files locked while packing, which killed the dev server with
         EBUSY: resource busy or locked. None of these are inputs. */
      ignored: ["**/release/**", "**/dist/**", "**/jre/**"]
    }
  },
  build: {
    outDir: "dist",
    emptyOutDir: true
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJDOlxcXFxVc2Vyc1xcXFxsdWNpZlxcXFxEb3dubG9hZHNcXFxcTWl4IENsaWVudFxcXFxsYXVuY2hlci1kZXNrdG9wXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJDOlxcXFxVc2Vyc1xcXFxsdWNpZlxcXFxEb3dubG9hZHNcXFxcTWl4IENsaWVudFxcXFxsYXVuY2hlci1kZXNrdG9wXFxcXHZpdGUuY29uZmlnLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9DOi9Vc2Vycy9sdWNpZi9Eb3dubG9hZHMvTWl4JTIwQ2xpZW50L2xhdW5jaGVyLWRlc2t0b3Avdml0ZS5jb25maWcuanNcIjtpbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tICd2aXRlJztcbmltcG9ydCByZWFjdCBmcm9tICdAdml0ZWpzL3BsdWdpbi1yZWFjdCc7XG5cbi8qIFRoZSByZW5kZXJlciBpcyBsb2FkZWQgZnJvbSBkaXNrIGJ5IEVsZWN0cm9uLCBzbyBldmVyeSBhc3NldCBwYXRoIGhhcyB0byBiZVxuICAgcmVsYXRpdmUgaW5zdGVhZCBvZiByb290ZWQgYXQgXCIvXCIuICovXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xuICBiYXNlOiAnLi8nLFxuICBwbHVnaW5zOiBbcmVhY3QoKV0sXG4gIHNlcnZlcjoge1xuICAgIHBvcnQ6IDUxODMsXG4gICAgc3RyaWN0UG9ydDogdHJ1ZSxcbiAgICB3YXRjaDoge1xuICAgICAgLyogZWxlY3Ryb24tYnVpbGRlciB3cml0ZXMgdGhlIHVucGFja2VkIGFwcCBuZXh0IHRvIHRoZSBzb3VyY2VzIGFuZCBrZWVwc1xuICAgICAgICAgaXRzIHRlbXAgZmlsZXMgbG9ja2VkIHdoaWxlIHBhY2tpbmcsIHdoaWNoIGtpbGxlZCB0aGUgZGV2IHNlcnZlciB3aXRoXG4gICAgICAgICBFQlVTWTogcmVzb3VyY2UgYnVzeSBvciBsb2NrZWQuIE5vbmUgb2YgdGhlc2UgYXJlIGlucHV0cy4gKi9cbiAgICAgIGlnbm9yZWQ6IFsnKiovcmVsZWFzZS8qKicsICcqKi9kaXN0LyoqJywgJyoqL2pyZS8qKiddXG4gICAgfVxuICB9LFxuICBidWlsZDoge1xuICAgIG91dERpcjogJ2Rpc3QnLFxuICAgIGVtcHR5T3V0RGlyOiB0cnVlXG4gIH1cbn0pO1xuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUE0VixTQUFTLG9CQUFvQjtBQUN6WCxPQUFPLFdBQVc7QUFJbEIsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsTUFBTTtBQUFBLEVBQ04sU0FBUyxDQUFDLE1BQU0sQ0FBQztBQUFBLEVBQ2pCLFFBQVE7QUFBQSxJQUNOLE1BQU07QUFBQSxJQUNOLFlBQVk7QUFBQSxJQUNaLE9BQU87QUFBQTtBQUFBO0FBQUE7QUFBQSxNQUlMLFNBQVMsQ0FBQyxpQkFBaUIsY0FBYyxXQUFXO0FBQUEsSUFDdEQ7QUFBQSxFQUNGO0FBQUEsRUFDQSxPQUFPO0FBQUEsSUFDTCxRQUFRO0FBQUEsSUFDUixhQUFhO0FBQUEsRUFDZjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
