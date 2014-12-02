package fr.neatmonster.nocheatplus.compat.cb3100;

import java.util.Iterator;
import java.util.List;

import net.minecraft.server.v1_8_R1.*;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;

import fr.neatmonster.nocheatplus.utilities.BlockCache;

public class BlockCacheCBDev extends BlockCache implements IBlockAccess{

    protected net.minecraft.server.v1_8_R1.WorldServer world;
    protected World bukkitWorld; // WHACKS

    public BlockCacheCBDev(World world) {
        setAccess(world);
    }

    @Override
    public void setAccess(World world) {
        if (world != null) {
            this.maxBlockY = world.getMaxHeight() - 1;
            this.world = ((CraftWorld) world).getHandle();
            this.bukkitWorld = world;
        } else {
            this.world = null;
            this.bukkitWorld = null;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public int fetchTypeId(final int x, final int y, final int z) {
        return bukkitWorld.getBlockTypeIdAt(x, y, z);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int fetchData(final int x, final int y, final int z) {
        return bukkitWorld.getBlockAt(x, y, z).getData();
    }

    @Override
    public double[] fetchBounds(final int x, final int y, final int z){
        final int id = getTypeId(x, y, z);		
        final net.minecraft.server.v1_8_R1.Block block = net.minecraft.server.v1_8_R1.Block.getById(id);
        if (block == null) {
            // TODO: Convention for null bounds -> full ?
            return null;
        }
        block.updateShape(this, new BlockPosition(x, y, z));


        // minX, minY, minZ, maxX, maxY, maxZ
        return new double[]{block.z(), block.B(), block.D(), block.A(),  block.C(),  block.E()};
    }

    @Override
    public boolean standsOnEntity(final Entity entity, final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ){
        try{
            // TODO: Find some simplification!

            final net.minecraft.server.v1_8_R1.Entity mcEntity  = ((CraftEntity) entity).getHandle();

            final AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
            @SuppressWarnings("rawtypes")
            final List list = world.getEntities(mcEntity, box);
            @SuppressWarnings("rawtypes")
            final Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                final net.minecraft.server.v1_8_R1.Entity other = (net.minecraft.server.v1_8_R1.Entity) iterator.next();
                if (!(other instanceof EntityBoat)){ // && !(other instanceof EntityMinecart)) continue;
                    continue;
                }
                if (minY >= other.locY && minY - other.locY <= 0.7){
                    return true;
                }
                // Still check this for some reason.
                final AxisAlignedBB otherBox = other.getBoundingBox();
                if (box.a > otherBox.d || box.d < otherBox.a || box.b > otherBox.e || box.e < otherBox.b || box.c > otherBox.f || box.f < otherBox.c) {
                    continue;
                }
                else {
                    return true;
                }
            }
        }
        catch (Throwable t){
            // Ignore exceptions (Context: DisguiseCraft).
        }
        return false;
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.utilities.BlockCache#cleanup()
     */
    @Override
    public void cleanup() {
        super.cleanup();
        world = null;
        bukkitWorld = null;
    }

    @Override
    public int getBlockPower(BlockPosition pos, EnumDirection dir) {
        return world.getBlockPower(pos, dir);
    }

    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        return world.getTileEntity(pos);
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        return world.getType(pos);
    }

    @Override
    public boolean isEmpty(BlockPosition pos) {
        return world.isEmpty(pos);
    }

}
