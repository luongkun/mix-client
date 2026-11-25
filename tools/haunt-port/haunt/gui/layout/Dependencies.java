package mix.client.mixui.integration.haunt.gui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import mix.client.mixui.integration.haunt.setting.Setting;
import mix.client.mixui.integration.haunt.setting.SettingProbe;

final class Dependencies {
   private static final int MAX_OPTIONS = 16;
   private static final int PAIR_REACH = 16;

   static Map<Setting, Setting> find(List<Setting> settings, Predicate<Setting> notSwitch) {
      IdentityHashMap<Setting, Integer> index = new IdentityHashMap();
      ArrayList<Setting> conditional = new ArrayList();
      ArrayList<Setting> switches = new ArrayList();

      for(int i = 0; i < settings.size(); ++i) {
         Setting s = (Setting)settings.get(i);
         index.put(s, Integer.valueOf(i));
         if(s.hasCondition()) {
            conditional.add(s);
         }

         Object[] objectArray = notSwitch.test(s)?null:SettingProbe.trials(s);
         if(objectArray != null && objectArray.length > 0) {
            switches.add(s);
         }
      }

      IdentityHashMap<Setting, ArrayList<Setting>> switchesOf = new IdentityHashMap();

      for(Setting setting : switches) {
         Object[] objectArray = SettingProbe.trials(setting);
         ArrayList<Setting> watch = new ArrayList();

         for(Setting setting2 : conditional) {
            if(setting2 != setting && (setting.getOwner() == null || setting.getOwner() == setting2.getOwner())) {
               watch.add(setting2);
            }
         }

         if(!watch.isEmpty()) {
            boolean[] base = new boolean[watch.size()];

            for(int j = 0; j < base.length; ++j) {
               base[j] = visible((Setting)watch.get(j));
            }

            boolean[] blArray = new boolean[watch.size()];
            Object was = SettingProbe.current(setting);

            try {
               for(Object t : objectArray) {
                  SettingProbe.set(setting, t);

                  for(int i = 0; i < base.length; ++i) {
                     if(!blArray[i] && visible((Setting)watch.get(i)) != base[i]) {
                        blArray[i] = true;
                     }
                  }
               }
            } finally {
               SettingProbe.set(setting, was);
            }

            for(int n = 0; n < blArray.length; ++n) {
               if(blArray[n]) {
                  ArrayList<Setting> list = (ArrayList)switchesOf.get(watch.get(n));
                  if(list == null) {
                     list = new ArrayList();
                     switchesOf.put(watch.get(n), list);
                  }

                  list.add(setting);
               }
            }
         }
      }

      IdentityHashMap<Setting, Boolean> byPair = new IdentityHashMap();

      for(Setting setting : conditional) {
         Setting[] pair;
         if(!switchesOf.containsKey(setting) && (pair = pair(setting, switches, index)) != null) {
            ArrayList list = new ArrayList();
            list.add(pair[0]);
            list.add(pair[1]);
            switchesOf.put(setting, list);
            byPair.put(setting, Boolean.TRUE);
         }
      }

      IdentityHashMap<Setting, Setting> identityHashMap = new IdentityHashMap();
      IdentityHashMap identityHashMap2 = new IdentityHashMap();

      for(int round = 0; round < 64; ++round) {
         identityHashMap.clear();

         for(Entry<Setting, ArrayList<Setting>> entry : switchesOf.entrySet()) {
            Setting c = (Setting)entry.getKey();
            int n = ((Integer)index.get(c)).intValue();
            boolean outer = byPair.containsKey(c);
            Setting best = null;
            int bestScore = Integer.MAX_VALUE;

            for(Setting p : (ArrayList)entry.getValue()) {
               if(!identityHashMap2.containsKey(p)) {
                  int pi = ((Integer)index.get(p)).intValue();
                  boolean sameStyle = p.getOwner() == c.getOwner();
                  if(!sameStyle || pi <= n) {
                     int distance = outer?pi:n - pi;
                     int score = (p.showInMenu()?0:1000000) + (sameStyle?distance:500000 + Math.abs(n - pi));
                     if(score < bestScore) {
                        bestScore = score;
                        best = p;
                     }
                  }
               }
            }

            if(best != null) {
               identityHashMap.put(c, best);
            }
         }

         IdentityHashMap<Setting, Integer> count = new IdentityHashMap();
         boolean bl = false;
         Iterator<Setting> iterator = identityHashMap.values().iterator();

         while(iterator.hasNext()) {
            Setting settingx;
            Integer n = (Integer)count.get(settingx = (Setting)iterator.next());
            count.put(settingx, Integer.valueOf(n == null?1:n.intValue() + 1));
         }

         for(Entry entry : count.entrySet()) {
            if(((Integer)entry.getValue()).intValue() > 16) {
               identityHashMap2.put(entry.getKey(), Boolean.TRUE);
               bl = true;
            }
         }

         if(!bl) {
            break;
         }
      }

      return identityHashMap;
   }

   private static Setting[] pair(Setting c, List<Setting> switches, final Map<Setting, Integer> index) {
      final int ci = ((Integer)index.get(c)).intValue();
      List near = new ArrayList();

      for(Setting p : switches) {
         if(p != c && (p.getOwner() == null || p.getOwner() == c.getOwner()) && (p.getOwner() != c.getOwner() || ((Integer)index.get(p)).intValue() <= ci)) {
            near.add(p);
         }
      }

      Collections.sort(near, new Comparator<Setting>() {
         public int compare(Setting a, Setting b) {
            return Math.abs(((Integer)index.get(a)).intValue() - ci) - Math.abs(((Integer)index.get(b)).intValue() - ci);
         }
      });
      if(near.size() > 16) {
         near = near.subList(0, 16);
      }

      boolean base = visible(c);

      for(int i = 0; i < ((List)near).size(); ++i) {
         Setting a = (Setting)near.get(i);
         Object[] ta = SettingProbe.trials(a);
         Object wasA = SettingProbe.current(a);

         for(int j = i + 1; j < ((List)near).size(); ++j) {
            Setting b = (Setting)near.get(j);
            Object[] tb = SettingProbe.trials(b);
            Object wasB = SettingProbe.current(b);

            try {
               for(Object va : ta) {
                  SettingProbe.set(a, va);

                  for(Object vb : tb) {
                     SettingProbe.set(b, vb);
                     if(visible(c) != base) {
                        Setting[] var23 = new Setting[]{a, b};
                        return var23;
                     }
                  }

                  SettingProbe.set(b, wasB);
               }
            } finally {
               SettingProbe.set(b, wasB);
               SettingProbe.set(a, wasA);
            }
         }
      }

      return null;
   }

   private static boolean visible(Setting s) {
      try {
         return s.isVisibleIgnoringStyle();
      } catch (RuntimeException var2) {
         return false;
      }
   }
}
