package gjum.minecraft.forge.morechunks;

import org.apache.logging.log4j.Level;

public class MockEnv implements IEnv {
    long nowMs = 0;

    @Override
    public long currentTimeMillis() {
        return nowMs;
    }

    @Override
    public void log(Level level, String format, Object... args) {
        System.out.println(level + " " + String.format(format, args));
    }
}
