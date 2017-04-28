package gjum.minecraft.forge.chunkfix;

import gjum.minecraft.forge.chunkfix.config.ChunkFixConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.io.File;

@Mod(
        modid = ChunkFixMod.MOD_ID,
        name = ChunkFixMod.MOD_NAME,
        version = ChunkFixMod.VERSION,
        guiFactory = "gjum.minecraft.forge.chunkfix.config.ConfigGuiFactory",
        clientSideOnly = true)
public class ChunkFixMod {
    public static final String MOD_ID = "chunkfix";
    public static final String MOD_NAME = "ChunkFix";
    public static final String VERSION = "@VERSION@";
    public static final String BUILD_TIME = "@BUILD_TIME@";

    @Mod.Instance(MOD_ID)
    public static ChunkFixMod instance;

    private ChunkFix chunkFix = new ChunkFix();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ChunkFix.setLogger(event.getModLog());
        File configFile = event.getSuggestedConfigurationFile();
        ChunkFix.log("Loading config from " + configFile);
        ChunkFixConfig.instance.load(configFile);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ChunkFix.log("%s version %s built at %s", MOD_NAME, VERSION, BUILD_TIME);

        MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(ChunkFixMod.MOD_ID)) {
            ChunkFixConfig.instance.afterGuiSave();
        }
    }

    @SubscribeEvent
    public void onJoinServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        chunkFix.onJoinServer();
    }

    @SubscribeEvent
    public void onChunkReceived(ChunkEvent.Load event) {
        chunkFix.afterConnectionIsMade();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        chunkFix.onTick();
    }
}
