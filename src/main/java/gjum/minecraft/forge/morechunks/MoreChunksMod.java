package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.gui.GuiConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
@Mod(
        modid = MoreChunksMod.MOD_ID,
        name = MoreChunksMod.MOD_NAME,
        version = MoreChunksMod.VERSION,
        guiFactory = MoreChunksMod.guiFactory,
        clientSideOnly = true)
public class MoreChunksMod {
    public static final String MOD_ID = "morechunks";
    public static final String MOD_NAME = "MoreChunks";
    public static final String VERSION = "@VERSION@";
    public static final String BUILD_TIME = "@BUILD_TIME@";
    public static final String guiFactory = "gjum.minecraft.forge.morechunks.gui.ConfigGuiFactory";

    @Mod.Instance(MOD_ID)
    public static MoreChunksMod instance;

    public final KeyBinding openGuiKey = new KeyBinding(MOD_ID + ".key.openGui", Keyboard.KEY_NONE, MOD_NAME);
    public final KeyBinding toggleEnabledKey = new KeyBinding(MOD_ID + ".key.toggleEnabled", Keyboard.KEY_NONE, MOD_NAME);

    public final IEnv env;
    public final IConfig config;
    public final MoreChunks moreChunks;

    private ChannelHandler moreChunksPacketHandler;

    public MoreChunksMod() {
        env = new Env();
        config = new Config();
        moreChunks = new MoreChunks(new McGame(env), config, env);
        moreChunks.setChunkServer(new ChunkServer(config, env, moreChunks));
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        env.log(Level.INFO, "%s version %s built at %s", MOD_NAME, VERSION, BUILD_TIME);

        File configFile = event.getSuggestedConfigurationFile();
        configFile = new File(configFile.getAbsolutePath().replaceAll("\\.[^.]+$", ".json"));
        try {
            config.load(configFile);
            env.log(Level.INFO, "Loaded config from %s", configFile);
        } catch (IOException e) {
            e.printStackTrace();
            env.log(Level.WARN, "Failed to load config from %s", configFile);
        }
        try {
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
            env.log(Level.WARN, "Failed to save config to %s", configFile);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        moreChunks.onTick();
    }

    @SubscribeEvent
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (moreChunksPacketHandler != null) return;

        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient mcConnection = mc.getConnection();
        if (mcConnection == null) {
            env.log(Level.ERROR, "Could not inject packet handler into pipeline: mc.connection == null");
            return;
        }

        env.log(Level.DEBUG, "Connected to game, injecting packet handler into pipeline");

        moreChunksPacketHandler = new PacketHandler(moreChunks);

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();
        pipe.addBefore("fml:packet_handler", PacketHandler.NAME, moreChunksPacketHandler);

        moreChunks.onGameConnected();
    }

    @SubscribeEvent
    public void onGameDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        moreChunks.onGameDisconnected();
        moreChunksPacketHandler = null;
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        moreChunks.onPlayerChangedDimension(event.toDim);
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (openGuiKey.isPressed()) {
            mc.displayGuiScreen(new GuiConfig(mc.currentScreen));
        }
        if (toggleEnabledKey.isPressed()) {
            config.setEnabled(!config.getEnabled());
        }
    }
}
