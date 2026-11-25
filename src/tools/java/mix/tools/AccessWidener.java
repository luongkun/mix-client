package mix.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public final class AccessWidener {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: AccessWidener <in.jar> <out.jar>");
            System.exit(2);
        }
        try (JarInputStream input = new JarInputStream(new FileInputStream(args[0]));
             JarOutputStream output = new JarOutputStream(new FileOutputStream(args[1]))) {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA")
                        || name.endsWith(".DSA") || name.equals("META-INF/INDEX.LIST"))) {
                    continue;
                }
                byte[] data = read(input);
                if (name.endsWith(".class")) {
                    ClassReader reader = new ClassReader(data);
                    ClassWriter writer = new ClassWriter(0);
                    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM7, writer) {
                        @Override
                        public FieldVisitor visitField(int access, String fieldName, String descriptor,
                                                       String signature, Object value) {
                            int widened = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED))
                                    | Opcodes.ACC_PUBLIC;
                            return super.visitField(widened, fieldName, descriptor, signature, value);
                        }
                    };
                    reader.accept(visitor, 0);
                    data = writer.toByteArray();
                }
                JarEntry copy = new JarEntry(name);
                output.putNextEntry(copy);
                output.write(data);
                output.closeEntry();
            }
        }
    }

    private static byte[] read(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
