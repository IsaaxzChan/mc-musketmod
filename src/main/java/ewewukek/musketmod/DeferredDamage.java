package ewewukek.musketmod;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class DeferredDamage {
    public static void hurt(Entity target, DamageSource source, float damage) {
        Entry entry = entries.get(target);
        if (entry == null) {
            entry = new Entry();
            entries.put(target, entry);
        }
        entry.source = source;
        entry.damage += damage;
    }

    public static void apply() {
        entries.forEach((target, entry) -> {
            target.hurt(entry.source, entry.damage);
        });
        entries.clear();
    }

    private static Map<Entity, Entry> entries = new HashMap<>();

    private static class Entry {
        DamageSource source;
        float damage;
    }
}
