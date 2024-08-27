package ewewukek.musketmod;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ClientUtilities {
    public static void registerItemProperties() {
        ClampedItemPropertyFunction predicate = (stack, level, player, seed) -> {
            return GunItem.isLoaded(stack) ? 1 : 0;
        };
        ResourceLocation location = MusketMod.resource("loaded");
        ItemProperties.register(Items.MUSKET, location, predicate);
        ItemProperties.register(Items.MUSKET_WITH_BAYONET, location, predicate);
        ItemProperties.register(Items.MUSKET_WITH_SCOPE, location, predicate);
        ItemProperties.register(Items.BLUNDERBUSS, location, predicate);
        ItemProperties.register(Items.PISTOL, location, predicate);
    }

    public static Optional<HumanoidModel.ArmPose> getArmPose(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.swinging && !stack.isEmpty() && stack.getItem() instanceof GunItem gun) {
            if (gun.canUseFrom(player, hand) && GunItem.isLoaded(stack)) {
                return Optional.of(HumanoidModel.ArmPose.CROSSBOW_HOLD);
            }
        }
        return Optional.empty();
    }

    public static boolean disableMainHandEquipAnimation;
    public static boolean disableOffhandEquipAnimation;

    public static void renderGunInHand(ItemInHandRenderer renderer, AbstractClientPlayer player, InteractionHand hand, float dt, float pitch, float swingProgress, float equipProgress, ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (player.isScoping()) {
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isRightHand = arm == HumanoidArm.RIGHT;
        float sign = isRightHand ? 1 : -1;

        GunItem gun = (GunItem)stack.getItem();
        if (!gun.canUseFrom(player, hand)) {
            poseStack.pushPose();
            poseStack.translate(sign * 0.5, -0.5 - 0.6 * equipProgress, -0.7);
            poseStack.mulPose(Axis.XP.rotationDegrees(70));
            renderer.renderItem(player, stack, isRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightHand, poseStack, bufferSource, light);
            poseStack.popPose();
            return;
        }

        ItemStack activeStack = GunItem.getActiveStack(hand);
        if (stack == activeStack) {
            setEquipAnimationDisabled(hand, true);

        } else if (activeStack != null && activeStack.getItem() != gun) {
            setEquipAnimationDisabled(hand, false);
        }

        poseStack.pushPose();
        poseStack.translate(sign * 0.15, -0.25, -0.35);

        if (swingProgress > 0) {
            float swingSharp = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI);
            float swingNormal = Mth.sin(swingProgress * (float)Math.PI);

            if (gun == Items.MUSKET_WITH_BAYONET) {
                poseStack.translate(sign * -0.05 * swingNormal, 0, 0.05 - 0.3 * swingSharp);
                poseStack.mulPose(Axis.YP.rotationDegrees(5 * swingSharp));
            } else {
                poseStack.translate(sign * 0.05 * (1 - swingNormal), 0.05 * (1 - swingNormal), 0.05 - 0.4 * swingSharp);
                poseStack.mulPose(Axis.XP.rotationDegrees(180 + sign * 20 * (1 - swingSharp)));
            }

        } else if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            Pair<Integer, Integer> loadingDuration = GunItem.getLoadingDuration(stack);
            int loadingStages = loadingDuration.getLeft();
            int ticksPerLoadingStage = loadingDuration.getRight();

            float usingTicks = player.getTicksUsingItem() + dt - 1;
            int loadingStage = GunItem.getLoadingStage(stack) + (int)(usingTicks / ticksPerLoadingStage);
            int reloadDuration = GunItem.reloadDuration(stack);

            if (reloadDuration > 0 && usingTicks < reloadDuration + 5) {
                poseStack.translate(0, -0.3, 0.05);
                poseStack.mulPose(Axis.XP.rotationDegrees(60));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * 10));

                float t = 0;
                // return
                if (usingTicks >= ticksPerLoadingStage && loadingStage <= loadingStages) {
                    usingTicks = usingTicks % ticksPerLoadingStage;
                    if (usingTicks < 4) {
                        t = (4 - usingTicks) / 4;
                    }
                }
                // hit down by ramrod
                if (usingTicks >= ticksPerLoadingStage - 2 && loadingStage < loadingStages) {
                    t = (usingTicks - ticksPerLoadingStage + 2) / 2;
                    t = Mth.sin((float)Math.PI / 2 * Mth.sqrt(t));
                }
                poseStack.translate(0, 0, 0.025 * t);

                if (gun == Items.BLUNDERBUSS) {
                    poseStack.translate(0, 0, -0.06);
                } else if (gun == Items.PISTOL) {
                    poseStack.translate(0, 0, -0.12);
                }
            }
        } else {
            if (isEquipAnimationDisabled(hand)) {
                if (equipProgress == 0) {
                    setEquipAnimationDisabled(hand, false);
                    GunItem.setActiveStack(hand, null);
                }
            } else {
                poseStack.translate(0, -0.6 * equipProgress, 0);
            }
        }

        renderer.renderItem(player, stack, isRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightHand, poseStack, bufferSource, light);
        poseStack.popPose();
    }

    public static boolean isEquipAnimationDisabled(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return disableMainHandEquipAnimation;
        } else {
            return disableOffhandEquipAnimation;
        }
    }

    public static void setEquipAnimationDisabled(InteractionHand hand, boolean disabled) {
        if (hand == InteractionHand.MAIN_HAND) {
            disableMainHandEquipAnimation = disabled;
        } else {
            disableOffhandEquipAnimation = disabled;
        }
    }
}
