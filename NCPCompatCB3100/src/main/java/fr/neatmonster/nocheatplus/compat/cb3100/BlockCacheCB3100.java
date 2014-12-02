package fr.neatmonster.nocheatplus.compat.cb3100;

import java.util.Iterator;
import java.util.List;

import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import net.minecraft.server.v1_7_R4.Block;
import net.minecraft.server.v1_7_R4.EntityBoat;
import net.minecraft.server.v1_7_R4.IBlockAccess;
import net.minecraft.server.v1_7_R4.TileEntity;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Entity;

import fr.neatmonster.nocheatplus.utilities.BlockCache;

public class BlockCacheCB3100 extends BlockCache implements IBlockAccess{

    /** Box for one time use, no nesting, no extra storing this(!). */
    protected static final AxisAlignedBB useBox = AxisAlignedBB.a(0, 0, 0, 0, 0, 0);

    protected net.minecraft.server.v1_7_R4.WorldServer world;

    public BlockCacheCB3100(World world) {
        setAccess(world);
    }

    @Override
    public void setAccess(World world) {
        if (world != null) {
            this.maxBlockY = world.getMaxHeight() - 1;
            this.world = ((CraftWorld) world).getHandle();
        } else {
            this.world = null;
        }
    }

    @Override
    public int fetchTypeId(final int x, final int y, final int z) {
        return world.getTypeId(x, y, z);
    }

    @Override
    public int fetchData(final int x, final int y, final int z) {
        return world.getData(x, y, z);
    }

    @Override
    public double[] fetchBounds(final int x, final int y, final int z){
        final int id = getTypeId(x, y, z);
        final net.minecraft.server.v1_7_R4.Block block = net.minecraft.server.v1_7_R4.Block.getById(id);
        if (block == null) {
            // TODO: Convention for null bounds -> full ?
            return null;
        }
        block.updateShape(this, x, y, z);

        // minX, minY, minZ, maxX, maxY, maxZ
        return new double[]{block.x(), block.z(), block.B(), block.y(),  block.A(),  block.C()};
    }

    @Override
    public boolean standsOnEntity(final Entity entity, final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ){
        try{
            // TODO: Find some simplification!

            final net.minecraft.server.v1_7_R4.Entity mcEntity  = ((CraftEntity) entity).getHandle();

            final AxisAlignedBB box = useBox.b(minX, minY, minZ, maxX, maxY, maxZ);
            @SuppressWarnings("rawtypes")
            final List list = world.getEntities(mcEntity, box);
            @SuppressWarnings("rawtypes")
            final Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                final net.minecraft.server.v1_7_R4.Entity other = (net.minecraft.server.v1_7_R4.Entity) iterator.next();
                if (!(other instanceof EntityBoat)){ // && !(other instanceof EntityMinecart)) continue;
                    continue;
                }
                if (minY >= other.locY && minY - other.locY <= 0.7){
                    return true;
                }
                // Still check this for some reason.
                final AxisAlignedBB otherBox = other.boundingBox;
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
    }

    @Override
    public TileEntity getTileEntity(final int x, final int y, final int z) {
        return world.getTileEntity(x, y, z);
    }

    @Override
    public int getBlockPower(final int arg0, final int arg1, final int arg2, final int arg3) {
        return world.getBlockPower(arg0, arg1, arg2, arg3);
    }

    @Override
    public Block getType(int x, int y, int z) {
        return world.getType(x, y, z);
    }

}