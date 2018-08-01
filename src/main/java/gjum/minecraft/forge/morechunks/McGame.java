package gjum.minecraft.forge.morechunks;

import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McGame implements IMcGame {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final HashMap<Pos2, Long> chunkLoadTimes = new HashMap<>();

    private IEnv env;
    private PacketHandler packetHandler;

    public McGame(IEnv env) {
        this.env = env;
    }

    @Override
    public void clearChunkCache() {
        chunkLoadTimes.clear();
    }

    @Override
    public String getCurrentServerIp() {
        if (null == mc.getCurrentServerData()) return null;
        return mc.getCurrentServerData().serverIP;
    }

    @Override
    public Map<Pos2, Long> getChunkLoadTimes() {
        // make sure chunkLoadTimes isn't modified concurrently
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling getChunkLoadTimes from non-mc-thread");
        }

        return new HashMap<>(chunkLoadTimes);
    }

    @Override
    public List<Pos2> getLoadedChunks() {
        // make sure chunkLoadTimes isn't modified concurrently
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling getLoadedChunks from non-mc-thread");
        }

        return new ArrayList<>(chunkLoadTimes.keySet());
    }

    @Override
    public Pos2 getPlayerChunkPos() {
        if (mc.player.posX == 0 && mc.player.posY == 0 && mc.player.posZ == 0) return null;
        if (mc.player.posX == 8.5 && mc.player.posY == 65 && mc.player.posZ == 8.5) return null; // position not set from server yet
        return Pos2.chunkPosFromBlockPos(mc.player.posX, mc.player.posZ);
    }

    @Override
    public int getPlayerDimension() {
        return mc.player.dimension;
    }

    @Override
    public int getRenderDistance() {
        return mc.gameSettings.renderDistanceChunks;
    }

    @Override
    public void insertPacketHandler(IMoreChunks moreChunks) {
        NetHandlerPlayClient mcConnection = (NetHandlerPlayClient) FMLClientHandler.instance().getClientPlayHandler();
        if (mcConnection == null) {
            env.log(Level.ERROR, "Could not inject packet handler into pipeline: mc.connection == null");
            return;
        }

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();

        if (pipe.get(PacketHandler.NAME) != null) {
            env.log(Level.WARN, "game server connection pipeline already contains " + PacketHandler.NAME + ", removing and re-adding");
            pipe.remove(PacketHandler.NAME);
        }

        packetHandler = new PacketHandler(moreChunks);
        pipe.addBefore("fml:packet_handler", PacketHandler.NAME, packetHandler);

        env.log(Level.DEBUG, "Packet handler inserted");
    }

    @Override
    public boolean isIngame() {
        return getCurrentServerIp() != null && mc.getConnection() != null;
    }

    @Override
    public void loadChunk(Chunk chunk) {
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling loadChunk from non-mc-thread");
        }

        // load into game by pretending this comes from the game server
        NetHandlerPlayClient conn = mc.getConnection();
        if (conn == null) {
            env.log(Level.ERROR, "mc.connection == null, ignoring extra chunk at %s", chunk.pos);
            return;
        }

        // prevent memory leaks
        unloadChunk(chunk.pos);

        conn.handleChunkData(chunk.packet);
        chunkLoadTimes.put(chunk.pos, env.currentTimeMillis());
    }

    @Override
    public void runOnMcThread(Runnable runnable) {
        mc.addScheduledTask(runnable);
    }

    @Override
    public void showAchievement(String title, String msg) {
        mc.getToastGui().add(new SystemToast(
                SystemToast.Type.TUTORIAL_HINT,
                new TextComponentString(title),
                new TextComponentString(msg)));
    }

    @Override
    public void showChat(String msg) {
        final Style modStyle = new Style().setColor(TextFormatting.RED);
        final Style msgStyle = new Style().setColor(TextFormatting.GOLD);
        final ITextComponent modComponent = new TextComponentString("[" + MoreChunksMod.MOD_NAME + "] ").setStyle(modStyle);
        final ITextComponent msgComponent = new TextComponentString(msg).setStyle(msgStyle);
        mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString("")
                .appendSibling(modComponent)
                .appendSibling(msgComponent));
    }

    @Override
    public void showHotbarMsg(String msg) {
        mc.ingameGUI.setOverlayMessage(msg, false);
    }

    @Override
    public void unloadChunk(Pos2 pos) {
        if (!mc.isCallingFromMinecraftThread()) {
            // try wrapping this call in runOnMcThread() in your code
            throw new Error("Calling unloadChunk from non-mc-thread");
        }

        NetHandlerPlayClient conn = mc.getConnection();
        if (conn == null) {
            env.log(Level.ERROR, "mc.connection == null, not unloading chunk at %s", pos);
            return;
        }
        conn.processChunkUnload(new SPacketUnloadChunk(pos.x, pos.z));
        chunkLoadTimes.remove(pos);
    }

    @Override
    public boolean wasPacketHandlerAlreadyInserted() {
        NetHandlerPlayClient mcConnection = mc.getConnection();
        if (mcConnection == null) return false;

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();
        return pipe.get(PacketHandler.NAME) != null;
    }
}
