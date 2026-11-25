package mix.client.mixui.integration.haunt.fx.physics;

import mix.client.mixui.integration.haunt.fx.physics.RigidBox;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public final class BlockCollider {
   private static final double RESTING = 0.005D;
   private final MutableBlockPos pos = new MutableBlockPos();
   public World world;
   private double floorY = Double.NaN;
   private AxisAlignedBB floor;
   private boolean floorBlocks;

   public BlockCollider(World world) {
      this.world = world;
   }

   public void setFloor(double y) {
      this.floorY = y;
      this.floor = Double.isNaN(y)?null:new AxisAlignedBB(-1.0E7D, y - 1000.0D, -1.0E7D, 1.0E7D, y, 1.0E7D);
      this.floorBlocks = false;
   }

   public void setBlockFloor(double y) {
      this.setFloor(y);
      this.floorBlocks = true;
   }

   public void collide(RigidBox b) {
      this.collide(b, true);
   }

   public void separate(RigidBox b) {
      this.collide(b, false);
   }

   private void collide(RigidBox b, boolean respond) {
      if((this.world != null || this.floor != null) && b.invMass != 0.0D && !b.sleeping) {
         for(int i = 0; i < 8; ++i) {
            if(this.resolve(b, i, respond)) {
               b.touched = true;
            }
         }
      }

   }

   public boolean solid(double x, double y, double z) {
      return this.box(x, y, z) != null;
   }

   private boolean free(double x, double y, double z) {
      return this.box(x, y, z) == null;
   }

   private AxisAlignedBB box(double x, double y, double z) {
      if(this.world == null) {
         if(this.floor != null && y < this.floorY) {
            if(this.floorBlocks) {
               double bx = Math.floor(x);
               double bz = Math.floor(z);
               double by = Math.floor(y);
               return new AxisAlignedBB(bx, by, bz, bx + 1.0D, Math.min(by + 1.0D, this.floorY), bz + 1.0D);
            } else {
               return this.floor;
            }
         } else {
            return null;
         }
      } else if(y >= 0.0D && y < 256.0D) {
         this.pos.set((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
         IBlockState state = this.world.getBlockState(this.pos);
         Block block = state.getBlock();
         if(!block.getMaterial().blocksMovement()) {
            return null;
         } else {
            AxisAlignedBB bb;
            try {
               bb = block.getCollisionBoundingBox(this.world, this.pos, state);
            } catch (RuntimeException var13) {
               return null;
            }

            return bb != null && x >= bb.minX && x <= bb.maxX && y >= bb.minY && y <= bb.maxY && z >= bb.minZ && z <= bb.maxZ?bb:null;
         }
      } else {
         return null;
      }
   }

   private boolean resolve(RigidBox b, int i, boolean respond) {
      double x = b.x[i];
      double y = b.y[i];
      double z = b.z[i];
      AxisAlignedBB bb = this.box(x, y, z);
      if(bb == null) {
         return false;
      } else {
         double px = b.ox[i];
         double py = b.oy[i];
         double pz = b.oz[i];
         double nx = 0.0D;
         double ny = 0.0D;
         double nz = 0.0D;
         double eps = 1.0E-4D;
         double out = 0.01D;
         double depth;
         if(py >= bb.maxY - 0.001D && this.free(x, bb.maxY + 0.01D, z)) {
            ny = 1.0D;
            depth = bb.maxY - y;
         } else if(py <= bb.minY + 0.001D && this.free(x, bb.minY - 0.01D, z)) {
            ny = -1.0D;
            depth = y - bb.minY;
         } else if(px <= bb.minX + 0.001D && this.free(bb.minX - 0.01D, y, z)) {
            nx = -1.0D;
            depth = x - bb.minX;
         } else if(px >= bb.maxX - 0.001D && this.free(bb.maxX + 0.01D, y, z)) {
            nx = 1.0D;
            depth = bb.maxX - x;
         } else if(pz <= bb.minZ + 0.001D && this.free(x, y, bb.minZ - 0.01D)) {
            nz = -1.0D;
            depth = z - bb.minZ;
         } else if(pz >= bb.maxZ - 0.001D && this.free(x, y, bb.maxZ + 0.01D)) {
            nz = 1.0D;
            depth = bb.maxZ - z;
         } else {
            ny = 1.0D;
            depth = bb.maxY - y;
            double best = Double.MAX_VALUE;
            if(bb.maxY - y < best && this.free(x, bb.maxY + 0.01D, z)) {
               best = bb.maxY - y;
            }

            if(y - bb.minY < best && this.free(x, bb.minY - 0.01D, z)) {
               best = y - bb.minY;
               ny = -1.0D;
            }

            if(x - bb.minX < best && this.free(bb.minX - 0.01D, y, z)) {
               best = x - bb.minX;
               ny = 0.0D;
               nx = -1.0D;
            }

            if(bb.maxX - x < best && this.free(bb.maxX + 0.01D, y, z)) {
               best = bb.maxX - x;
               ny = 0.0D;
               nx = 1.0D;
            }

            if(z - bb.minZ < best && this.free(x, y, bb.minZ - 0.01D)) {
               best = z - bb.minZ;
               ny = 0.0D;
               nx = 0.0D;
               nz = -1.0D;
            }

            if(bb.maxZ - z < best && this.free(x, y, bb.maxZ + 0.01D)) {
               best = bb.maxZ - z;
               ny = 0.0D;
               nx = 0.0D;
               nz = 1.0D;
            }

            if(best != Double.MAX_VALUE) {
               depth = best;
            }
         }

         if(!respond) {
            double sx = nx * (depth + 1.0E-4D);
            double sy = ny * (depth + 1.0E-4D);
            double sz = nz * (depth + 1.0E-4D);
            b.x[i] = x + sx;
            b.y[i] = y + sy;
            b.z[i] = z + sz;
            b.ox[i] = px + sx;
            b.oy[i] = py + sy;
            b.oz[i] = pz + sz;
            return true;
         } else {
            double vx = x - px;
            double vy = y - py;
            double vz = z - pz;
            double vn = vx * nx + vy * ny + vz * nz;
            double tx = vx - vn * nx;
            double ty = vy - vn * ny;
            double tz = vz - vn * nz;
            double pressed = vn < 0.0D?-vn:0.0D;
            double bounce = vn < 0.0D?(pressed > 0.005D?-vn * (double)b.restitution:0.0D):vn;
            double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
            double keep = tl < 1.0E-12D?0.0D:Math.max(0.0D, 1.0D - (double)b.friction * (pressed + bounce) / tl);
            vx = tx * keep + nx * bounce;
            vy = ty * keep + ny * bounce;
            vz = tz * keep + nz * bounce;
            double var53;
            b.x[i] = var53 = x + nx * (depth + 1.0E-4D);
            double var54;
            b.y[i] = var54 = y + ny * (depth + 1.0E-4D);
            double var55;
            b.z[i] = var55 = z + nz * (depth + 1.0E-4D);
            b.ox[i] = var53 - vx;
            b.oy[i] = var54 - vy;
            b.oz[i] = var55 - vz;
            return true;
         }
      }
   }
}
