package mix.client.mixui.integration.haunt.fx.trail;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.util.MathHelper;

public final class TrailPose {
   public double x;
   public double y;
   public double z;
   public float bodyYaw;
   public boolean sneaking;
   public boolean riding;
   public boolean other;
   public float limbSwing;
   public float limbAmount;
   public float swing;
   public float pitch;
   public float age;
   public int held;

   public TrailPose from(EntityLivingBase e, float partialTicks, boolean other) {
      this.x = e.lastTickPosX + (e.posX - e.lastTickPosX) * (double)partialTicks;
      this.y = e.lastTickPosY + (e.posY - e.lastTickPosY) * (double)partialTicks;
      this.z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * (double)partialTicks;
      this.bodyYaw = lerpAngle(e.prevRenderYawOffset, e.renderYawOffset, partialTicks);
      this.sneaking = e.isSneaking();
      this.riding = e.isRiding();
      this.other = other;
      this.limbAmount = e.prevLimbSwingAmount + (e.limbSwingAmount - e.prevLimbSwingAmount) * partialTicks;
      this.limbSwing = e.limbSwing - e.limbSwingAmount * (1.0F - partialTicks);
      if(this.limbAmount > 1.0F) {
         this.limbAmount = 1.0F;
      }

      this.swing = e.getSwingProgress(partialTicks);
      this.pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * partialTicks;
      this.age = (float)e.ticksExisted + partialTicks;
      this.held = 0;
      if(e.getHeldItem() != null) {
         this.held = 1;
         if(e instanceof EntityPlayer && ((EntityPlayer)e).getItemInUseCount() > 0 && e.getHeldItem().getItemUseAction() == EnumAction.BLOCK) {
            this.held = 3;
         }
      }

      return this;
   }

   private static float lerpAngle(float from, float to, float t) {
      float d = MathHelper.wrapAngleTo180_float(to - from);
      return from + d * t;
   }

   public float armAngle(boolean right) {
      float a = MathHelper.cos(this.limbSwing * 0.6662F + (right?3.1415927F:0.0F)) * this.limbAmount;
      if(this.riding) {
         a -= 0.62831855F;
      }

      if(right && this.held != 0) {
         a = a * 0.5F - 0.31415927F * (float)this.held;
      }

      if(right && this.swing > 0.0F) {
         float f = 1.0F - this.swing;
         f = f * f;
         f = f * f;
         f = 1.0F - f;
         float f2 = MathHelper.sin(f * 3.1415927F);
         float f3 = MathHelper.sin(this.swing * 3.1415927F) * -((float)Math.toRadians((double)this.pitch) - 0.7F) * 0.75F;
         a -= f2 * 1.2F + f3;
      }

      if(this.sneaking) {
         a += 0.4F;
      }

      float idle = MathHelper.sin(this.age * 0.067F) * 0.05F;
      return right?a + idle:a - idle;
   }

   public float legAngle(boolean right) {
      return this.riding?-1.2566371F:MathHelper.cos(this.limbSwing * 0.6662F + (right?0.0F:3.1415927F)) * 1.4F * this.limbAmount;
   }
}
