package ewewukek.musketmod;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(MusketMod.MODID)
public class MusketMod {
    public static final String MODID = "musketmod";
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("musketmod.txt");

    public MusketMod(IEventBus bus, ModContainer modContainer) {
        Config.reload();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
            () -> (container, parent) -> ClothConfigScreen.build(parent));

        bus.addListener(this::register);
        bus.addListener(this::creativeTabs);
        bus.addListener(this::registerPacket);
        NeoForge.EVENT_BUS.addListener(this::worldTick);
        NeoForge.EVENT_BUS.addListener(this::reload);
    }

    public void register(final RegisterEvent event) {
        Items.registerDataComponentTypes((path, component) -> {
            event.register(Registries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, path), () -> component);
        });
        Items.register((path, item) -> {
            event.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, path), () -> item);
        });
        Sounds.register((sound) -> {
            event.register(Registries.SOUND_EVENT, sound.getLocation(), () -> sound);
        });
        event.register(Registries.ENTITY_TYPE, helper -> {
            BulletEntity.register((string, entityType) -> {
                helper.register(ResourceLocation.fromNamespaceAndPath(MODID, string), entityType);
            });
        });
    }

    public void creativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            Items.addToCombatTab((item) -> {
                event.accept(item);
            });
        }
    }

    public void worldTick(final LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide) {
            DeferredDamage.apply();
        }
    }

    public void registerPacket(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MusketMod.MODID).versioned("2").optional();
        registrar.playToClient(
            SmokeEffectPacket.TYPE,
            SmokeEffectPacket.CODEC,
            (packet, context) -> {
                context.enqueueWork(() -> {
                    ClientUtilities.handleSmokeEffectPacket(packet);
                });
            }
        );
    }

    public void reload(final AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
                Executor gameExecutor) {

                return stage.wait(Unit.INSTANCE).thenRunAsync(() -> {
                    Config.reload();
                }, gameExecutor);
            }
        });
    }

    public static void disableVelocityUpdate(EntityType.Builder<?> builder) {
        builder.setShouldReceiveVelocityUpdates(false);
    }

    public static void sendSmokeEffect(ServerLevel level, Vec3 origin, Vec3 direction) {
        SmokeEffectPacket packet = SmokeEffectPacket.fromVec3(origin, direction);
        BlockPos blockPos = BlockPos.containing(origin.x, origin.y, origin.z);
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(blockPos), packet);
    }
}
