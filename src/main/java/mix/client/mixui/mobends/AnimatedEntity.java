package mix.client.mixui.mobends;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import mix.client.mixui.logger.MixLogger;
import mix.client.mixui.mobends.animation.MoBendsAnimation;
import mix.client.mixui.mobends.client.renderer.entity.RenderBendsPlayer;
import mix.client.mixui.mobends.util.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class AnimatedEntity {
	
	public static List<AnimatedEntity> animatedEntities = new ArrayList<AnimatedEntity>();
	
	public static Map<String, RenderBendsPlayer> skinMap = Maps.newHashMap();
    public static RenderBendsPlayer playerRenderer;
	
	public String id;
	public String displayName;
	public Entity entity;
	
	public Class<? extends Entity> entityClass;
	public Render<?> renderer;
	
	public List<MoBendsAnimation> animations = new ArrayList<MoBendsAnimation>();
	
	public boolean animate = true;
	
	public AnimatedEntity(String argID, String argDisplayName, Entity argEntity, Class<? extends Entity> argClass, Render<?> argRenderer){
		this.id = argID;
		this.displayName = argDisplayName;
		this.entityClass = argClass;
		this.renderer = argRenderer;
		this.entity = argEntity;
		this.animate = true;
	}
	
	public AnimatedEntity add(MoBendsAnimation argGroup){
		this.animations.add(argGroup);
		return this;
	}
	
	public static void register(){
		
		MixLogger.info("[Mo Bends] Registering Animated Entities...");
		
		animatedEntities.clear();
		
		registerEntity(new AnimatedEntity("player","Player",Minecraft.getMinecraft().thePlayer,EntityPlayer.class,new RenderBendsPlayer(Minecraft.getMinecraft().getRenderManager())).
			add(new mix.client.mixui.mobends.animation.player.Animation_Stand()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Walk()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Sneak()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Sprint()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Jump()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Attack()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Swimming()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Bow()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Riding()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Mining()).
			add(new mix.client.mixui.mobends.animation.player.Animation_Axe()));
		
		for(int i = 0;i < AnimatedEntity.animatedEntities.size();i++){
			AnimatedEntity.animatedEntities.get(i).animate = true;
        }
		
		for(int i = 0;i < animatedEntities.size();i++){
			if(animatedEntities.get(i).animate) RenderingRegistry.registerEntityRenderingHandler(animatedEntities.get(i).entityClass, animatedEntities.get(i).renderer);
		}
		
		playerRenderer = new RenderBendsPlayer(Minecraft.getMinecraft().getRenderManager());
		skinMap.put("default", playerRenderer);
		skinMap.put("slim", new RenderBendsPlayer(Minecraft.getMinecraft().getRenderManager(), true));
	}
	
	public static void registerEntity(AnimatedEntity argEntity){
		MixLogger.info("[Mo Bends] Registering " + argEntity.displayName);
		animatedEntities.add(argEntity);
	}
	
	public MoBendsAnimation get(String argName){
		for(int i = 0;i < animations.size();i++){
			if(animations.get(i).getName().equalsIgnoreCase(argName)){
				return animations.get(i);
			}
		}
		return null;
	}
	
	public static AnimatedEntity getByEntity(Entity argEntity){
		for(int i = 0;i < animatedEntities.size();i++){
			if(animatedEntities.get(i).entityClass.isInstance(argEntity)){
				return animatedEntities.get(i);
			}
		}
		return null;
	}

	public static RenderBendsPlayer getPlayerRenderer(AbstractClientPlayer player) {
		String s = ((AbstractClientPlayer)player).getSkinType();
		RenderBendsPlayer renderplayer = (RenderBendsPlayer)skinMap.get(s);
        return renderplayer != null ? renderplayer : playerRenderer;
	}
}
