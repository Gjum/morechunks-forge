package gjum.minecraft.forge.morechunks;

import org.apache.logging.log4j.Level;

public class Env implements IEnv {
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public void log(String source, Level level, String format, Object... args) {
        format = String.format("[%s] [%s] %s \n", level, source, format);
        System.out.printf(format, args);
    }
}
