package mix.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rewrites the {@code @Shadow} and {@code @Overwrite} member names of the ported
 * Mix UI mixins inside {@code MixClient-notch.jar}.
 *
 * <p>SpecialSource remaps the mixins' <em>references</em> (an {@code @At} target
 * descriptor, a {@code @Mixin} class literal) but leaves a {@code @Shadow}
 * declaration and an {@code @Overwrite} handler alone: both are members of the
 * mixin class itself and no mapping covers them. Mixin then looks for the
 * deobfuscated name on an obfuscated class and the mixin fails to apply.
 *
 * <p>Which names to write depends on how the game classes reach Mixin:
 * <ul>
 *   <li>{@code srg} — Forge. FML deobfuscates vanilla Minecraft to SRG, so the
 *       game classes answer to {@code func_*} member names while keeping their
 *       deobfuscated class names. This is what every 1.8.9 Forge client targets
 *       (LabyMod, for instance, builds against Mixin 0.7.11 with
 *       {@code defaultObfuscationEnv searge}).</li>
 *   <li>{@code notch} — OptiFine and the plain launch, where the classes are
 *       handed to Mixin obfuscated and no refmap is applied.</li>
 * </ul>
 *
 * <p>The rename is keyed by deobfuscated name <em>and</em> descriptor. BlockPos
 * alone has six no-arg methods that all share {@code ()Lcj;}, and
 * {@code up()} / {@code up(int)} map to two different names, so neither the name
 * nor the descriptor alone identifies a member.
 */
public final class ShadowRemapper {

    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

    private static final String[] INJECTIONS = {
        "Lorg/spongepowered/asm/mixin/injection/Inject;",
        "Lorg/spongepowered/asm/mixin/injection/Redirect;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;"
    };

    /** {@code "method()V": "Lave;am()V"} -> {@code "method()V": "am()V"} */
    private static final Pattern REFMAP_MEMBER =
            Pattern.compile("\"([^\";]*)\"\\s*:\\s*\"L[^;]*;([^\"]*)\"");

    /** notch class -> deobfuscated class */
    private final Map<String, String> notchToMcp = new HashMap<String, String>();
    /** {@code mcpClass|mcpName|notchDesc} -> mcpDesc */
    private final Map<String, String> notchDescToMcp = new HashMap<String, String>();
    /** {@code mcpClass|mcpName|mcpDesc} -> srgName */
    private final Map<String, String> srgMethods = new HashMap<String, String>();
    /** {@code mcpClass|mcpName} -> srgName */
    private final Map<String, String> srgFields = new HashMap<String, String>();
    /** {@code notchClass|mcpName|notchDesc} -> notchName (notch namespace) */
    private final Map<String, String> notchMethods = new HashMap<String, String>();
    /** {@code notchClass|mcpName} -> notchName (notch namespace) */
    private final Map<String, String> notchFields = new HashMap<String, String>();

    private final String namespace;

    public ShadowRemapper(String namespace) {
        this.namespace = namespace;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: ShadowRemapper <in.jar> <out.jar> <mappingsDir> <notch|srg> [mixinPackagePrefix]");
            System.exit(2);
        }
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        Path mappings = Paths.get(args[2]);
        String ns = args[3];
        String prefix = args.length > 4 ? args[4] : "mix/client/mixui/injection/";

        ShadowRemapper tool = new ShadowRemapper(ns);
        tool.load(mappings);
        tool.process(in, out, prefix);
    }

    private void load(Path dir) throws IOException {
        parseSrg(dir.resolve("notch-mcp.srg"), Mode.NOTCH_TO_MCP);
        if ("srg".equals(namespace)) {
            parseSrg(dir.resolve("mcp-srg.srg"), Mode.MCP_TO_SRG);
        } else {
            parseSrg(dir.resolve("mcp-notch.srg"), Mode.MCP_TO_NOTCH);
        }
    }

    private enum Mode {
        NOTCH_TO_MCP, MCP_TO_SRG, MCP_TO_NOTCH
    }

    private void parseSrg(Path file, Mode mode) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("CL: ")) {
                    String[] p = line.substring(4).split(" ");
                    if (p.length != 2) {
                        continue;
                    }
                    if (mode == Mode.NOTCH_TO_MCP) {
                        /* notch-mcp.srg is written notch first */
                        notchToMcp.put(p[0], p[1]);
                    } else {
                        notchToMcp.put(p[1], p[0]);
                    }
                } else if (line.startsWith("MD: ")) {
                    parseMethod(line.substring(4), mode);
                } else if (line.startsWith("FD: ")) {
                    parseField(line.substring(4), mode);
                }
            }
        }
    }

    /** {@code <mcpClass>/<mcpName> <mcpDesc> <targetClass>/<targetName> <targetDesc>} */
    private void parseMethod(String body, Mode mode) {
        String[] p = body.split(" ");
        if (p.length != 4) {
            return;
        }
        switch (mode) {
            case NOTCH_TO_MCP: {
                /* notch-mcp.srg is written notch first:
                   <notchClass>/<notchName> <notchDesc> <mcpClass>/<mcpName> <mcpDesc> */
                String mcpOwner = p[2].substring(0, p[2].lastIndexOf('/'));
                String mcpName = p[2].substring(p[2].lastIndexOf('/') + 1);
                notchDescToMcp.put(mcpOwner + "|" + mcpName + "|" + p[1], p[3]);
                break;
            }
            case MCP_TO_SRG: {
                String mcpOwner = p[0].substring(0, p[0].lastIndexOf('/'));
                String mcpName = p[0].substring(p[0].lastIndexOf('/') + 1);
                String targetName = p[2].substring(p[2].lastIndexOf('/') + 1);
                srgMethods.put(mcpOwner + "|" + mcpName + "|" + p[1], targetName);
                break;
            }
            case MCP_TO_NOTCH: {
                String targetOwner = p[2].substring(0, p[2].lastIndexOf('/'));
                String mcpName = p[0].substring(p[0].lastIndexOf('/') + 1);
                String targetName = p[2].substring(p[2].lastIndexOf('/') + 1);
                notchMethods.put(targetOwner + "|" + mcpName + "|" + p[3], targetName);
                break;
            }
            default:
                break;
        }
    }

    /** {@code <mcpClass>/<mcpName> <targetClass>/<targetName>} */
    private void parseField(String body, Mode mode) {
        String[] p = body.split(" ");
        if (p.length != 2) {
            return;
        }
        String mcpClass = p[0].substring(0, p[0].lastIndexOf('/'));
        String mcpName = p[0].substring(p[0].lastIndexOf('/') + 1);
        String targetOwner = p[1].substring(0, p[1].lastIndexOf('/'));
        String targetName = p[1].substring(p[1].lastIndexOf('/') + 1);

        switch (mode) {
            case MCP_TO_SRG:
                srgFields.put(mcpClass + "|" + mcpName, targetName);
                break;
            case MCP_TO_NOTCH:
                notchFields.put(targetOwner + "|" + mcpName, targetName);
                break;
            default:
                break;
        }
    }

    private void process(Path in, Path out, String prefix) throws IOException {
        int classes = 0;
        int refmaps = 0;

        Path temp = Paths.get(out.toString() + ".shadow-tmp");
        try (ZipFile zip = new ZipFile(in.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(temp))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] bytes = read(zip.getInputStream(entry));
                if (entry.getName().endsWith(".refmap.json")) {
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    String stripped = REFMAP_MEMBER.matcher(json).replaceAll("\"$1\": \"$2\"");
                    if (!stripped.equals(json)) {
                        bytes = stripped.getBytes(StandardCharsets.UTF_8);
                        refmaps++;
                    }
                } else if (entry.getName().startsWith(prefix) && entry.getName().endsWith(".class")) {
                    byte[] patched = patch(bytes);
                    if (patched != null) {
                        bytes = patched;
                        classes++;
                    }
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(entry.getTime());
                zos.putNextEntry(copy);
                zos.write(bytes);
                zos.closeEntry();
            }
        }
        Files.move(temp, out, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[ShadowRemapper] namespace=" + namespace + ", patched " + classes
                + " mixin classes, stripped owners from " + refmaps + " refmap(s)");
    }

    private byte[] patch(byte[] bytes) {
        ClassNode node = new ClassNode();
        new org.objectweb.asm.ClassReader(bytes).accept(node, 0);

        String target = targetClass(node);
        if (target == null) {
            return null;
        }

        Map<String, String> renames = new HashMap<String, String>();
        for (MethodNode method : node.methods) {
            if (isShadow(method.visibleAnnotations) || isOverwrite(method.visibleAnnotations)) {
                String mapped = mapMethodName(target, method.name, method.desc);
                if (mapped != null && !mapped.equals(method.name)) {
                    renames.put(method.name + method.desc, mapped);
                    method.name = mapped;
                }
            }
        }
        for (FieldNode field : node.fields) {
            if (isShadow(field.visibleAnnotations)) {
                String mapped = mapFieldName(target, field.name);
                if (mapped != null && !mapped.equals(field.name)) {
                    renames.put(field.name + field.desc, mapped);
                    field.name = mapped;
                }
            }
        }

        stripOwners(node, target);
        rewriteUses(node, renames);

        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * The target class name in the namespace the game classes are in: the
     * obfuscated name for the notch build, the deobfuscated name for SRG (FML
     * keeps class names readable and only renames members).
     */
    private String targetClass(ClassNode node) {
        String declared = declaredTarget(node);
        if (declared == null) {
            return null;
        }
        if ("srg".equals(namespace)) {
            String mcp = notchToMcp.get(declared);
            return mcp != null ? mcp : declared;
        }
        return declared;
    }

    private String declaredTarget(ClassNode node) {
        List<AnnotationNode> annotations = new java.util.ArrayList<AnnotationNode>();
        if (node.visibleAnnotations != null) {
            annotations.addAll(node.visibleAnnotations);
        }
        if (node.invisibleAnnotations != null) {
            annotations.addAll(node.invisibleAnnotations);
        }
        for (AnnotationNode annotation : annotations) {
            if (!MIXIN.equals(annotation.desc) || annotation.values == null) {
                continue;
            }
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (!(annotation.values.get(i) instanceof String)) {
                    continue;
                }
                String key = (String) annotation.values.get(i);
                if (!"value".equals(key) && !"targets".equals(key)) {
                    continue;
                }
                String declared = firstClassName(annotation.values.get(i + 1));
                if (declared != null) {
                    return declared;
                }
            }
        }
        return null;
    }

    private String mapMethodName(String target, String name, String notchDesc) {
        if ("srg".equals(namespace)) {
            String mcpDesc = notchDescToMcp.get(target + "|" + name + "|" + notchDesc);
            if (mcpDesc == null) {
                return null;
            }
            return srgMethods.get(target + "|" + name + "|" + mcpDesc);
        }
        return notchMethods.get(target + "|" + name + "|" + notchDesc);
    }

    private String mapFieldName(String target, String name) {
        if ("srg".equals(namespace)) {
            return srgFields.get(target + "|" + name);
        }
        return notchFields.get(target + "|" + name);
    }

    private void rewriteUses(ClassNode node, Map<String, String> renames) {
        if (renames.isEmpty()) {
            return;
        }
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.owner.equals(node.name)) {
                        String r = renames.get(call.name + call.desc);
                        if (r != null) {
                            call.name = r;
                        }
                    }
                } else if (insn instanceof FieldInsnNode) {
                    FieldInsnNode access = (FieldInsnNode) insn;
                    if (access.owner.equals(node.name)) {
                        String r = renames.get(access.name + access.desc);
                        if (r != null) {
                            access.name = r;
                        }
                    }
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (indy.bsmArgs[i] instanceof Handle) {
                            Handle handle = (Handle) indy.bsmArgs[i];
                            if (handle.getOwner().equals(node.name)) {
                                String r = renames.get(handle.getName() + handle.getDesc());
                                if (r != null) {
                                    indy.bsmArgs[i] = new Handle(handle.getTag(), handle.getOwner(), r,
                                            handle.getDesc(), handle.isInterface());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String firstClassName(Object value) {
        if (value instanceof Type) {
            return ((Type) value).getInternalName();
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                return firstClassName(list.get(0));
            }
        }
        return null;
    }

    private static boolean isShadow(List<AnnotationNode> annotations) {
        return has(annotations, SHADOW);
    }

    private static boolean isOverwrite(List<AnnotationNode> annotations) {
        return has(annotations, OVERWRITE);
    }

    private static boolean has(List<AnnotationNode> annotations, String desc) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (desc.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInjection(String desc) {
        for (String injection : INJECTIONS) {
            if (injection.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    private static void stripOwners(ClassNode node, String targetNotch) {
        for (MethodNode method : node.methods) {
            stripFrom(method.visibleAnnotations, targetNotch);
            stripFrom(method.invisibleAnnotations, targetNotch);
        }
    }

    private static void stripFrom(List<AnnotationNode> annotations, String targetNotch) {
        if (annotations == null) {
            return;
        }
        for (AnnotationNode annotation : annotations) {
            if (!isInjection(annotation.desc) || annotation.values == null) {
                continue;
            }
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (!"method".equals(annotation.values.get(i))) {
                    continue;
                }
                Object value = annotation.values.get(i + 1);
                if (value instanceof String) {
                    annotation.values.set(i + 1, stripOwner((String) value, targetNotch));
                } else if (value instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> list = (java.util.List<Object>) value;
                    for (int j = 0; j < list.size(); j++) {
                        if (list.get(j) instanceof String) {
                            list.set(j, stripOwner((String) list.get(j), targetNotch));
                        }
                    }
                }
            }
        }
    }

    private static String stripOwner(String method, String targetNotch) {
        if (method == null || !method.startsWith("L")) {
            return method;
        }
        int semi = method.indexOf(';');
        if (semi > 0 && method.substring(1, semi).equals(targetNotch)) {
            return method.substring(semi + 1);
        }
        return method;
    }

    private static byte[] read(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        input.close();
        return buffer.toByteArray();
    }
}
