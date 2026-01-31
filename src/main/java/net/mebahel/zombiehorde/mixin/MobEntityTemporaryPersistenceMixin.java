package net.mebahel.zombiehorde.mixin;

import net.mebahel.zombiehorde.util.TemporaryPersistentMob;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityTemporaryPersistenceMixin implements TemporaryPersistentMob {

    @Unique
    private long mebahel$persistentUntil = -1L;

    @Override
    public void mebahel$setPersistentUntil(long tick) {
        this.mebahel$persistentUntil = tick;
    }

    @Override
    public long mebahel$getPersistentUntil() {
        return this.mebahel$persistentUntil;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void mebahel$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.mebahel$persistentUntil > 0) {
            nbt.putLong("MebahelPersistentUntil", this.mebahel$persistentUntil);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void mebahel$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("MebahelPersistentUntil")) {
            this.mebahel$persistentUntil = nbt.getLong("MebahelPersistentUntil");
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mebahel$tick(CallbackInfo ci) {
        if (this.mebahel$persistentUntil <= 0) return;

        MobEntity self = (MobEntity) (Object) this;
        long time = self.getWorld().getTime();

        if (time >= this.mebahel$persistentUntil) {
            // ✅ enlever le "persistent" (via accessor)
            ((MobEntityAccessor) self).mebahel$setPersistentFlag(false);

            // ✅ clear timer
            this.mebahel$persistentUntil = -1L;
        }
    }
}