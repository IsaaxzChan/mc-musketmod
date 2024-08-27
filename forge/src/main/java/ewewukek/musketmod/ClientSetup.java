package ewewukek.musketmod;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkEvent;

public class ClientSetup {
    public ClientSetup(IEventBus bus) {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (client, parent) -> ClothConfigScreen.build(parent)));

        bus.addListener(this::setup);
        bus.addListener(this::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, this::renderHand);
        MinecraftForge.EVENT_BUS.addListener(this::renderPlayer);
    }

    public void setup(final FMLClientSetupEvent event) {
        ClientUtilities.registerItemProperties();
    }

    public void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BulletEntity.ENTITY_TYPE, BulletRenderer::new);
    }

    public void renderHand(final RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem) {
            Minecraft mc = Minecraft.getInstance();
            ClientUtilities.renderGunInHand(
                    mc.getEntityRenderDispatcher().getItemInHandRenderer(), mc.player,
                    event.getHand(), event.getPartialTick(), event.getInterpolatedPitch(),
                    event.getSwingProgress(), event.getEquipProgress(), stack,
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            event.setCanceled(true);
        }
    }

    public void renderPlayer(final RenderLivingEvent.Pre<Player, PlayerModel<Player>> event) {
        if (!(event.getEntity() instanceof Player player)
            || !(event.getRenderer().getModel() instanceof PlayerModel)) return;

        PlayerModel<Player> model = event.getRenderer().getModel();
        Optional<HumanoidModel.ArmPose> mainHandPose = ClientUtilities.getArmPose(player, InteractionHand.MAIN_HAND);
        Optional<HumanoidModel.ArmPose> offhandPose = ClientUtilities.getArmPose(player, InteractionHand.OFF_HAND);
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = mainHandPose.isPresent() ? mainHandPose.get() : model.rightArmPose;
            model.leftArmPose = offhandPose.isPresent() ? offhandPose.get() : model.leftArmPose;
        } else {
            model.rightArmPose = offhandPose.isPresent() ? offhandPose.get() : model.rightArmPose;
            model.leftArmPose = mainHandPose.isPresent() ? mainHandPose.get() : model.leftArmPose;
        }
    }

    public static void handleSmokeEffectPacket(MusketMod.SmokeEffectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        PacketListener listener = ctx.get().getNetworkManager().getPacketListener();
        if (listener instanceof ClientPacketListener) {
            GunItem.fireParticles(((ClientPacketListener)listener).getLevel(), packet.origin, packet.direction);
        }
    }
}
