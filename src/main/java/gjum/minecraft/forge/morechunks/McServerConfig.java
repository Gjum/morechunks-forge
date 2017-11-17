package gjum.minecraft.forge.morechunks;

import com.google.gson.annotations.Expose;

public class McServerConfig {
    @Expose()
    public final String mcServerAddress;
    @Expose()
    public final boolean enabled;
    @Expose()
    public final String chunkServerAddress;
    @Expose()
    public final int serverRenderDistance;

    public McServerConfig(String gameServerAddress, boolean enabled, String chunkServerAddress, int serverRenderDistance) {
        this.mcServerAddress = gameServerAddress;
        this.enabled = enabled;
        this.chunkServerAddress = chunkServerAddress;
        this.serverRenderDistance = serverRenderDistance;
    }
}
