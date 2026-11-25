package mix.client.mixui.integration.haunt.gui;

import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.gui.Density;
import mix.client.mixui.integration.haunt.gui.FuriahScreen;
import mix.client.mixui.integration.haunt.gui.MenuMemory;
import mix.client.mixui.integration.haunt.gui.MenuPrefs;
import mix.client.mixui.integration.haunt.gui.SectionCategories;
import mix.client.mixui.integration.haunt.gui.UiProbe;
import mix.client.mixui.integration.haunt.gui.layout.LayoutPage;
import mix.client.mixui.integration.haunt.gui.layout.SectionLayout;
import mix.client.mixui.integration.haunt.gui.layout.SectionLayouts;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.watermark.IslandCheck;
import mix.client.mixui.integration.haunt.module.weapons.WeaponsModule;
import mix.client.mixui.integration.haunt.render.Easing;
import net.minecraft.client.Minecraft;

public final class GuiCheck {
   private static final Density.Mode[] DENSITIES = new Density.Mode[]{Density.Mode.COMFORTABLE, Density.Mode.COMPACT, Density.Mode.DENSE};
   private static final int[] SCALES = new int[]{75, 100, 125, 150, 200};
   private static final int CONFIG_FRAMES = 14;
   private static final int PAGE_FRAMES = 5;
   private static final int SCROLL_FRAMES = 2;
   static final String COMPARE = "+compare";
   private static GuiCheck pending;
   static GuiCheck running;
   private final List<String[]> configs = new ArrayList();
   private final List<GuiCheck.Page> pages = new ArrayList();
   private int config = -1;
   private int pageIndex;
   private int wait;
   private float scroll;
   private boolean measureNext;
   private int states;
   private int frames;
   private final Map<String, GuiCheck.Found> found = new LinkedHashMap();
   private long started;
   private Density.Mode savedDensity;
   private int savedScale;
   private int savedPanelW;
   private int savedPanelH;
   private boolean savedAuto;
   private boolean savedFull;
   private MenuPrefs.SidebarStyle savedSidebar;
   private JsonElement savedMemory;
   private String savedUiTab;
   public static String lastSummary;
   public static int lastCount = -1;

   private GuiCheck() {
      for(Density.Mode d : DENSITIES) {
         for(int s : SCALES) {
            this.configs.add(new String[]{d.name(), String.valueOf(s), "large", "FULL"});
            this.configs.add(new String[]{d.name(), String.valueOf(s), "small", "FULL"});
         }
      }

      this.configs.add(new String[]{"COMPACT", "100", "large", "HIDDEN"});
      this.configs.add(new String[]{"COMFORTABLE", "125", "small", "HIDDEN"});
   }

   public static void request() {
      pending = new GuiCheck();
      FuriahsVisuals.instance.openMenuNextTick();
   }

   static void attach(FuriahScreen screen) {
      if(pending != null) {
         running = pending;
         pending = null;
         running.start(screen);
      }

   }

   private void start(FuriahScreen screen) {
      this.started = System.currentTimeMillis();
      this.savedDensity = (Density.Mode)MenuPrefs.VIEW_THEME.get();
      this.savedScale = MenuPrefs.GUI_SCALE.getInt();
      this.savedAuto = MenuPrefs.GUI_SCALE_AUTO.get();
      this.savedPanelW = MenuPrefs.PANEL_W.getInt();
      this.savedPanelH = MenuPrefs.PANEL_H.getInt();
      this.savedFull = MenuPrefs.FULL_SCREEN.get();
      this.savedSidebar = (MenuPrefs.SidebarStyle)MenuPrefs.SIDEBAR.get();
      this.savedMemory = MenuMemory.snapshot();
      this.savedUiTab = FuriahsVisuals.instance.getUi().currentTabId();
      this.buildPages();
      Easing.instant = true;
      FuriahsVisuals.LOG.info("GUI check: {} configurations x {} pages", new Object[]{Integer.valueOf(this.configs.size()), Integer.valueOf(this.pages.size())});
      FuriahsVisuals.chat("GUI check: going through " + this.configs.size() + " configurations of " + this.pages.size() + " pages (Esc stops it)…");
   }

   private void buildPages() {
      for(Module m : SectionCategories.ordered(FuriahsVisuals.instance.getModules().getModules())) {
         boolean weapons = "weapons".equals(m.getId());
         SectionLayout l = SectionLayouts.of(m);

         for(LayoutPage p : l.pages) {
            if(!l.tabbed() || !p.isSection()) {
               String sub = p.isSection()?null:p.key;
               if(weapons && p.isSection()) {
                  for(String tab : new String[]{"FIRST_PERSON", "THIRD_PERSON", "MODEL"}) {
                     this.pages.add(new GuiCheck.Page(m, (String)null, tab + "+compare", false, (String)null));
                  }

                  for(String tab : new String[]{"FIRST_PERSON", "THIRD_PERSON", "MODEL"}) {
                     this.pages.add(new GuiCheck.Page(m, (String)null, tab, false, (String)null));
                  }

                  this.pages.add(new GuiCheck.Page(m, (String)null, (String)null, true, (String)null));
               } else {
                  this.pages.add(new GuiCheck.Page(m, sub, (String)null, false, (String)null));
                  if(p.isSection() && "watermark".equals(m.getId()) || p.isTab() && "ui".equals(m.getId())) {
                     this.pages.add(new GuiCheck.Page(m, sub, (String)null, true, (String)null));
                  }
               }
            }
         }

         if(l.tabbed()) {
            for(int mode : new int[]{1, 2, 3}) {
               this.pages.add(new GuiCheck.Page(m, l.firstPage().key, (String)null, false, (String)null, mode));
            }
         }
      }

      this.pages.add(new GuiCheck.Page((Module)null, (String)null, (String)null, false, "#settings"));
      this.pages.add(new GuiCheck.Page((Module)null, (String)null, (String)null, false, "#presets"));
      this.pages.add(new GuiCheck.Page((Module)null, (String)null, (String)null, false, "#search"));
   }

   private String state() {
      String[] c = (String[])this.configs.get(Math.max(0, this.config));
      return c[0] + " " + c[1] + "% " + c[2] + " panel, " + c[3] + " sidebar";
   }

   boolean beforeFrame(FuriahScreen s) {
      ++this.frames;
      if(this.wait <= 0) {
         if(this.config < 0) {
            this.nextConfig(s);
            return false;
         } else {
            float[] e = s.checkScroll();
            float max = e[0];
            float view = e[1];
            if(this.scroll < max - 0.5F) {
               this.scroll = Math.min(max, this.scroll + Math.max(40.0F, view * 0.8F));
               s.checkScrollTo(this.scroll);
               this.wait = 2;
               this.measureNext = true;
               return false;
            } else {
               ++this.pageIndex;
               if(this.pageIndex >= this.pages.size()) {
                  this.pageIndex = 0;
                  if(this.config + 1 >= this.configs.size()) {
                     this.finish(s, true);
                     return false;
                  } else {
                     this.nextConfig(s);
                     return false;
                  }
               } else {
                  this.showPage(s);
                  return false;
               }
            }
         }
      } else {
         --this.wait;
         return this.wait == 0 && this.measureNext;
      }
   }

   private void nextConfig(FuriahScreen s) {
      ++this.config;
      String[] c = (String[])this.configs.get(this.config);
      MenuPrefs.VIEW_THEME.set(Density.Mode.valueOf(c[0]));
      MenuPrefs.GUI_SCALE.set((double)Integer.parseInt(c[1]));
      MenuPrefs.GUI_SCALE_AUTO.set(false);
      MenuPrefs.SIDEBAR.set(MenuPrefs.SidebarStyle.valueOf(c[3]));
      if(c[2].equals("small")) {
         MenuPrefs.FULL_SCREEN.set(false);
         MenuPrefs.PANEL_W.set(1.0D);
         MenuPrefs.PANEL_H.set(1.0D);
      } else {
         MenuPrefs.FULL_SCREEN.set(true);
      }

      this.pageIndex = 0;
      this.showPage(s);
      this.wait = 14;
   }

   private void showPage(FuriahScreen s) {
      GuiCheck.Page p = (GuiCheck.Page)this.pages.get(this.pageIndex);
      s.checkShow(p);
      this.scroll = 0.0F;
      s.checkScrollTo(0.0F);
      this.wait = Math.max(this.wait, 5);
      this.measureNext = true;
   }

   void measured(FuriahScreen s, List<UiProbe.Item> items) {
      this.measureNext = false;
      ++this.states;
      GuiCheck.Page p = (GuiCheck.Page)this.pages.get(this.pageIndex);

      for(UiProbe.Overlap o : UiProbe.overlaps(new ArrayList(items))) {
         String key = p.name() + "|" + o.a.label + "|" + o.b.label;
         GuiCheck.Found f = (GuiCheck.Found)this.found.get(key);
         if(f != null) {
            ++f.times;
         } else {
            this.found.put(key, new GuiCheck.Found(p.name(), this.state() + ", scrolled " + Math.round(this.scroll), o.toString()));
         }
      }

   }

   void abort(FuriahScreen s) {
      this.finish(s, false);
   }

   private void finish(FuriahScreen s, boolean done) {
      running = null;
      Easing.instant = false;
      MenuPrefs.VIEW_THEME.set(this.savedDensity);
      MenuPrefs.GUI_SCALE.set((double)this.savedScale);
      MenuPrefs.GUI_SCALE_AUTO.set(this.savedAuto);
      MenuPrefs.PANEL_W.set((double)this.savedPanelW);
      MenuPrefs.PANEL_H.set((double)this.savedPanelH);
      MenuPrefs.FULL_SCREEN.set(this.savedFull);
      MenuPrefs.SIDEBAR.set(this.savedSidebar);
      MenuMemory.restore(this.savedMemory);
      if(this.savedUiTab != null) {
         FuriahsVisuals.instance.getUi().selectForTest(this.savedUiTab);
      }

      Module weapons;
      if((weapons = FuriahsVisuals.instance.getModules().get("weapons")) instanceof WeaponsModule) {
         ((WeaponsModule)weapons).compareForTest(false, -1.0F);
      }

      s.checkDone();
      String summary = String.format(Locale.ROOT, "GUI check %s: %d overlap%s in %d states (%d configurations, %d pages, %d frames, %.0f s)", new Object[]{done?"done":"stopped", Integer.valueOf(this.found.size()), this.found.size() == 1?"":"s", Integer.valueOf(this.states), Integer.valueOf(Math.min(this.configs.size(), this.config + 1)), Integer.valueOf(this.pages.size()), Integer.valueOf(this.frames), Double.valueOf((double)(System.currentTimeMillis() - this.started) / 1000.0D)});
      File out = new File(Minecraft.getMinecraft().mcDataDir, "logs/hauntvisuals-gui-check.txt");
      StringBuilder sb = (new StringBuilder(summary)).append('\n');

      for(GuiCheck.Found f : this.found.values()) {
         sb.append('\n').append(f.page).append("  (").append(f.state).append(f.times > 1?", " + f.times + " times":"").append(")\n  ").append(f.what).append('\n');
      }

      try {
         out.getParentFile().mkdirs();
         Files.write(out.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
      } catch (IOException var9) {
         FuriahsVisuals.LOG.warn("GUI check: could not write {}", new Object[]{out, var9});
      }

      FuriahsVisuals.LOG.info(summary);

      for(GuiCheck.Found f : this.found.values()) {
         FuriahsVisuals.LOG.info("GUI check: {} ({}): {}", new Object[]{f.page, f.state, f.what});
      }

      FuriahsVisuals.chat(summary + (this.found.isEmpty()?".":": see logs/hauntvisuals-gui-check.txt"));
      lastSummary = summary;
      lastCount = this.found.size();
      if(done && IslandCheck.start(summary, this.found.size())) {
         s.closeForCheck();
      } else if(done) {
         FuriahsVisuals.LOG.info("GUI check: the island\'s part skipped (it needs a world and the WaterMark on)");
      }

   }

   public static void islandDone(String summary, int count) {
      lastSummary = summary;
      lastCount = count;
   }

   public static boolean isRunning() {
      return running != null || IslandCheck.isRunning();
   }

   private static final class Found {
      final String page;
      final String state;
      final String what;
      int times = 1;

      Found(String page, String state, String what) {
         this.page = page;
         this.state = state;
         this.what = what;
      }
   }

   static final class Page {
      final Module module;
      final String sub;
      final String weaponTab;
      final boolean previewsFolded;
      final String special;
      final int tabMode;

      Page(Module module, String sub, String weaponTab, boolean previewsFolded, String special) {
         this(module, sub, weaponTab, previewsFolded, special, -1);
      }

      Page(Module module, String sub, String weaponTab, boolean previewsFolded, String special, int tabMode) {
         this.module = module;
         this.sub = sub;
         this.weaponTab = weaponTab;
         this.previewsFolded = previewsFolded;
         this.special = special;
         this.tabMode = tabMode;
      }

      String name() {
         if(this.special != null) {
            return this.special;
         } else {
            StringBuilder sb = new StringBuilder(this.module.getName());
            if(this.sub != null && !this.sub.isEmpty()) {
               sb.append(" › ").append(this.sub);
            }

            if(this.weaponTab != null) {
               sb.append(" [preview ").append(this.weaponTab).append(']');
            }

            if(this.previewsFolded) {
               sb.append(" [previews folded]");
            }

            if(this.tabMode >= 0) {
               sb.append(this.tabMode == 1?" [tabs: short names]":(this.tabMode == 2?" [tabs: icons]":" [tabs: ⋯]"));
            }

            return sb.toString();
         }
      }
   }
}
