package ewewukek.musketmod;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ObjectHolder;

public class MusketItem extends Item {
    public static final int DURABILITY = 250;
    public static final int LOADING_STAGE_1 = 5;
    public static final int LOADING_STAGE_2 = 10;
    public static final int LOADING_STAGE_3 = 20;
    public static final int RELOAD_DURATION = 30;

    public static float bulletStdDev;
    public static double bulletSpeed;

    @ObjectHolder(MusketMod.MODID + ":cartridge")
    public static Item CARTRIDGE;

    @ObjectHolder(MusketMod.MODID + ":musket_load0")
    public static SoundEvent SOUND_MUSKET_LOAD_0;
    @ObjectHolder(MusketMod.MODID + ":musket_load1")
    public static SoundEvent SOUND_MUSKET_LOAD_1;
    @ObjectHolder(MusketMod.MODID + ":musket_load2")
    public static SoundEvent SOUND_MUSKET_LOAD_2;

    @ObjectHolder(MusketMod.MODID + ":musket_ready")
    public static SoundEvent SOUND_MUSKET_READY;

    @ObjectHolder(MusketMod.MODID + ":musket_fire")
    public static SoundEvent SOUND_MUSKET_FIRE;

    public MusketItem(Item.Properties properties) {
        super(properties.defaultMaxDamage(DURABILITY));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return super.onItemRightClick(worldIn, player, hand);

        ItemStack stack = player.getHeldItem(hand);
        boolean creative = player.abilities.isCreativeMode;

        if (player.areEyesInFluid(FluidTags.WATER) && !creative) {
            return ActionResult.resultFail(stack);
        }

        boolean haveAmmo = !findAmmo(player).isEmpty() || creative;
        boolean loaded = isLoaded(stack);

        if (loaded && isReady(stack)) {
            if (!worldIn.isRemote) {
                fireBullet(worldIn, player);
            }
            player.playSound(SOUND_MUSKET_FIRE, 1.5f, 1);

            damageItem(stack, player);
            setReady(stack, false);
            setLoaded(stack, false);

            return ActionResult.resultConsume(stack);

        } else if (loaded || haveAmmo) {

            if (!loaded) {
                setLoadingStage(stack, 0);
            }
            player.setActiveHand(hand);
            return ActionResult.resultConsume(stack);

        } else {
            return ActionResult.resultFail(stack);
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
        if (isLoaded(stack)) setReady(stack, true);
    }

    @Override
    public void onUse(World world, LivingEntity entity, ItemStack stack, int timeLeft) {
        if (world.isRemote || !(entity instanceof PlayerEntity)) return;

        int usingDuration = getUseDuration(stack) - timeLeft;
        int loadingStage = getLoadingStage(stack);

        double posX = entity.getPosX();
        double posY = entity.getPosY();
        double posZ = entity.getPosZ();

        if (loadingStage == 0 && usingDuration >= LOADING_STAGE_1) {
            world.playSound(null, posX, posY, posZ, SOUND_MUSKET_LOAD_0, SoundCategory.PLAYERS, 0.5F, 1.0F);
            loadingStage = 1;

        } else if (loadingStage == 1 && usingDuration >= LOADING_STAGE_2) {
            world.playSound(null, posX, posY, posZ, SOUND_MUSKET_LOAD_1, SoundCategory.PLAYERS, 0.5F, 1.0F);
            loadingStage = 2;

        } else if (loadingStage == 2 && usingDuration >= LOADING_STAGE_3) {
            world.playSound(null, posX, posY, posZ, SOUND_MUSKET_LOAD_2, SoundCategory.PLAYERS, 0.5F, 1.0F);
            loadingStage = 3;
        }
        setLoadingStage(stack, loadingStage);
    }


    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof PlayerEntity)) return;

        if (getUseDuration(stack) - timeLeft >= RELOAD_DURATION && !isLoaded(stack)) {
            PlayerEntity player = (PlayerEntity) entityLiving;

            if (!player.abilities.isCreativeMode) {
                ItemStack ammoStack = findAmmo(player);
                if (ammoStack.isEmpty()) return;

                ammoStack.shrink(1);
                if (ammoStack.isEmpty()) player.inventory.deleteStack(ammoStack);
            }

            player.playSound(SOUND_MUSKET_READY, 0.5f, 1);
            setLoaded(stack, true);
        }
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        if (!worldIn.isRemote && entityLiving instanceof PlayerEntity && state.getBlockHardness(worldIn, pos) != 0.0f) {
            damageItem(stack, (PlayerEntity) entityLiving);
        }
        return false;
    }

    public static void damageItem(ItemStack stack, PlayerEntity player) {
        stack.damageItem(1, player, (entity) -> {
            entity.sendBreakAnimation(player.getActiveHand());
        });
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    public static boolean isLoaded(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.getByte("loaded") == 1;
    }

    public static boolean isReady(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.getByte("ready") == 1;
    }

    private boolean isAmmo(ItemStack stack) {
        return stack.getItem() == CARTRIDGE;
    }

    private ItemStack findAmmo(PlayerEntity player) {
        if (isAmmo(player.getHeldItem(Hand.OFF_HAND))) {
            return player.getHeldItem(Hand.OFF_HAND);

        } else if (isAmmo(player.getHeldItem(Hand.MAIN_HAND))) {
            return player.getHeldItem(Hand.MAIN_HAND);

        } else {
            for (int i = 0; i != player.inventory.getSizeInventory(); ++i) {
                ItemStack itemstack = player.inventory.getStackInSlot(i);
                if (isAmmo(itemstack)) return itemstack;
            }

            return ItemStack.EMPTY;
        }
    }

    private Vector3d getPlayerFiringPoint(PlayerEntity player) {
        Vector3d side = Vector3d.fromPitchYaw(0, player.rotationYaw + 90);
        if (player.getActiveHand() == Hand.OFF_HAND) side = side.scale(-1);
        Vector3d down = Vector3d.fromPitchYaw(player.rotationPitch + 90, player.rotationYaw);

        double posX = player.getPosX();
        double posY = player.getPosY();
        double posZ = player.getPosZ();

        return new Vector3d(posX, posY + player.getEyeHeight(), posZ)
                .add(side.add(down).scale(0.1));
    }

    private void fireBullet(World worldIn, PlayerEntity player) {
        Vector3d pos = getPlayerFiringPoint(player);
        Vector3d front = Vector3d.fromPitchYaw(player.rotationPitch, player.rotationYaw);

        float angle = (float) Math.PI * 2 * random.nextFloat();
        float gaussian = Math.abs((float) random.nextGaussian());
        if (gaussian > 4) gaussian = 4;

        front = front.rotatePitch(bulletStdDev * gaussian * MathHelper.sin(angle))
                .rotateYaw(bulletStdDev * gaussian * MathHelper.cos(angle));

        Vector3d motion = front.scale(bulletSpeed);

        Vector3d playerMotion = player.getMotion();
        motion.add(playerMotion.x, player.isOnGround() ? 0 : playerMotion.y, playerMotion.z);

        BulletEntity bullet = new BulletEntity(worldIn);
        bullet.setShooter(player);
        bullet.setPosition(pos.x, pos.y, pos.z);
        bullet.setMotion(motion);
        bullet.doFireParticles = true;

        worldIn.addEntity(bullet);
    }

    private void setLoaded(ItemStack stack, boolean loaded) {
        stack.getOrCreateTag().putByte("loaded", (byte) (loaded ? 1 : 0));
    }

    private void setReady(ItemStack stack, boolean ready) {
        stack.getOrCreateTag().putByte("ready", (byte) (ready ? 1 : 0));
    }

    private void setLoadingStage(ItemStack stack, int loadingStage) {
        stack.getOrCreateTag().putInt("loadingStage", loadingStage);
    }

    private int getLoadingStage(ItemStack stack) {
        return stack.getOrCreateTag().getInt("loadingStage");
    }
}
