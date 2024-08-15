package ewewukek.musketmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ewewukek.musketmod.ILootTableId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

@Mixin(targets = "net.minecraft.server.ReloadableServerRegistries$Holder")
public class ReloadableServerRegistriesHolderMixin {
    private ResourceKey<LootTable> key;

    @ModifyVariable(method = "getLootTable", at = @At("HEAD"))
    private ResourceKey<LootTable> storeTableId(ResourceKey<LootTable> key) {
        this.key = key;
        return key;
    }

    @Inject(method = "getLootTable", at = @At("RETURN"))
    private void passTableId(CallbackInfoReturnable<LootTable> ci) {
        ((ILootTableId)ci.getReturnValue()).setKey(key);
    }
}
