package mix.client.mixui.integration.haunt.fx;

import mix.client.mixui.integration.haunt.fx.FxFrame;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class Anchor {
   private Entity entity;
   private final boolean follow;
   public final double offX;
   public final double offY;
   public final double offZ;
   private double feetX;
   private double feetY;
   private double feetZ;
   private double prevX;
   private double prevY;
   private double prevZ;
   private boolean first = true;
   public double x;
   public double y;
   public double z;
   public double dx;
   public double dy;
   public double dz;
   public long frame = -1L;

   private Anchor(Entity entity, boolean follow, double feetX, double feetY, double feetZ, double offX, double offY, double offZ) {
      this.entity = entity;
      this.follow = follow;
      this.feetX = feetX;
      this.feetY = feetY;
      this.feetZ = feetZ;
      this.offX = offX;
      this.offY = offY;
      this.offZ = offZ;
      this.x = feetX + offX;
      this.y = feetY + offY;
      this.z = feetZ + offZ;
   }

   public static Anchor world(double x, double y, double z) {
      return new Anchor((Entity)null, false, x, y, z, 0.0D, 0.0D, 0.0D);
   }

   public static Anchor fixed(double feetX, double feetY, double feetZ, double offX, double offY, double offZ) {
      return new Anchor((Entity)null, false, feetX, feetY, feetZ, offX, offY, offZ);
   }

   public static Anchor entity(Entity e, double offX, double offY, double offZ, boolean follow, float partialTicks) {
      double fx = e.lastTickPosX + (e.posX - e.lastTickPosX) * (double)partialTicks;
      double fy = e.lastTickPosY + (e.posY - e.lastTickPosY) * (double)partialTicks;
      double fz = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * (double)partialTicks;
      return new Anchor(e, follow, fx, fy, fz, offX, offY, offZ);
   }

   public void update(float partialTicks, World world) {
      if(this.entity != null) {
         boolean gone = this.entity.isDead || world == null || this.entity.worldObj != world || world.getEntityByID(this.entity.getEntityId()) != this.entity;
         if(gone) {
            this.entity = null;
         } else if(this.follow) {
            this.feetX = this.entity.lastTickPosX + (this.entity.posX - this.entity.lastTickPosX) * (double)partialTicks;
            this.feetY = this.entity.lastTickPosY + (this.entity.posY - this.entity.lastTickPosY) * (double)partialTicks;
            this.feetZ = this.entity.lastTickPosZ + (this.entity.posZ - this.entity.lastTickPosZ) * (double)partialTicks;
         }
      }

      this.x = this.feetX + this.offX;
      this.y = this.feetY + this.offY;
      this.z = this.feetZ + this.offZ;
      if(this.first) {
         this.prevX = this.x;
         this.prevY = this.y;
         this.prevZ = this.z;
         this.first = false;
      }

      this.dx = this.x - this.prevX;
      this.dy = this.y - this.prevY;
      this.dz = this.z - this.prevZ;
      this.prevX = this.x;
      this.prevY = this.y;
      this.prevZ = this.z;
      this.frame = FxFrame.currentFrame();
   }

   public Entity entity() {
      return this.entity;
   }

   public double feetX() {
      return this.feetX;
   }

   public double feetY() {
      return this.feetY;
   }

   public double feetZ() {
      return this.feetZ;
   }
}
