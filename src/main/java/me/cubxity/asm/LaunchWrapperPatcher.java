package me.cubxity.asm;

import me.cubxity.libs.org.objectweb.asm.tree.*;
import me.lpk.util.JarUtils;
import me.cubxity.libs.org.objectweb.asm.ClassWriter;
import me.cubxity.libs.org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows you to redirect exception from unable to launch catch
 *
 * @author Cubxity
 */
public class LaunchWrapperPatcher {

    /**
     * Patches the launchWrapper
     *
     * @param file        launchwrapper (supported version: 1.7)
     * @param targetOwner class that contains the method to execute
     * @param targetName  method name NOTE: Method must be {@code static}
     * @param targetDesc  method descriptor, must have only one param with type {@link java.lang.Exception}
     * @param out         output jar
     */
    public static void patch(File file, String targetOwner, String targetName, String targetDesc, File out) throws IOException {
        Map<String, ClassNode> entries = JarUtils.loadClasses(file);
        ClassNode lc = entries.get("net/minecraft/launchwrapper/Launch");
        MethodNode launch = lc.methods.stream().filter(mn -> mn.name.equals("launch")).findFirst().get();
        AbstractInsnNode log = null;
        for (AbstractInsnNode node : launch.instructions.toArray())
            if (node.getOpcode() == Opcodes.INVOKESTATIC)
                log = node; // Find last node of invokestatic
        InsnList insns = new InsnList();
        insns.add(new VarInsnNode(Opcodes.ALOAD, 14)); // load exception variable
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, targetOwner, targetName, targetDesc, false)); // instruction to call the method
        launch.instructions.insert(log, insns);

        Map<String, byte[]> bytes = new HashMap<>();
        for (Map.Entry<String, ClassNode> e : entries.entrySet()) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            e.getValue().accept(cw);
            bytes.put(e.getKey(), cw.toByteArray());
        }
        bytes.putAll(JarUtils.loadNonClassEntries(file));

        JarUtils.saveAsJar(bytes, out.getAbsolutePath());
    }
}
