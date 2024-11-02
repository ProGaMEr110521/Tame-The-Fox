package org.progamer.tame_the_fox.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import org.progamer.tame_the_fox.CustomFox;

import java.util.EnumSet;

public class FoxSitWhenOrderedToGoal extends Goal{
    private final TamableAnimal mob;

    public FoxSitWhenOrderedToGoal(TamableAnimal tamableAnimal) {
        this.mob = tamableAnimal;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    public boolean canContinueToUse() {
        return this.mob.isOrderedToSit();
    }

    public boolean canUse() {
        if (!this.mob.isTame()) {
            return false;
        } else if (this.mob.isInWaterOrBubble()) {
            return false;
        } else if (!this.mob.onGround()) {
            return false;
        } else {
            LivingEntity livingentity = this.mob.getOwner();
            if (livingentity == null) {
                return true;
            } else {
                return (!(this.mob.distanceToSqr(livingentity) < 144.0D) || livingentity.getLastHurtByMob() == null) && this.mob.isOrderedToSit();
            }
        }
    }

    public void start() {
        this.mob.getNavigation().stop();
        this.mob.setInSittingPose(true);
        ((CustomFox)this.mob).setSitting(true);
    }

    public void stop() {
        this.mob.setInSittingPose(false);
        ((CustomFox)this.mob).setSitting(false);
    }
}
