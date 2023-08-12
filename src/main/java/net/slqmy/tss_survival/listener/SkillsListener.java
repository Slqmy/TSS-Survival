package net.slqmy.tss_survival.listener;

import net.slqmy.tss_core.datatype.player.Message;
import net.slqmy.tss_core.datatype.player.PlayerProfile;
import net.slqmy.tss_core.datatype.player.survival.SkillData;
import net.slqmy.tss_core.util.DebugUtil;
import net.slqmy.tss_survival.MiningSkillExpReward;
import net.slqmy.tss_survival.TSSSurvivalPlugin;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public class SkillsListener implements Listener {

  private final TSSSurvivalPlugin plugin;

  public SkillsListener(TSSSurvivalPlugin plugin) {
	this.plugin = plugin;
  }

  @EventHandler
  public void onMobKill(@NotNull EntityDeathEvent event) {
	LivingEntity dead = event.getEntity();
	Player killer = dead.getKiller();

	if (killer == null) {
	  return;
	}

	PlayerProfile murdererProfile = plugin.getCore().getPlayerManager().getProfile(killer);
	SkillData skillData = murdererProfile.getSurvivalData().getSkillData();

	int oldLevel = getLevel(skillData.getCombatSkillExperience());

	double gainedExp = 0;
	for (
			Attribute attribute
			: new Attribute[]{
			Attribute.GENERIC_MAX_HEALTH,
			Attribute.GENERIC_ARMOR,
			Attribute.GENERIC_ARMOR_TOUGHNESS,
			Attribute.GENERIC_ATTACK_DAMAGE,
			Attribute.GENERIC_ATTACK_KNOCKBACK,
			Attribute.GENERIC_ATTACK_SPEED,
			Attribute.GENERIC_KNOCKBACK_RESISTANCE,
			Attribute.GENERIC_MOVEMENT_SPEED
	}
	) {
	  AttributeInstance instance = dead.getAttribute(attribute);

	  if (instance != null) {
		gainedExp += instance.getValue();
	  }
	}

	murdererProfile.getSurvivalData().getSkillData().incrementCombatSkillExperience((int) gainedExp);

	int newLevel = getLevel(skillData.getCombatSkillExperience());
	DebugUtil.log("New combat experience: " + skillData.getCombatSkillExperience());

	if (newLevel != oldLevel) {
	  plugin.getCore().getMessageManager().sendMessage(killer, Message.SKILL_LEVEL_UP);
	}
  }

  @EventHandler
  public void onMineBlock(@NotNull BlockBreakEvent event) {
	Block brokenBlock = event.getBlock();
	Material blockType = brokenBlock.getType();

	MiningSkillExpReward miningSkillExpReward;
	try {
	  miningSkillExpReward = MiningSkillExpReward.valueOf(blockType.name());
	} catch (IllegalArgumentException exception) {
	  return;
	}

	PlayerProfile profile = plugin.getCore().getPlayerManager().getProfile(event.getPlayer());
	SkillData skillData = profile.getSurvivalData().getSkillData();

	int currentExp = skillData.getMiningSkillExperience();
	int oldLevel = getLevel(currentExp);

	skillData.incrementMiningSkillExperience(miningSkillExpReward.getExpReward());
	int newLevel = getLevel(skillData.getMiningSkillExperience());

	if (newLevel != oldLevel) {
	  plugin.getCore().getMessageManager().sendMessage(event.getPlayer(), Message.SKILL_LEVEL_UP);
	}
  }

  @EventHandler
  public void onForage(@NotNull BlockBreakEvent event) {
	Block brokenBlock = event.getBlock();
	Material blockType = brokenBlock.getType();
	String blockTypeString = blockType.name();

	int gainedExp = 0;

	if (blockTypeString.endsWith("_LOG") || blockTypeString.endsWith("_WOOD")) {
	  if (blockTypeString.contains("ACACIA_") || blockTypeString.contains("MANGROVE_") || blockTypeString.contains("CHERRY_")) {
		gainedExp = 35;
	  } else {
		gainedExp = 20;
	  }
	} else if (blockTypeString.endsWith("_STEM") || blockTypeString.endsWith("_HYPHAE")) {
	  gainedExp = 25;
	} else {
	  return;
	}

	PlayerProfile profile = plugin.getCore().getPlayerManager().getProfile(event.getPlayer());
	SkillData skillData = profile.getSurvivalData().getSkillData();

	int currentExp = skillData.getForagingSkillExperience();
	int oldLevel = getLevel(currentExp);

	skillData.incrementForagingSkillExperience(gainedExp);
	int newLevel = getLevel(skillData.getForagingSkillExperience());

	if (newLevel != oldLevel) {
	  plugin.getCore().getMessageManager().sendMessage(event.getPlayer(), Message.SKILL_LEVEL_UP);
	}
  }

  private int getLevel(int experience) {
	double power = Math.pow(Math.sqrt(3D) * Math.sqrt(27D * Math.pow(experience, 2D) + 10000D) - 9D * experience, 1D / 3D);
	return (int) Math.floor(Math.pow(10D, 2D / 3D) / (Math.pow(3D, 1D / 3D) * power) - power / Math.pow(30D, 2D / 3D));
  }
}