package com.lulan.shincolle.block;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.lulan.shincolle.proxy.ClientProxy;

public class BlockFrame extends BasicBlockFacing
{
	
	public static final String NAME = "BlockFrame";
	protected static final AxisAlignedBB AABB_Frame = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);

	
	public BlockFrame()
	{
		super(Material.IRON);
		this.setUnlocalizedName(NAME);
		this.setRegistryName(NAME);
		this.setHarvestLevel("pickaxe", 0);
	    this.setHardness(1F);
	    this.setResistance(40F);
	    this.setLightOpacity(0);
	    this.setTickRandomly(false);
	    this.setSoundType(SoundType.METAL);
	    
        GameRegistry.register(this);
        GameRegistry.register(new ItemBlock(this), this.getRegistryName());
        
        //default state
        this.setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	@SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World w, BlockPos pos, Random rand) {}

	@Override
	public boolean getTickRandomly()
	{
        return false;
    }
	
	/** 設定AABB */
	@Override
	public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn)
    {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_Frame);
    }
	
	@Override
	@SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }
	
	@Override
	public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }
	
	@Override
	public boolean isNormalCube(IBlockState state)
	{
		return false;
	}

	@Override
	public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity)
	{
		return true;
	}
	
	/** do not block player's sight if player in the block */
	@Override
    public boolean isVisuallyOpaque()
    {
        return false;
    }
	
	/** ladder movement */
	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity)
	{
		//if client player
		if (entity instanceof EntityPlayer && world.isRemote)
		{
			GameSettings keySet = ClientProxy.getGameSetting();
			
			if (keySet.keyBindForward.isKeyDown())
			{
				entity.addVelocity(0D, 0.4D, 0D);
			}
		}
		
		//最低下降速度
		if (entity.motionY < -0.1D)
		{
			entity.motionY = -0.1D;
		}
		//最高上升速度
		else if (entity.motionY > 0.4D)
		{
			entity.motionY = 0.4D;
		}
		//蹲下時速度
		if (entity.isSneaking())
		{
			entity.motionY = 0.08D;
		}
		//重設墜落距離
		entity.fallDistance = 0F;
		
	}
	
	/** random facing on placed */
	@Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		//只考慮水平四個方向
//		world.setBlockState(pos, state.withProperty(FACING, EnumFacing.getHorizontal(rand.nextInt(4))), 2);
		//依照人物面向調整
		world.setBlockState(pos, state.withProperty(FACING, getFacingFromEntity(pos, placer)), 2);
	}
	
//	//can leash TODO issue: knot will setDead after 100 ticks
//	@Override
//	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitx, float hity, float hitz) {
//        ItemStack i = player.inventory.getCurrentItem();
//        
//		if(i != null && i.getItem() instanceof ItemLead) {
//			if(world.isRemote) {
//	        	return true;
//	        }
//			else {
//				//set leash
//				EntityLeashKnot knot = EntityLeashKnot.getKnotForBlock(world, x, y, z);
//		        
//				//get all nearby entity leashed by player
//				double d0 = 7.0D;
//		        List<EntityLiving> list = world.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox((double)x - d0, (double)y - d0, (double)z - d0, (double)x + d0, (double)y + d0, (double)z + d0));
//
//		        if(list != null) {
//		        	for(EntityLiving ent : list) {
//		        		if(ent.getLeashed() && ent.getLeashedToEntity() == player) {
//		        			//if no knot, create one
//		                    if(knot == null) {
//		                    	knot = EntityLeashKnot.func_110129_a(world, x, y, z);
//		                    }
//
//		                    //leash the entity to knot
//		                    ent.setLeashedToEntity(knot, true);
//		                }
//		        	}
//		        }
//		        
//		        //tweak knot position
//		        if(knot != null) {
//		        	switch(side) {
//		        	case 0:
//		        		knot.setPosition(knot.posX, knot.posY - 0.5D, knot.posZ);
//		        		break;
//		        	case 1:
//		        		knot.setPosition(knot.posX, knot.posY + 0.5D, knot.posZ);
//						break;
//					case 2:
//						knot.setPosition(knot.posX, knot.posY, knot.posZ - 0.5D);
//						break;
//					case 3:
//						knot.setPosition(knot.posX, knot.posY, knot.posZ + 0.5D);
//						break;
//					case 4:
//						knot.setPosition(knot.posX - 0.5D, knot.posY, knot.posZ);
//						break;
//					case 5:
//						knot.setPosition(knot.posX + 0.5D, knot.posY, knot.posZ);
//						break;
//			        }
//		        }
//			}//end server side
//		}//end holding lead
//		
//		return false;
//    }
	
	
}