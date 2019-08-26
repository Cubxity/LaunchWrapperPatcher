package me.lpk.util;

import me.cubxity.libs.org.objectweb.asm.ClassReader;
import me.cubxity.libs.org.objectweb.asm.tree.ClassNode;

public class ASMUtils {
    public static ClassNode getNode(final byte[] bytez) {
        ClassReader cr = new ClassReader(bytez);
        ClassNode cn = new ClassNode();
        try {
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
        } catch (Exception e) {
            try {
                cr.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            } catch (Exception ignored) {
            }
        }
        return cn;
    }
}
