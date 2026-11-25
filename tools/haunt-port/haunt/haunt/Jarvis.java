package mix.client.mixui.integration.haunt.haunt;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;
import mix.client.mixui.integration.haunt.module.Module;
import mix.client.mixui.integration.haunt.module.fps.FpsOptimizer;
import mix.client.mixui.integration.haunt.module.kill.KillStyle;
import mix.client.mixui.integration.haunt.module.sky.SkyModule;
import mix.client.mixui.integration.haunt.module.sky.SkyStyle;

public final class Jarvis {
   private static final Random RNG = new Random();
   private static final String[][] SECTIONS = new String[][]{{"target_aura", "target aura", "aura"}, {"hit_effect", "hit effect", "efeito de hit", "hit"}, {"kill_effects", "kill effect", "efeito de kill", "kill"}, {"landing_impact", "landing", "impacto", "queda"}, {"trails", "trail", "rastro"}, {"particles", "particula", "particles"}, {"weapons", "weapon", "arma"}, {"glow", "glow", "brilho"}, {"ambience", "ambience", "ambiente"}, {"dynamic_skies", "sky", "ceu", "skies"}, {"motion_blur", "motion blur", "blur", "desfoque"}, {"watermark", "watermark", "marca"}, {"ui", "hud", "interface"}, {"fps_optimization", "fps", "otimiza"}};
   private static final String[] CHEATS = new String[]{"cheat", "xit", "hack", "killaura", "kill aura", "aimbot", "aim assist", "reach", "autoclick", "auto click", "auto clicker", "wallhack", "xray", "x-ray", "velocity", "antikb", "anti kb", "no knockback", "fly hack", "speed hack", "triggerbot", "esp", "bhop"};

   static String norm(String s) {
      String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Form.NFD);
      return n.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 \\-]", " ").replaceAll("\\s+", " ").trim();
   }

   private static void say(String s) {
      FuriahsVisuals.chat("§5[JARVIS] §7" + s);
   }

   public static void handle(String request) {
      String q = " " + norm(request) + " ";
      if(!q.trim().isEmpty() && !has(q, new String[]{"ajuda", "help", "o que voce faz", "comandos"})) {
         if(has(q, CHEATS)) {
            say("Não posso ajudar com cheats, xits ou hacks. Posso deixar o visual do jogo mais bonito ou mais leve, se quiser.");
         } else if(has(q, new String[]{"otimiz", "fps", "travando", " lag"}) && !has(q, new String[]{" liga ", " ativa ", "desliga", "desativa", "turn on", "turn off"})) {
            if(has(q, new String[]{"para", "parar", "stop", "cancela"})) {
               FpsOptimizer.INSTANCE.stop();
               say("Otimização interrompida.");
            } else if(has(q, new String[]{"volta", "desfaz", "restaura", "restore"})) {
               FpsOptimizer.INSTANCE.restoreLast();
            } else {
               FpsOptimizer.INSTANCE.start();
               say("Vou medir seu FPS e testar uma mudança de cada vez; só fica o que realmente ajudar. Feche o menu e jogue normalmente.");
            }
         } else if(has(q, new String[]{"preset"})) {
            String p = FuriahsVisuals.instance.activePresetName();
            say(p != null && !p.isEmpty()?"Sua preset ativa é: " + p + " (só você vê esta mensagem).":"Nenhuma preset está ativa agora.");
         } else if(has(q, new String[]{" sky", "ceu", "skies"}) && has(q, new String[]{"tem ", "existe", "procura", "alguma", "quais", "lista"})) {
            findSkies(q);
         } else if(has(q, new String[]{" sky", "ceu"}) && has(q, new String[]{"sombri", "escur", "dark", "assustador", "terror", "halloween"})) {
            darkSky();
         } else if(has(q, new String[]{" sky ", " ceu "}) && has(q, new String[]{"coloca", "poe", "muda", "troca", "usa", "quero"})) {
            pickSky(q);
         } else if(has(q, new String[]{"kill effect", "efeito de kill", "kill "}) && has(q, new String[]{"coloca", "poe", "muda", "troca", "usa", "quero"})) {
            pickKill(q);
         } else if(has(q, new String[]{"liga", "ativa", "acende", "turn on", "enable", "desliga", "desativa", "apaga", "turn off", "disable"})) {
            toggle(q);
         } else {
            say("Não entendi bem. Exemplos: \"ativa minha Dynamic Sky\", \"coloca uma sky mais sombria\", \"tem alguma sky de eclipse?\", \"desliga os trails\", \"otimiza meu FPS\".");
         }
      } else {
         help();
      }

   }

   private static void help() {
      say("Eu cuido do HAUN\'T VISUALS por você: ligar e desligar seções, escolher Dynamic Skies e Kill Effects, mostrar sua preset e otimizar o FPS.");
      say("Escreva /jarvis e o pedido, por exemplo: /jarvis coloca uma sky mais sombria");
   }

   private static boolean has(String q, String... words) {
      for(String w : words) {
         if(q.contains(w)) {
            return true;
         }
      }

      return false;
   }

   private static void toggle(String q) {
      boolean on = !has(q, new String[]{"desliga", "desativa", "apaga", "turn off", "disable"});
      List<String> done = new ArrayList();

      label85:
      for(String[] s : SECTIONS) {
         for(int i = 1; i < s.length; ++i) {
            if(q.contains(s[i])) {
               Iterator var8 = FuriahsVisuals.instance.getModules().getModules().iterator();

               while(true) {
                  if(!var8.hasNext()) {
                     continue label85;
                  }

                  Module m = (Module)var8.next();
                  if(m.getId().equals(s[0])) {
                     m.setEnabled(on);
                     done.add(m.getName());
                  }
               }
            }
         }
      }

      if(done.isEmpty()) {
         say("Qual seção? Por exemplo: Dynamic Sky, Kill Effects, Trails, Target Aura, Weapons, Glow.");
      } else {
         say((on?"Ativei: ":"Desativei: ") + String.join(", ", done) + ".");
      }

   }

   private static boolean matches(SkyStyle s, String q) {
      String name = norm(s.displayName());

      for(String w : q.trim().split(" ")) {
         if(w.length() >= 4 && !w.equals("alguma") && !w.equals("tema") && !w.equals("existe") && !w.equals("procura")) {
            if(name.contains(w)) {
               return true;
            }

            String[] kw = s.keywords();
            if(kw != null) {
               for(String k : kw) {
                  if(norm(k).contains(w) || w.startsWith(norm(k))) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static void findSkies(String q) {
      List<String> found = new ArrayList();

      for(SkyStyle s : SkyStyle.values()) {
         if(matches(s, q.replace("eclipse", "eclipse ").replace(" lua ", " moon "))) {
            found.add(s.displayName());
         }
      }

      say(found.isEmpty()?"Não achei nenhuma sky com esse tema.":"Encontrei: " + String.join(", ", found) + ". Diga \"coloca a sky " + (String)found.get(0) + "\" para usar.");
   }

   private static void setSky(SkyStyle s) {
      SkyModule sky = FuriahsVisuals.instance.getSky();
      sky.style.set(s);
      sky.setEnabled(true);
      say("Pronto: Dynamic Sky agora é " + s.displayName() + ".");
   }

   private static void darkSky() {
      SkyStyle[] dark = new SkyStyle[]{SkyStyle.VOID, SkyStyle.BLOOD_MOON, SkyStyle.ECLIPSE, SkyStyle.CURSED_ECLIPSE, SkyStyle.GRAVEYARD_FOG, SkyStyle.HAUNTED_MOON, SkyStyle.SPECTRAL_STORM, SkyStyle.SEALED_DOMAIN, SkyStyle.MONOCHROME_HALFTONE, SkyStyle.ORGANIC_LIQUID};
      SkyStyle now = (SkyStyle)FuriahsVisuals.instance.getSky().style.get();

      SkyStyle pick;
      while(true) {
         pick = dark[RNG.nextInt(dark.length)];
         if(pick != now) {
            break;
         }
      }

      setSky(pick);
   }

   private static void pickSky(String q) {
      for(SkyStyle s : SkyStyle.values()) {
         if(q.contains(norm(s.displayName()))) {
            setSky(s);
            return;
         }
      }

      for(SkyStyle s : SkyStyle.values()) {
         if(matches(s, q)) {
            setSky(s);
            return;
         }
      }

      if(has(q, new String[]{"minha", "ativa"})) {
         FuriahsVisuals.instance.getSky().setEnabled(true);
         say("Dynamic Sky ativada.");
      } else {
         say("Não achei essa sky. Pergunte \"tem alguma sky de ...?\" para ver as opções.");
      }

   }

   private static void pickKill(String q) {
      for(KillStyle k : KillStyle.values()) {
         if(q.contains(norm(k.displayName()))) {
            FuriahsVisuals.instance.getKillEffects().style.set(k);
            FuriahsVisuals.instance.getKillEffects().setEnabled(true);
            say("Pronto: Kill Effect agora é " + k.displayName() + ".");
            return;
         }
      }

      say("Não achei esse Kill Effect. Exemplos: Sanguine Death, Void Sphere, Grave Hands.");
   }
}
