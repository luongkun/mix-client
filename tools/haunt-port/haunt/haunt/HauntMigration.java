package mix.client.mixui.integration.haunt.haunt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.Date;
import mix.client.mixui.integration.haunt.core.FuriahsVisuals;

public final class HauntMigration {
   public static void run(File configDir) {
      File newFile = new File(configDir, "hauntvisuals.json");
      File oldFile = new File(configDir, "furiahsvisuals.json");
      File newDir = new File(configDir, "hauntvisuals");
      File oldDir = new File(configDir, "furiahsvisuals");
      StringBuilder note = new StringBuilder();

      try {
         if(!newFile.exists() && oldFile.isFile()) {
            Files.copy(oldFile.toPath(), newFile.toPath(), new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES});
            note.append("copied ").append(oldFile.getName()).append(" -> ").append(newFile.getName()).append('\n');
            FuriahsVisuals.LOG.info("Migration: your settings were copied from {} (the old file stays as a backup)", new Object[]{oldFile.getName()});
         }
      } catch (IOException var9) {
         FuriahsVisuals.LOG.error("Migration: could not copy {}; it is left untouched", new Object[]{oldFile, var9});
      }

      try {
         if(!newDir.exists() && oldDir.isDirectory()) {
            copyTree(oldDir.toPath(), newDir.toPath());
            note.append("copied folder ").append(oldDir.getName()).append(" -> ").append(newDir.getName()).append('\n');
            FuriahsVisuals.LOG.info("Migration: presets, skins, weapons and images were copied from config/{} (the old folder stays as a backup)", new Object[]{oldDir.getName()});
         }
      } catch (IOException var81) {
         FuriahsVisuals.LOG.error("Migration: could not copy the folder {}; it is left untouched", new Object[]{oldDir, var81});
      }

      if(note.length() > 0) {
         try {
            newDir.mkdirs();
            String stamp = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
            Files.write((new File(newDir, "MIGRATED_FROM_FURIAHS_VISUALS.txt")).toPath(), ("HAUN\'T VISUALS 2.0.0 brought these over on " + stamp + ":\n" + note + "The originals were left in place as a backup; nothing was deleted.\n").getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
         } catch (IOException var7) {
            ;
         }
      }

   }

   private static void copyTree(final Path from, final Path to) throws IOException {
      Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Files.createDirectories(to.resolve(from.relativize(dir).toString()), new FileAttribute[0]);
            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path target = to.resolve(from.relativize(file).toString());
            if(!Files.exists(target, new LinkOption[0])) {
               Files.copy(file, target, new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES});
            }

            return FileVisitResult.CONTINUE;
         }
      });
   }
}
