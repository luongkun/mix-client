package mix.client.mixui.integration.haunt.module.weapons;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import mix.client.mixui.integration.haunt.asset.AssetSource;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.haunt.HauntMemory;
import mix.client.mixui.integration.haunt.image.ImageLoader;
import mix.client.mixui.integration.haunt.module.weapons.ObjModel;
import mix.client.mixui.integration.haunt.module.weapons.PixelMesh;
import mix.client.mixui.integration.haunt.module.weapons.PixelModel;
import mix.client.mixui.integration.haunt.module.weapons.PixelPalette;
import mix.client.mixui.integration.haunt.module.weapons.WeaponDef;
import mix.client.mixui.integration.haunt.module.weapons.WeaponMesh;
import mix.client.mixui.integration.haunt.module.weapons.WeaponOptions;
import mix.client.mixui.integration.haunt.module.weapons.WeaponSettings;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

final class WeaponAssets {
   static final long UNLOAD_MS = 60000L;
   static final int MAX_TRIANGLES = 150000;
   static final int MAX_TEXTURE = 4096;
   private static final int CARD_TEXTURE = 256;
   private static final long PUMP_NANOS = 2000000L;
   private static final int GL_TEXTURE_MAX_ANISOTROPY = 34046;
   private static final int GL_MAX_TEXTURE_MAX_ANISOTROPY = 34047;
   private static final int LOADER_THREADS = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
   private static final ThreadPoolExecutor LOADER = new ThreadPoolExecutor(LOADER_THREADS, LOADER_THREADS, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue(), new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger();

      public Thread newThread(Runnable r) {
         Thread t = new Thread(r, "HAUN’T VISUALS weapon loader " + this.count.incrementAndGet());
         t.setDaemon(true);
         t.setPriority(5);
         return t;
      }
   });
   private static final Map<String, WeaponAssets> ALL = new HashMap();
   private static final Deque<WeaponAssets> UPLOADS = new ArrayDeque();
   private static Float anisotropy;
   final WeaponDef def;
   final WeaponAssets.Tier tier;
   private volatile WeaponAssets.State state = WeaponAssets.State.EMPTY;
   private volatile String error;
   private volatile WeaponAssets.Decoded decoded;
   private volatile int generation;
   private final List<Runnable> steps = new ArrayList();
   private int step;
   final WeaponMesh[] lods = new WeaponMesh[3];
   int albedo = -1;
   int material = -1;
   int normal = -1;
   volatile PixelModel pixelModel;
   final PixelMesh[] pixelLods = new PixelMesh[2];
   private final PixelPalette[] palettes = new PixelPalette[WeaponOptions.Finish.values().length];
   private WeaponAssets pixelTwin;
   private WeaponAssets.Job queued;
   int textureSize;
   private long gpuBytes;
   private long lastUsed = System.currentTimeMillis();
   boolean pinned;
   private int wantedSize;
   private volatile boolean none;
   private volatile String stamp;
   static final String NO_PIXEL = "it has no Pixelated variant";
   private volatile boolean outOfMemory;

   private WeaponAssets(WeaponDef def, WeaponAssets.Tier tier) {
      this.def = def;
      this.tier = tier;
   }

   static void runOnLoader(Runnable job) {
      LOADER.execute(new WeaponAssets.Job(job, false));
   }

   static WeaponAssets of(WeaponDef def, WeaponAssets.Tier tier) {
      String key = def.id + "|" + tier + "|" + (def.isImported()?def.folder.getAbsolutePath():"");
      WeaponAssets a = (WeaponAssets)ALL.get(key);
      if(a == null) {
         a = new WeaponAssets(def, tier);
         ALL.put(key, a);
      }

      return a;
   }

   WeaponAssets.State state() {
      return this.state;
   }

   String error() {
      return this.error;
   }

   boolean ready() {
      return this.state == WeaponAssets.State.READY;
   }

   boolean loading() {
      return this.state != WeaponAssets.State.READY && this.state != WeaponAssets.State.FAILED;
   }

   WeaponAssets pixelTwin() {
      if(this.tier == WeaponAssets.Tier.PIXEL) {
         return this;
      } else {
         if(this.pixelTwin == null) {
            this.pixelTwin = of(this.def, WeaponAssets.Tier.PIXEL);
         }

         return this.pixelTwin;
      }
   }

   float[] arcs(boolean pixel) {
      if(pixel && this.def.arcs != null) {
         WeaponAssets px = this.pixelTwin();
         PixelModel m = px.ready()?px.pixelModel:null;
         if(m != null && m.arcs != null) {
            return m.arcs;
         }
      }

      return this.def.arcs;
   }

   PixelMesh pixelMesh(int lod) {
      if(this.state != WeaponAssets.State.READY) {
         return null;
      } else {
         int want = lod <= 0?0:1;
         PixelMesh m = this.pixelLods[want];
         if(m != null && m.uploaded()) {
            return m;
         } else {
            m = this.pixelLods[1 - want];
            return m != null && m.uploaded()?m:null;
         }
      }
   }

   PixelPalette palette(WeaponOptions.Finish finish) {
      int i = finish.ordinal();
      if(this.palettes[i] == null) {
         this.palettes[i] = new PixelPalette();
      }

      return this.palettes[i];
   }

   void bakeColors(PixelMesh pm, PixelPalette pal, WeaponSettings s, long key) {
      int[] colors = new int[pm.bakedQuadCount];
      float wear = s != null && s.wear != null?s.wear.getFloat() / 100.0F:0.0F;
      int seed = this.def.id.hashCode() & 1023;
      float span = Math.max(1.0F, pm.alongMax - pm.alongMin);

      for(int q = 0; q < pm.bakedQuadCount; ++q) {
         int o = q * 6;
         int slot = pm.bakedQuads[o];
         int px = pm.bakedQuads[o + 1];
         int py = pm.bakedQuads[o + 2];
         int pz = pm.bakedQuads[o + 3];
         int axis = pm.bakedQuads[o + 4] >> 1;
         boolean edge = (pm.bakedQuads[o + 5] & 1) != 0;
         boolean front = (pm.bakedQuads[o + 5] & 32) != 0;
         float along = Math.max(0.0F, Math.min(0.999F, ((float)py + 0.5F - pm.alongMin) / span));
         colors[q] = -16777216 | pal.color(slot, px, py, pz, axis, edge, front, along, wear, seed);
      }

      pm.bake(colors, key);
   }

   void request(int textureSize) {
      this.request(textureSize, this.tier == WeaponAssets.Tier.FULL);
   }

   void request(int textureSize, boolean urgent) {
      this.lastUsed = System.currentTimeMillis();
      final int size = this.tier == WeaponAssets.Tier.CARD?256:HauntMemory.textureCap(textureSize);
      if(this.state == WeaponAssets.State.READY && size != this.wantedSize && this.tier == WeaponAssets.Tier.FULL) {
         this.release();
      }

      if(this.state == WeaponAssets.State.EMPTY) {
         if(!this.def.isImported() && !AssetSource.ready()) {
            return;
         }

         this.wantedSize = size;
         this.state = WeaponAssets.State.LOADING;
         final int gen = this.generation;
         this.queued = new WeaponAssets.Job(new Runnable() {
            public void run() {
               WeaponAssets.this.load(gen, size);
            }
         }, urgent);
         LOADER.execute(this.queued);
      } else if(urgent && this.state == WeaponAssets.State.LOADING && this.queued != null && !this.queued.urgent && LOADER.getQueue().remove(this.queued)) {
         this.queued = new WeaponAssets.Job(this.queued.work, true);
         LOADER.execute(this.queued);
      }

   }

   boolean pixelImport() {
      return this.none && this.ready();
   }

   boolean frame(int lod, float[] out) {
      WeaponMesh m = this.mesh(lod);
      if(m != null) {
         System.arraycopy(m.min, 0, out, 0, 3);
         System.arraycopy(m.max, 0, out, 3, 3);
         return true;
      } else if(this.pixelImport()) {
         System.arraycopy(this.def.bounds, 0, out, 0, 6);
         return true;
      } else {
         return false;
      }
   }

   WeaponMesh mesh(int lod) {
      for(int i = Math.max(0, Math.min(2, lod)); i >= 0; --i) {
         if(this.lods[i] != null && this.lods[i].uploaded()) {
            return this.lods[i];
         }
      }

      for(int var3 = lod + 1; var3 < 3; ++var3) {
         if(this.lods[var3] != null && this.lods[var3].uploaded()) {
            return this.lods[var3];
         }
      }

      return null;
   }

   long gpuBytes() {
      return this.gpuBytes;
   }

   static boolean pixelOnly(WeaponDef def) {
      return def.isImported() && def.folder != null && !(new File(def.folder, "model.obj")).isFile() && (new File(def.folder, "pixel" + File.separator + "pixel.json")).isFile();
   }

   private void loadPixel(int gen) {
      try {
         PixelModel m = PixelModel.read(this.def);
         if(m == null) {
            throw new IOException("it has no Pixelated variant");
         }

         WeaponAssets.Decoded d = new WeaponAssets.Decoded();
         d.pixel = m;
         d.pixelMeshes = new PixelMesh[]{PixelMesh.build(m, false), PixelMesh.build(m, true)};
         if(gen != this.generation) {
            return;
         }

         this.decoded = d;
         this.state = WeaponAssets.State.UPLOADING;
         Deque<WeaponAssets> deque = UPLOADS;
         synchronized(deque) {
            UPLOADS.add(this);
         }
      } catch (IOException | OutOfMemoryError | RuntimeException var8) {
         if(gen != this.generation) {
            return;
         }

         this.error = var8.getMessage();
         this.state = WeaponAssets.State.FAILED;
         if(!"it has no Pixelated variant".equals(var8.getMessage())) {
            FuriahsVisuals.LOG.warn("Weapon {}\'s Pixelated variant could not be loaded: {}", new Object[]{this.def.id, var8.getMessage(), var8 instanceof RuntimeException?var8:null});
         }
      }

   }

   private void load(int gen, int size) {
      boolean heavy = this.tier == WeaponAssets.Tier.FULL;
      if(heavy) {
         HauntMemory.HEAVY_DECODE.acquireUninterruptibly();
      }

      try {
         this.outOfMemory = false;
         this.loadInner(gen, size);
         if(this.outOfMemory && gen == this.generation && size > 256) {
            HauntMemory.degrade();
            this.outOfMemory = false;
            this.error = null;
            this.state = WeaponAssets.State.LOADING;
            FuriahsVisuals.LOG.warn("Weapon {} ran out of memory at {} px, retrying at {} px", new Object[]{this.def.id, Integer.valueOf(size), Integer.valueOf(size / 2)});
            this.loadInner(gen, size / 2);
         }
      } finally {
         if(heavy) {
            HauntMemory.HEAVY_DECODE.release();
         }

      }

   }

   static void emergencyRelease() {
      HauntMemory.degrade();

      for(WeaponAssets a : new ArrayList(ALL.values())) {
         try {
            a.release();
         } catch (Throwable var3) {
            ;
         }
      }

   }

   private void loadInner(int gen, int size) {
      this.stamp = stamp(this.def, this.tier);
      if(this.tier == WeaponAssets.Tier.PIXEL) {
         this.loadPixel(gen);
      } else {
         try {
            WeaponAssets.Decoded d = new WeaponAssets.Decoded();
            if(pixelOnly(this.def)) {
               d.none = true;
               if(gen != this.generation) {
                  return;
               }

               this.none = true;
               this.decoded = d;
               this.state = WeaponAssets.State.UPLOADING;
               Deque<WeaponAssets> deque = UPLOADS;
               synchronized(deque) {
                  UPLOADS.add(this);
                  return;
               }
            }

            this.none = false;
            if(this.tier == WeaponAssets.Tier.FULL) {
               String[] names = new String[]{"model.obj", "model_lod1.obj", "model_lod2.obj"};

               for(int i = 0; i < 3; ++i) {
                  ObjModel m = this.readModel(names[i], i == 0);
                  if(m != null) {
                     d.meshes[i] = WeaponMesh.build(m);
                  }
               }
            } else {
               ObjModel m = this.readModel("model_lod1.obj", false);
               if(m == null) {
                  m = this.readModel("model.obj", true);
               }

               d.meshes[1] = WeaponMesh.build(m);
            }

            if(d.meshes[0] == null && d.meshes[1] == null && d.meshes[2] == null) {
               throw new IOException("the weapon has no model");
            }

            int[] dims = new int[2];
            int[] albedoPx = this.readImage(this.def.texture("albedo"), dims, true);
            int w = dims[0];
            int h = dims[1];
            int[] materialPx = this.readImage(this.def.texture("material"), dims, false);
            if(materialPx != null && (dims[0] != w || dims[1] != h)) {
               materialPx = null;
            }

            int[] normalPx;
            if((normalPx = this.readImage(this.def.texture("normal"), dims, false)) != null && (dims[0] != w || dims[1] != h)) {
               normalPx = null;
            }

            int levelsToSkip;
            for(levelsToSkip = 0; Math.max(w, h) >> levelsToSkip > size && Math.min(w, h) >> levelsToSkip > 1; ++levelsToSkip) {
               ;
            }

            ArrayList<int[]> sizes = new ArrayList();
            d.albedo = mips(albedoPx, w, h, levelsToSkip, false, sizes);
            d.material = materialPx == null?null:mips(materialPx, w, h, levelsToSkip, false, (List<int[]>)null);
            d.normal = normalPx == null?null:mips(normalPx, w, h, levelsToSkip, true, (List<int[]>)null);
            d.sizes = new int[sizes.size() * 2];

            for(int ix = 0; ix < sizes.size(); ++ix) {
               d.sizes[ix * 2] = ((int[])sizes.get(ix))[0];
               d.sizes[ix * 2 + 1] = ((int[])sizes.get(ix))[1];
            }

            if(gen != this.generation) {
               return;
            }

            this.decoded = d;
            this.state = WeaponAssets.State.UPLOADING;
            Deque<WeaponAssets> deque = UPLOADS;
            synchronized(deque) {
               UPLOADS.add(this);
            }
         } catch (RuntimeException | ImageLoader.LoadException | IOException | OutOfMemoryError var17) {
            if(var17 instanceof OutOfMemoryError) {
               this.outOfMemory = true;
            }

            if(gen != this.generation) {
               return;
            }

            this.error = var17.getMessage();
            this.state = WeaponAssets.State.FAILED;
            FuriahsVisuals.LOG.warn("Weapon {} could not be loaded: {}", new Object[]{this.def.id, var17.getMessage(), var17 instanceof RuntimeException?var17:null});
         }
      }

   }

   private ObjModel readModel(String name, boolean required) throws IOException {
      InputStream in = this.def.open(name);
      if(in == null) {
         if(required) {
            throw new IOException("its model (" + name + ") is missing");
         } else {
            return null;
         }
      } else {
         ObjModel var12;
         try {
            ObjModel m = ObjModel.read(in, 150000);
            if(this.def.chainPath != null) {
               for(String part : new ArrayList(m.parts.keySet())) {
                  if(this.def.roleOf(part) == WeaponDef.Role.CHAIN) {
                     m.bakeChain(part, this.def.chainPath);
                  }
               }
            }

            var12 = m;
         } catch (NumberFormatException | ArrayIndexOutOfBoundsException var11) {
            throw new IOException("its model (" + name + ") is not a valid OBJ file: " + var11.getMessage());
         } finally {
            in.close();
         }

         return var12;
      }
   }

   private int[] readImage(String name, int[] dims, boolean required) throws IOException, ImageLoader.LoadException {
      ImageLoader.Image img;
      if(this.def.isImported()) {
         File f = new File(this.def.folder, name);
         if(!f.isFile()) {
            if(required) {
               dims[1] = 4;
               dims[0] = 4;
               int[] px = new int[16];
               Arrays.fill(px, -4934476);
               return px;
            }

            return null;
         }

         img = ImageLoader.load(f, this.tier == WeaponAssets.Tier.CARD?512:4096, this.tier == WeaponAssets.Tier.CARD);
      } else {
         InputStream in = this.def.open(name);
         if(in == null) {
            if(required) {
               throw new IOException("its texture " + name + " is missing");
            }

            return null;
         }

         byte[] bytes;
         try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];

            int n;
            while((n = in.read(buf)) > 0) {
               out.write(buf, 0, n);
            }

            bytes = out.toByteArray();
         } finally {
            in.close();
         }

         img = ImageLoader.decodeBytes(bytes, name, this.tier == WeaponAssets.Tier.CARD?512:4096, this.tier == WeaponAssets.Tier.CARD);
      }

      dims[0] = img.width;
      dims[1] = img.height;
      return img.argb;
   }

   private static ByteBuffer[] mips(int[] argb, int w, int h, int skip, boolean normalMap, List<int[]> sizes) {
      ArrayList<ByteBuffer> out = new ArrayList();
      int[] cur = argb;
      int cw = w;
      int ch = h;
      int level = 0;

      while(true) {
         if(level >= skip) {
            ByteBuffer b = ByteBuffer.allocateDirect(cw * ch * 4).order(ByteOrder.nativeOrder());

            for(int i = 0; i < cw * ch; ++i) {
               int c = cur[i];
               b.put((byte)(c >> 16)).put((byte)(c >> 8)).put((byte)c).put((byte)(c >>> 24));
            }

            b.flip();
            out.add(b);
            if(sizes != null) {
               sizes.add(new int[]{cw, ch});
            }
         }

         if(cw == 1 && ch == 1) {
            return (ByteBuffer[])out.toArray(new ByteBuffer[0]);
         }

         int nw = Math.max(1, cw / 2);
         int nh = Math.max(1, ch / 2);
         int[] next = new int[nw * nh];

         for(int y = 0; y < nh; ++y) {
            for(int x = 0; x < nw; ++x) {
               int x0 = Math.min(cw - 1, x * 2);
               int x1 = Math.min(cw - 1, x * 2 + 1);
               int y0 = Math.min(ch - 1, y * 2);
               int y1 = Math.min(ch - 1, y * 2 + 1);
               int a = cur[y0 * cw + x0];
               int b = cur[y0 * cw + x1];
               int c = cur[y1 * cw + x0];
               int d = cur[y1 * cw + x1];
               int ca = (a >>> 24) + (b >>> 24) + (c >>> 24) + (d >>> 24) + 2 >> 2;
               int cr = (a >> 16 & 255) + (b >> 16 & 255) + (c >> 16 & 255) + (d >> 16 & 255) + 2 >> 2;
               int cg = (a >> 8 & 255) + (b >> 8 & 255) + (c >> 8 & 255) + (d >> 8 & 255) + 2 >> 2;
               int cb = (a & 255) + (b & 255) + (c & 255) + (d & 255) + 2 >> 2;
               double nz;
               double ny;
               double nx;
               double l;
               if(normalMap && (l = Math.sqrt((nx = (double)cr / 127.5D - 1.0D) * nx + (ny = (double)cg / 127.5D - 1.0D) * ny + (nz = (double)cb / 127.5D - 1.0D) * nz)) > 1.0E-6D) {
                  cr = (int)Math.round((nx / l * 0.5D + 0.5D) * 255.0D);
                  cg = (int)Math.round((ny / l * 0.5D + 0.5D) * 255.0D);
                  cb = (int)Math.round((nz / l * 0.5D + 0.5D) * 255.0D);
               }

               next[y * nw + x] = ca << 24 | cr << 16 | cg << 8 | cb;
            }
         }

         cur = next;
         cw = nw;
         ch = nh;
         ++level;
      }
   }

   static void pump() {
      long start = System.nanoTime();

      while(System.nanoTime() - start < 2000000L) {
         Deque<WeaponAssets> deque = UPLOADS;
         WeaponAssets a;
         synchronized(deque) {
            a = (WeaponAssets)UPLOADS.peek();
         }

         if(a == null) {
            return;
         }

         if(!a.uploadStep()) {
            deque = UPLOADS;
            synchronized(deque) {
               UPLOADS.poll();
            }
         }
      }

   }

   private boolean uploadStep() {
      if(this.state == WeaponAssets.State.UPLOADING && this.decoded != null) {
         if(this.steps.isEmpty()) {
            this.planUpload();
         }

         if(this.step < this.steps.size()) {
            try {
               ((Runnable)this.steps.get(this.step++)).run();
            } catch (RuntimeException | LinkageError var2) {
               this.error = "the graphics card refused it (" + var2.getMessage() + ")";
               this.state = WeaponAssets.State.FAILED;
               FuriahsVisuals.LOG.warn("Weapon {} could not be uploaded", new Object[]{this.def.id, var2});
               this.deleteGpu();
               this.decoded = null;
               this.steps.clear();
               return false;
            }
         }

         if(this.step >= this.steps.size()) {
            this.steps.clear();
            this.step = 0;
            this.decoded = null;
            this.state = WeaponAssets.State.READY;
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private void planUpload() {
      final WeaponAssets.Decoded d = this.decoded;
      this.step = 0;
      if(d.none) {
         this.steps.add(new Runnable() {
            public void run() {
            }
         });
      } else if(d.pixelMeshes == null) {
         for(final int ii = 0; ii < 3; ++ii) {
            if(d.meshes[ii] != null) {
               this.steps.add(new Runnable() {
                  public void run() {
                     d.meshes[ii].upload();
                     WeaponAssets.this.lods[ii] = d.meshes[ii];
                     WeaponAssets.this.gpuBytes = WeaponAssets.this.gpuBytes + d.meshes[ii].bytes;
                  }
               });
            }
         }

         this.textureSize = Math.max(d.sizes[0], d.sizes[1]);
         this.addTexture(d, d.albedo, 0);
         if(d.material != null) {
            this.addTexture(d, d.material, 1);
         }

         if(d.normal != null) {
            this.addTexture(d, d.normal, 2);
         }
      } else {
         int ix = 0;

         while(ix < d.pixelMeshes.length) {
            final int k = ix++;
            this.steps.add(new Runnable() {
               public void run() {
                  d.pixelMeshes[k].upload();
                  WeaponAssets.this.pixelLods[k] = d.pixelMeshes[k];
                  WeaponAssets.this.gpuBytes = WeaponAssets.this.gpuBytes + d.pixelMeshes[k].bytes;
               }
            });
         }

         this.steps.add(new Runnable() {
            public void run() {
               WeaponAssets.this.pixelModel = d.pixel;
            }
         });
      }

   }

   private void addTexture(final WeaponAssets.Decoded d, final ByteBuffer[] levels, final int which) {
      this.steps.add(new Runnable() {
         public void run() {
            HauntMemory.safeUnpackState();
            int id = TextureUtil.glGenTextures();
            if(which == 0) {
               WeaponAssets.this.albedo = id;
            } else if(which == 1) {
               WeaponAssets.this.material = id;
            } else {
               WeaponAssets.this.normal = id;
            }

            GlStateManager.bindTexture(id);
            GL11.glTexParameteri(3553, 10241, 9987);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, '脯');
            GL11.glTexParameteri(3553, 10243, '脯');
            GL11.glTexParameteri(3553, '脽', levels.length - 1);
            float aniso = WeaponAssets.maxAnisotropy();
            if(aniso > 1.0F) {
               GL11.glTexParameterf(3553, '蓾', Math.min(8.0F, aniso));
            }

         }
      });
      int i = 0;

      while(i < levels.length) {
         final int level = i++;
         this.steps.add(new Runnable() {
            public void run() {
               int id = which == 0?WeaponAssets.this.albedo:(which == 1?WeaponAssets.this.material:WeaponAssets.this.normal);
               GlStateManager.bindTexture(id);
               int w = d.sizes[level * 2];
               int h = d.sizes[level * 2 + 1];
               ByteBuffer px = levels[level];
               if(px != null && px.capacity() >= w * h * 4) {
                  px.rewind();
                  GL11.glTexImage2D(3553, level, '聘', w, h, 0, 6408, 5121, px);
                  levels[level] = null;
                  WeaponAssets.this.gpuBytes = WeaponAssets.this.gpuBytes + (long)w * (long)h * 4L;
               } else {
                  throw new IllegalStateException("texture level " + level + " is smaller than " + w + "x" + h);
               }
            }
         });
      }

      this.steps.add(new Runnable() {
         public void run() {
            GlStateManager.bindTexture(0);
         }
      });
   }

   private static float maxAnisotropy() {
      if(anisotropy == null) {
         float a = 1.0F;

         try {
            boolean has;
            try {
               has = GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic;
            } catch (LinkageError var4) {
               String ext = GL11.glGetString(7939);
               has = ext != null && ext.contains("GL_EXT_texture_filter_anisotropic");
            }

            if(has) {
               a = GL11.glGetFloat('蓿');
            }
         } catch (RuntimeException | LinkageError var5) {
            a = 1.0F;
         }

         anisotropy = Float.valueOf(a);
      }

      return anisotropy.floatValue();
   }

   void release() {
      ++this.generation;
      Deque<WeaponAssets> deque = UPLOADS;
      synchronized(deque) {
         UPLOADS.remove(this);
      }

      this.steps.clear();
      this.step = 0;
      this.deleteGpu();
      this.decoded = null;
      this.error = null;
      this.state = WeaponAssets.State.EMPTY;
   }

   private void deleteGpu() {
      for(int i = 0; i < 3; ++i) {
         if(this.lods[i] != null) {
            this.lods[i].delete();
            this.lods[i] = null;
         }
      }

      for(int var6 = 0; var6 < this.pixelLods.length; ++var6) {
         if(this.pixelLods[var6] != null) {
            this.pixelLods[var6].delete();
            this.pixelLods[var6] = null;
         }
      }

      for(PixelPalette p : this.palettes) {
         if(p != null) {
            p.delete();
         }
      }

      this.pixelModel = null;
      if(this.albedo >= 0) {
         TextureUtil.deleteTexture(this.albedo);
      }

      if(this.material >= 0) {
         TextureUtil.deleteTexture(this.material);
      }

      if(this.normal >= 0) {
         TextureUtil.deleteTexture(this.normal);
      }

      this.normal = -1;
      this.material = -1;
      this.albedo = -1;
      this.gpuBytes = 0L;
   }

   static void collect() {
      long now = System.currentTimeMillis();

      for(WeaponAssets a : ALL.values()) {
         if(!a.pinned && a.state != WeaponAssets.State.EMPTY && now - a.lastUsed > 60000L) {
            a.release();
         }
      }

   }

   static String stamp(WeaponDef def, WeaponAssets.Tier tier) {
      if(def.isImported() && def.folder != null) {
         String[] stringArray;
         if(tier == WeaponAssets.Tier.PIXEL) {
            String[] stringArray2 = new String[1];
            stringArray = stringArray2;
            stringArray2[0] = "pixel/pixel.json";
         } else {
            String[] stringArray3 = new String[]{"model.obj", "model_lod1.obj", "model_lod2.obj", def.texture("albedo"), def.texture("material"), null};
            stringArray = stringArray3;
            stringArray3[5] = def.texture("normal");
         }

         StringBuilder sb = new StringBuilder();

         for(String n : stringArray) {
            if(n != null) {
               File f = new File(def.folder, n);
               sb.append(n).append(' ').append(f.isFile()?f.length() + " " + f.lastModified():"-").append(';');
            }
         }

         if(tier != WeaponAssets.Tier.PIXEL) {
            sb.append((new File(def.folder, "pixel" + File.separator + "pixel.json")).isFile()?"pixel":"-");
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   static void dropChanged(Map<String, WeaponDef> now) {
      HashSet<String> changed = new HashSet();

      for(WeaponAssets a : ALL.values()) {
         if(a.def.isImported()) {
            WeaponDef d = (WeaponDef)now.get(a.def.id);
            String was = a.stamp;
            if(d == null || !a.def.folder.equals(d.folder) || was != null && !was.equals(stamp(d, a.tier))) {
               changed.add(a.def.id);
            }
         }
      }

      if(!changed.isEmpty()) {
         Iterator<WeaponAssets> it = ALL.values().iterator();

         while(it.hasNext()) {
            WeaponAssets ax = (WeaponAssets)it.next();
            if(ax.def.isImported() && changed.contains(ax.def.id)) {
               ax.release();
               it.remove();
            }
         }
      }

   }

   static void releaseAll() {
      for(WeaponAssets a : ALL.values()) {
         a.release();
      }

      ALL.clear();
   }

   static long totalGpuBytes() {
      long n = 0L;

      for(WeaponAssets a : ALL.values()) {
         n += a.gpuBytes;
      }

      return n;
   }

   static int loadedCount() {
      int n = 0;

      for(WeaponAssets a : ALL.values()) {
         if(a.ready()) {
            ++n;
         }
      }

      return n;
   }

   static void unpinAll() {
      for(WeaponAssets a : ALL.values()) {
         a.pinned = false;
      }

   }

   static InputStream openFile(File folder, String name) throws IOException {
      File f = new File(folder, name);
      return f.isFile()?new FileInputStream(f):null;
   }

   private static final class Decoded {
      final WeaponMesh[] meshes;
      ByteBuffer[] albedo;
      ByteBuffer[] material;
      ByteBuffer[] normal;
      int[] sizes;
      PixelModel pixel;
      PixelMesh[] pixelMeshes;
      boolean none;

      private Decoded() {
         this.meshes = new WeaponMesh[3];
      }
   }

   private static final class Job implements Runnable, Comparable<WeaponAssets.Job> {
      private static final AtomicLong NEXT = new AtomicLong();
      final Runnable work;
      final boolean urgent;
      private final long order;

      Job(Runnable work, boolean urgent) {
         this.order = NEXT.getAndIncrement();
         this.work = work;
         this.urgent = urgent;
      }

      public void run() {
         try {
            this.work.run();
         } catch (RuntimeException | LinkageError var2) {
            FuriahsVisuals.LOG.warn("A weapon loader job failed", var2);
         }

      }

      public int compareTo(WeaponAssets.Job o) {
         return this.urgent != o.urgent?(this.urgent?-1:1):Long.compare(this.order, o.order);
      }
   }

   static enum State {
      EMPTY,
      LOADING,
      UPLOADING,
      READY,
      FAILED;

      // $FF: synthetic method
      private static WeaponAssets.State[] $values() {
         return new WeaponAssets.State[]{EMPTY, LOADING, UPLOADING, READY, FAILED};
      }
   }

   static enum Tier {
      FULL,
      CARD,
      PIXEL;

      // $FF: synthetic method
      private static WeaponAssets.Tier[] $values() {
         return new WeaponAssets.Tier[]{FULL, CARD, PIXEL};
      }
   }
}
