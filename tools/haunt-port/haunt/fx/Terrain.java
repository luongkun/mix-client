package mix.client.mixui.integration.haunt.fx;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class Terrain {
   private static final List<AxisAlignedBB> BOXES = new ArrayList();

   private static double topAt(World w, BlockPos pos, double x, double z) {
      IBlockState state = w.getBlockState(pos);
      Block block = state.getBlock();
      if(block.getMaterial() == Material.air) {
         return Double.NaN;
      } else if(block == Blocks.snow_layer) {
         return (double)pos.getY() + (double)((Integer)state.getValue(BlockSnow.LAYERS)).intValue() * 0.125D;
      } else {
         BOXES.clear();
         AxisAlignedBB probe = new AxisAlignedBB(x - 0.02D, (double)pos.getY() - 0.01D, z - 0.02D, x + 0.02D, (double)pos.getY() + 1.6D, z + 0.02D);

         try {
            block.addCollisionBoxesToList(w, pos, state, probe, BOXES, (Entity)null);
         } catch (RuntimeException var141) {
            return Double.NaN;
         }

         double top = Double.NaN;

         for(int i = 0; i < BOXES.size(); ++i) {
            double maxY = ((AxisAlignedBB)BOXES.get(i)).maxY;
            if(Double.isNaN(top) || maxY > top) {
               top = maxY;
            }
         }

         return top;
      }
   }

   public static double surfaceAt(World w, double x, double z, double refY, double up, double down) {
      if(w == null) {
         return refY;
      } else {
         int bx = (int)Math.floor(x);
         int bz = (int)Math.floor(z);
         int top = (int)Math.floor(refY + up);
         int bottom = (int)Math.floor(refY - down) - 1;

         for(int y = top; y >= bottom; --y) {
            double h = topAt(w, new BlockPos(bx, y, bz), x, z);
            if(!Double.isNaN(h)) {
               if(h > refY + up + 0.001D) {
                  return Double.NaN;
               }

               return h >= refY - down - 0.001D?h:Double.NaN;
            }
         }

         return Double.NaN;
      }
   }

   public static double standingSurface(World w, double minX, double minZ, double maxX, double maxZ, double feetY) {
      if(w == null) {
         return feetY;
      } else {
         double best = Double.NaN;
         int x0 = (int)Math.floor(minX + 0.001D);
         int x1 = (int)Math.floor(maxX - 0.001D);
         int z0 = (int)Math.floor(minZ + 0.001D);
         int z1 = (int)Math.floor(maxZ - 0.001D);
         int yTop = (int)Math.floor(feetY + 0.2D);
         int yBottom = (int)Math.floor(feetY - 0.7D);

         for(int bx = x0; bx <= x1; ++bx) {
            for(int bz = z0; bz <= z1; ++bz) {
               double sx = Math.max(minX, Math.min(maxX, (double)bx + 0.5D));
               double sz = Math.max(minZ, Math.min(maxZ, (double)bz + 0.5D));

               for(int y = yTop; y >= yBottom; --y) {
                  BlockPos pos = new BlockPos(bx, y, bz);
                  double h = topAtFootprint(w, pos, minX, minZ, maxX, maxZ);
                  if(Double.isNaN(h)) {
                     h = topAt(w, pos, sx, sz);
                  }

                  if(!Double.isNaN(h) && h <= feetY + 0.2D && h >= feetY - 0.7D) {
                     if(Double.isNaN(best) || h > best) {
                        best = h;
                     }
                     break;
                  }
               }
            }
         }

         return best;
      }
   }

   private static double topAtFootprint(World w, BlockPos pos, double minX, double minZ, double maxX, double maxZ) {
      IBlockState state = w.getBlockState(pos);
      Block block = state.getBlock();
      if(block.getMaterial() == Material.air) {
         return Double.NaN;
      } else if(block == Blocks.snow_layer) {
         return (double)pos.getY() + (double)((Integer)state.getValue(BlockSnow.LAYERS)).intValue() * 0.125D;
      } else {
         BOXES.clear();
         AxisAlignedBB probe = new AxisAlignedBB(minX, (double)pos.getY() - 0.01D, minZ, maxX, (double)pos.getY() + 1.6D, maxZ);

         try {
            block.addCollisionBoxesToList(w, pos, state, probe, BOXES, (Entity)null);
         } catch (RuntimeException var181) {
            return Double.NaN;
         }

         double top = Double.NaN;

         for(int i = 0; i < BOXES.size(); ++i) {
            double maxY = ((AxisAlignedBB)BOXES.get(i)).maxY;
            if(Double.isNaN(top) || maxY > top) {
               top = maxY;
            }
         }

         return top;
      }
   }

   public static int mapColor(World w, double x, double y, double z, int fallback) {
      if(w == null) {
         return fallback;
      } else {
         BlockPos feet = new BlockPos(Math.floor(x), Math.floor(y + 0.05D), Math.floor(z));
         IBlockState atFeet = w.getBlockState(feet);
         if(atFeet.getBlock() != Blocks.snow_layer && atFeet.getBlock() != Blocks.carpet) {
            BlockPos pos = new BlockPos(Math.floor(x), Math.floor(y - 0.01D), Math.floor(z));

            for(int i = 0; i < 2; pos = pos.down()) {
               IBlockState state = w.getBlockState(pos);
               if(state.getBlock().getMaterial() != Material.air) {
                  return color(state, fallback);
               }

               ++i;
            }

            return fallback;
         } else {
            return color(atFeet, fallback);
         }
      }
   }

   private static int color(IBlockState state, int fallback) {
      MapColor color = state.getBlock().getMapColor(state);
      return color != null && color.colorValue != 0?-16777216 | color.colorValue:fallback;
   }

   public static final class HeightField {
      private final double baseY;
      private final double minX;
      private final double minZ;
      private final int res;
      private final int size;
      private final float[] tops;

      private HeightField(double baseY, double minX, double minZ, int res, int size, float[] tops) {
         this.baseY = baseY;
         this.minX = minX;
         this.minZ = minZ;
         this.res = res;
         this.size = size;
         this.tops = tops;
      }

      public static Terrain.HeightField flat(double baseY) {
         return new Terrain.HeightField(baseY, 0.0D, 0.0D, 1, 0, (float[])null);
      }

      public static Terrain.HeightField sample(World w, double x, double baseY, double z, double radius, int res) {
         if(w == null) {
            return flat(baseY);
         } else {
            double minX = Math.floor(x - radius);
            double minZ = Math.floor(z - radius);
            int size = (int)Math.ceil(x + radius - minX) * res + 1;
            size = Math.min(size, 96);
            float[] tops = new float[size * size];
            boolean level = true;

            for(int i = 0; i < size; ++i) {
               for(int j = 0; j < size; ++j) {
                  double sx = minX + ((double)i + 0.5D) / (double)res;
                  double sz = minZ + ((double)j + 0.5D) / (double)res;
                  double h = Terrain.surfaceAt(w, sx, sz, baseY, 1.0D, 1.0D);
                  if(!Double.isNaN(h) && liquidOn(w, sx, h, sz)) {
                     h = Double.NaN;
                  }

                  tops[i * size + j] = (float)(Double.isNaN(h)?Double.NaN:h - baseY);
                  level &= !Double.isNaN(h) && Math.abs(h - baseY) < 0.001D;
               }
            }

            return level?flat(baseY):new Terrain.HeightField(baseY, minX, minZ, res, size, tops);
         }
      }

      private static boolean liquidOn(World w, double x, double top, double z) {
         BlockPos pos = new BlockPos((int)Math.floor(x), (int)Math.floor(top + 0.001D), (int)Math.floor(z));
         return w.getBlockState(pos).getBlock().getMaterial().isLiquid();
      }

      public double baseY() {
         return this.baseY;
      }

      public double at(double x, double z) {
         if(this.tops == null) {
            return this.baseY;
         } else {
            int i = (int)Math.floor((x - this.minX) * (double)this.res);
            int j = (int)Math.floor((z - this.minZ) * (double)this.res);
            if(i >= 0 && j >= 0 && i < this.size && j < this.size) {
               float h = this.tops[i * this.size + j];
               return Float.isNaN(h)?Double.NaN:this.baseY + (double)h;
            } else {
               return this.baseY;
            }
         }
      }

      public boolean isFlat() {
         return this.tops == null;
      }
   }
}
