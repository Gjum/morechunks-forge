package gjum.minecraft.forge.morechunks;

import org.apache.logging.log4j.Level;

public interface IEnv {
    long currentTimeMillis();

    void log(Level level, String format, Object... args);
}
