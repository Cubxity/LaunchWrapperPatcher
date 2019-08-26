package me.lpk.util;

import me.cubxity.libs.org.apache.commons.io.IOUtils;
import me.cubxity.libs.org.objectweb.asm.tree.ClassNode;
import me.cubxity.libs.org.objectweb.asm.tree.MethodNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarUtils {
    public static Map<String, ClassNode> loadClasses(File jarFile) throws IOException {
        Map<String, ClassNode> classes = new HashMap<String, ClassNode>();
        JarFile jar = new JarFile(jarFile);
        Stream<JarEntry> str = jar.stream();
        str.forEach(z -> readJar(jar, z, classes, null));
        jar.close();
        return classes;
    }

    private static void readJar(JarFile jar, JarEntry en, Map<String, ClassNode> classes, List<String> ignored) {
        String name = en.getName();
        try (InputStream jis = jar.getInputStream(en)) {
            if (name.endsWith(".class")) {
                if (ignored != null) {
                    for (String s : ignored) {
                        if (name.startsWith(s)) {
                            return;
                        }
                    }
                }
                byte[] bytes = IOUtils.toByteArray(jis);
                String cafebabe = String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]);
                if (cafebabe.toLowerCase().equals("cafebabe")) {
                    try {
                        final ClassNode cn = ASMUtils.getNode(bytes);
                        if (cn != null && (cn.name.equals("java/lang/Object") || cn.superName != null)) {
                            for (MethodNode mn : cn.methods) {
                                mn.owner = cn.name;
                            }
                            classes.put(cn.name, cn);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, byte[]> loadNonClassEntries(File jarFile) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        ZipInputStream jis = new ZipInputStream(new FileInputStream(jarFile));
        ZipEntry entry;
        while ((entry = jis.getNextEntry()) != null) {
            try {
                final String name = entry.getName();
                if (!name.endsWith(".class") && !entry.isDirectory()) {
                    byte[] bytes = IOUtils.toByteArray(jis);
                    entries.put(name, bytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                jis.closeEntry();
            }
        }
        jis.close();
        return entries;
    }

    public static void saveAsJar(Map<String, byte[]> outBytes, String fileName) {
        try {
            JarOutputStream out = new JarOutputStream(new java.io.FileOutputStream(fileName));
            for (String entry : outBytes.keySet()) {
                String ext = entry.contains(".") ? "" : ".class";
                out.putNextEntry(new ZipEntry(entry + ext));
                out.write(outBytes.get(entry));
                out.closeEntry();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
