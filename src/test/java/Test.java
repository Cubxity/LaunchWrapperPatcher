import me.cubxity.asm.LaunchWrapperPatcher;

import java.io.File;
import java.io.IOException;

/*
 * Created by Cubxity on 19/07/2018
 */
public class Test {
    public static void main(String[] args) throws IOException {
        LaunchWrapperPatcher.patch(new File("launchwrapper-1.7.jar"), "cc/hyperium/utils/CrashHandler", "handle", "(Ljava/lang/Exception;)V", new File("launchwrapper-mod.jar"));
    }
}
