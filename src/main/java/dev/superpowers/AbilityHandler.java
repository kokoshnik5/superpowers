package dev.superpowers;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side logic. The client only sends the ability id; everything is validated here. */
public final class AbilityHandler {
    private static final Map<UUID, EnumMap<Ability, Long>> COOLDOWNS = new HashMap<>();

    private AbilityHandler() {}

    public static void clear(UUID id) {
        COOLDOWNS.remove(id);
    }

    public static void use(ServerPlayer player, int id) {
        Ability ability = Ability.byId(id);
        if (ability == null || player.isSpectator() || !player.isAlive()) return;

        ServerLevel level = (ServerLevel) player.level;
        long now = level.getGameTime();
        EnumMap<Ability, Long> map = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new EnumMap<>(Ability.class));
        long readyAt = map.getOrDefault(ability, 0L);

        if (now < readyAt) {
            String left = String.format("%.1f", (readyAt - now) / 20.0);
            player.displayClientMessage(new TranslatableComponent("message.superpowers.cooldown", left), true);
            return;
        }

        boolean success = switch (ability) {
            case DASH -> dash(player, level);
            case PUNCH -> punch(player, level);
            case SLAM -> slam(player, level);
            case FLIGHT -> toggleFlight(player);
        };

        if (success) map.put(ability, now + ability.cooldownTicks);
    }

    private static void push(Entity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
        if (entity instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }

    private static boolean dash(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle().normalize();
        push(player, new Vec3(look.x * 2.2, look.y * 2.2 + 0.15, look.z * 2.2));
        player.fallDistance = 0;
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        return true;
    }

    private static boolean punch(ServerPlayer player, ServerLevel level) {
        double range = 5.0;
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 dir = player.getLookAngle();
        Vec3 end = start.add(dir.scale(range));
        AABB box = player.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
                e -> !e.isSpectator() && e.isAttackable() && e != player, range * range);
        if (hit == null) return false;

        Entity target = hit.getEntity();
        target.hurt(DamageSource.playerAttack(player), 14.0F);
        if (target instanceof LivingEntity living) {
            living.knockback(1.8F, -dir.x, -dir.z);
        }
        push(target, target.getDeltaMovement().add(0, 0.3, 0));

        Vec3 p = hit.getLocation();
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5F, 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        return true;
    }

    private static boolean slam(ServerPlayer player, ServerLevel level) {
        double radius = 6.0;
        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(radius),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()
                        && e.distanceToSqr(player) <= radius * radius);

        for (Entity e : targets) {
            e.hurt(DamageSource.playerAttack(player), 10.0F);
            Vec3 diff = e.position().subtract(player.position());
            ((LivingEntity) e).knockback(1.2F, -diff.x, -diff.z);
            push(e, e.getDeltaMovement().add(0, 0.4, 0));
        }

        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 60, radius / 3, 0.2, radius / 3, 0.1);
        return true;
    }

    private static boolean toggleFlight(ServerPlayer player) {
        // Creative already has flight; don't touch it.
        if (player.isCreative()) return false;

        Abilities abilities = player.getAbilities();
        boolean enable = !abilities.mayfly;
        abilities.mayfly = enable;
        abilities.setFlyingSpeed(enable ? 0.12F : 0.05F);
        if (!enable) abilities.flying = false;
        player.onUpdateAbilities();

        player.displayClientMessage(new TranslatableComponent(
                enable ? "message.superpowers.flight_on" : "message.superpowers.flight_off"), true);
        return true;
    }
}
