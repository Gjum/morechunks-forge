package gjum.minecraft.forge.morechunks;

public class Env implements IEnv {
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
