package com.shugatel.allaggressive;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@Mod(AllAggressive.MODID)
public class AllAggressive {
    public static final String MODID = "allaggressive";

    public AllAggressive(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && !(event.getEntity() instanceof Player)) {
            if (!event.getLevel().isClientSide()) {
                try {
                    mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));

                    if (mob instanceof PathfinderMob pathfinderMob) {
                        pathfinderMob.goalSelector.addGoal(1, new UniversalMeleeAttackGoal(pathfinderMob, 1.0D, false));
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public static class UniversalMeleeAttackGoal extends Goal {
        private final PathfinderMob mob;
        private final double speedModifier;
        private int ticksUntilNextAttack;

        public UniversalMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            this.mob = mob;
            this.speedModifier = speedModifier;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.ticksUntilNextAttack = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            double distance = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

            this.mob.getNavigation().moveTo(target, this.speedModifier);
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);

            if (distance <= this.getAttackReachSqr(target) && this.ticksUntilNextAttack <= 0) {
                this.ticksUntilNextAttack = 20;
                target.hurt(this.mob.damageSources().mobAttack(this.mob), 3.0F);
                this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }

        protected double getAttackReachSqr(LivingEntity target) {
            return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + target.getBbWidth();
        }
    }
}
