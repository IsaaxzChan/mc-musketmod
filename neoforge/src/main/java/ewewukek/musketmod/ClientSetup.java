package ewewukek.musketmod;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MusketMod.MODID, dist = Dist.CLIENT)
public class ClientSetup {
    public ClientSetup(IEventBus bus) {
        bus.addListener(this::setup);
        bus.addListener(this::registerRenderers);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, this::renderHand);
        NeoForge.EVENT_BUS.addListener(this::renderPlayer);
    }

    public void setup(final FMLClientSetupEvent event) {
        ClampedItemPropertyFunction loaded = (stack, world, player, arg) -> {
            return GunItem.isLoaded(stack) ? 1 : 0;
        };
        ItemProperties.register(Items.MUSKET, new ResourceLocation("loaded"), loaded);
        ItemProperties.register(Items.MUSKET_WITH_BAYONET, new ResourceLocation("loaded"), loaded);
        ItemProperties.register(Items.PISTOL, new ResourceLocation("loaded"), loaded);
    }

    public void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MusketMod.BULLET_ENTITY_TYPE, BulletRenderer::new);
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
        if (!(event.getEntity() instanceof Player)
         || !(event.getRenderer().getModel() instanceof PlayerModel)) return;
        Player player = (Player)event.getEntity();
        Optional<HumanoidModel.ArmPose> mainHandPose = ClientUtilities.getArmPose(player, InteractionHand.MAIN_HAND);
        Optional<HumanoidModel.ArmPose> offhandPose = ClientUtilities.getArmPose(player, InteractionHand.OFF_HAND);
        PlayerModel<Player> model = event.getRenderer().getModel();
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = mainHandPose.isPresent() ? mainHandPose.get() : model.rightArmPose;
            model.leftArmPose = offhandPose.isPresent() ? offhandPose.get() : model.leftArmPose;
        } else {
            model.rightArmPose = offhandPose.isPresent() ? offhandPose.get() : model.rightArmPose;
            model.leftArmPose = mainHandPose.isPresent() ? mainHandPose.get() : model.leftArmPose;
        }
    }

    public static void handleSmokeEffectPacket(SmokeEffectPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        Vec3 origin = new Vec3(packet.origin());
        Vec3 direction = new Vec3(packet.direction());
        GunItem.fireParticles(level, origin, direction);
    }
}