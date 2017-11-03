package gjum.minecraft.forge.morechunks;

public class MockEnv implements IEnv {
    long nowMs = 0;

    @Override
    public long currentTimeMillis() {
        return nowMs;
    }
}
