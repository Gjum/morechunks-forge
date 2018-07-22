package gjum.minecraft.forge.morechunks;

import gjum.minecraft.forge.morechunks.gui.GuiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
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

    public final Config config = new Config();
    public final IEnv env = new Env();
    public final McGame game = new McGame(env);
    public final MoreChunks moreChunks = new MoreChunks(game, config, env, new ChunkServer(env), VERSION);

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
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            env.log(Level.WARN, "Failed to save config to %s", configFile);
        }

        config.addSubscriber(moreChunks);
        config.propagateChange();

        ClientRegistry.registerKeyBinding(openGuiKey);
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
    public void onGameConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        moreChunks.onGameConnected();
    }

    @SubscribeEvent
    public void onGameDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        moreChunks.onGameDisconnected();
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        moreChunks.onPlayerChangedDimension(event.toDim);
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (openGuiKey.isPressed()) {
            mc.displayGuiScreen(new GuiConfig(mc.currentScreen));
        }
    }
}
