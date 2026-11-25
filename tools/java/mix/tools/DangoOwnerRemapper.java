package mix.tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public final class DangoOwnerRemapper {

    private DangoOwnerRemapper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("usage: DangoOwnerRemapper <in.jar> <out.jar> <oldOwner> <newOwner>");
            System.exit(2);
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        String oldOwner = args[2];
        String newOwner = args[3];
        Path temp = Paths.get(output.toString() + ".owner-tmp");
        int changed = 0;

        try (ZipFile zip = new ZipFile(input.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(temp))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] bytes = read(zip.getInputStream(entry));
                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassRemapper(writer, new Remapper() {
                        @Override
                        public String map(String internalName) {
                            return oldOwner.equals(internalName) ? newOwner : super.map(internalName);
                        }
                    }), 0);
                    byte[] remapped = writer.toByteArray();
                    if (!java.util.Arrays.equals(bytes, remapped)) {
                        changed++;
                    }
                    bytes = remapped;
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(entry.getTime());
                out.putNextEntry(copy);
                out.write(bytes);
                out.closeEntry();
            }
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[DangoOwnerRemapper] remapped " + changed + " class(es)");
    }

    private static byte[] read(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count;
        while ((count = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, count);
        }
        input.close();
        return buffer.toByteArray();
    }
}
