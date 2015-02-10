package com.lulan.shincolle.ai;

import java.util.Random;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IEntityShip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;

/**ENTITY RANGE ATTACK AI
 * 從骨弓的射箭AI修改而來
 * entity必須實作attackEntityWithAmmo, attackEntityWithHeavyAmmo 兩個方法
 */
public class EntityAIShipRangeAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityShip host;  	//entity with AI
    private EntityLivingBase attackTarget;  //entity of target
    private int delayLight = 0;			//light attack delay (attack when time 0)
    private int maxDelayLight;	    //light attack max delay (calc from ship attack speed)
    private int delayHeavy = 0;			//heavy attack delay
    private int maxDelayHeavy;	    //heavy attack max delay (= light delay x5)    
    private double entityMoveSpeed;	//move speed when finding attack path
    private int onSightTime;		//target on sight time
    private float attackRange;		//attack range
    private float rangeSq;			//attack range square
    private int aimTime;			//time before fire
    
    //直線前進用餐數
    private double distSq, distX, distY, distZ, motX, motY, motZ;	//跟目標的直線距離(的平方)
    
 
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipRangeAttack(BasicEntityShip host) {

        if (!(host instanceof BasicEntityShip)) {
            throw new IllegalArgumentException("RangeAttack AI requires BasicEntityShip with attackEntityWithAmmo");
        }
        else {
            this.host = host;
            this.setMutexBits(3);
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
    	EntityLivingBase target = this.host.getAttackTarget();
    	
        if (target != null && 
        	(this.host.getEntityFlag(AttrID.F_UseAmmoLight) || this.host.getEntityFlag(AttrID.F_UseAmmoHeavy)) && 
        	(this.host.hasAmmoLight() || this.host.hasAmmoHeavy())) {   
        	this.attackTarget = target;
            return true;
        }       
        
        return false;
    }
    
    //init AI parameter, call once every target
    @Override
    public void startExecuting() {
    	this.maxDelayLight = (int)(20F / (this.host.getFinalState(AttrID.SPD)));
    	this.maxDelayHeavy = (int)(100F / (this.host.getFinalState(AttrID.SPD)));    	
    	this.aimTime = (int) (20F * (float)( 150 - this.host.getShipLevel() ) / 150F) + 10;        
    	
    	//if target changed, check the delay time from prev attack
    	if(this.delayLight <= this.aimTime) {
    		this.delayLight = this.aimTime;
    	}
    	if(this.delayHeavy <= this.aimTime * 2) {
    		this.delayHeavy = this.aimTime * 2;
    	}
    	
        this.attackRange = this.host.getFinalState(AttrID.HIT) + 1F;
        this.rangeSq = this.attackRange * this.attackRange;
        
        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
       
    }

    //判定是否繼續AI： 有target就繼續, 或者已經移動完畢就繼續
    public boolean continueExecuting() {
        return this.shouldExecute() || !this.host.getNavigator().noPath();
    }

    //重置AI方法
    public void resetTask() {
        this.attackTarget = null;
        this.onSightTime = 0;
        this.delayLight = this.aimTime;
        this.delayHeavy = this.aimTime;
    }

    //進行AI
    public void updateTask() {
    	boolean onSight = false;	//判定直射是否無障礙物
    	  	
    	if(this.attackTarget != null) {  //for lots of NPE issue-.-	
    		this.distX = this.attackTarget.posX - this.host.posX;
    		this.distY = this.attackTarget.posY - this.host.posY;
    		this.distZ = this.attackTarget.posZ - this.host.posZ;	
    		this.distSq = distX*distX + distY*distY + distZ*distZ;
    		    		
            onSight = this.host.getEntitySenses().canSee(this.attackTarget);  

	        //可直視, 則parf++, 否則重置為0
	        if(onSight) {
	            ++this.onSightTime;
	        }
	        else {
	            this.onSightTime = 0;
	        }
	        
	        //若直視太久(每12秒檢查一次), 有50%機率清除目標, 使其重新選一次目標
	        if((this.onSightTime > 240) && (this.onSightTime % 240 == 0)) {
	        	if(rand.nextInt(2) > 0)  {
	        		this.resetTask();
	        		return;
	        	}
	        }
	
	        //若目標進入射程, 且目標無障礙物阻擋, 則清空AI移動的目標, 以停止繼續移動      
	        if(distSq < (double)this.rangeSq && onSight) {
	            this.host.getNavigator().clearPathEntity();
	        }
	        else {	//目標移動, 則繼續追擊	
	        	//在液體中, 採直線前進
	        	if(this.host.getShipDepth() > 0D) {
	        		//額外加上y軸速度, getPathToXYZ對空氣跟液體方塊無效, 因此y軸速度要另外加
	        		if(this.distY > 1.5D && this.host.getShipDepth() > 1.5D) {  //避免水面彈跳
	        			this.motY = 0.2F;
	        		}
	        		else if(this.distY < -1D) {
	        			this.motY = -0.2F;
	        		}
	  		
	        		//若水平撞到東西, 則嘗試跳跳
	        		if(this.host.isCollidedHorizontally) {
	        			this.host.setPosition(this.host.posX, this.host.posY + 0.3D, this.host.posZ);
	        		}
	        		
	        		//若直線可視, 則直接直線移動
	        		if(this.host.getEntitySenses().canSee(this.attackTarget)) {
	        			double PetSpeed = this.host.getFinalState(AttrID.MOV);
	        			this.motX = (this.distX / this.distSq) * PetSpeed * 6D;
	        			this.motZ = (this.distZ / this.distSq) * PetSpeed * 6D;

	        			this.host.motionY = this.motY;
	        			this.host.getMoveHelper().setMoveTo(this.host.posX+this.motX, this.host.posY+this.motY, this.host.posZ+this.motZ, 1D);
	        		}
	           	}
            	else {	//非液體中, 採用一般尋找路徑法
            		this.host.getNavigator().tryMoveToEntityLiving(this.attackTarget, 1D);
            	}
            }
	
	        //設定攻擊時, 頭部觀看的角度
	        this.host.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
	        
	        //delay time decr
	        this.delayLight--;
	        this.delayHeavy--;

	        //若attack delay倒數完了且瞄準時間夠久, 則開始攻擊
	        if(this.delayHeavy <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoHeavy() && this.host.getEntityFlag(AttrID.F_UseAmmoHeavy)) {
	        	//若目標跑出範圍 or 目標被阻擋 or 距離太近, 則停止攻擊, 進行下一輪ai判定
	            if(distSq > (double)this.rangeSq || distSq < 4D || !onSight) { return; }
	            
	            //使用entity的attackEntityWithAmmo進行傷害計算
	            this.host.attackEntityWithHeavyAmmo(this.attackTarget);
	            this.delayHeavy = this.maxDelayHeavy;
	        } 
	        
	        //若attack delay倒數完了且瞄準時間夠久, 則開始攻擊
	        if(this.delayLight <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoLight() && this.host.getEntityFlag(AttrID.F_UseAmmoLight)) {
	        	//若目標跑出範圍 or 目標被阻擋, 則停止攻擊, 進行下一輪ai判定
	            if(distSq > (double)this.rangeSq || !onSight) { return; }
	            
	            //使用entity的attackEntityWithAmmo進行傷害計算
	            this.host.attackEntityWithAmmo(this.attackTarget);
	            this.delayLight = this.maxDelayLight;
	        }
	        
	        //若超過太久都打不到目標(或是追不到), 則重置目標
	        if(this.delayHeavy < -200 || this.delayLight < -200) {
	        	this.resetTask();
	        	return;
	        }
    	}
    }
}
