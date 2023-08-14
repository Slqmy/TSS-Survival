package net.slqmy.tss_survival.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.slqmy.tss_core.TSSCorePlugin;
import net.slqmy.tss_core.datatype.player.Message;
import net.slqmy.tss_core.datatype.player.PlayerProfile;
import net.slqmy.tss_core.datatype.player.survival.ClaimedChunk;
import net.slqmy.tss_core.manager.MessageManager;
import net.slqmy.tss_survival.TSSSurvivalPlugin;
import net.slqmy.tss_survival.util.ClaimUtil;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrustCommand {

  public TrustCommand(TSSSurvivalPlugin plugin) {
	new CommandAPICommand("trust")
			.withArguments(new PlayerArgument("player"))
			.withArguments(
					new StringArgument("where-to-trust")
							.replaceSuggestions(
									ArgumentSuggestions.stringsWithTooltips(
											new IStringTooltip[]{
													StringTooltip.ofString("all", "Trust the player in all of your claimed chunks."),
													StringTooltip.ofString("connected", "Trust the player in all chunks connected to your current chunk."),
													StringTooltip.ofString("chunk", "Trust the player in just the current chunk.")
											}
									)
							)
			)
			.executesPlayer((Player player, CommandArguments args) -> {
			  String whereToSearch = (String) args.get("where-to-trust");
			  assert whereToSearch != null;

			  Chunk currentChunk = player.getChunk();
			  PersistentDataContainer container = currentChunk.getPersistentDataContainer();

			  NamespacedKey chunkClaimOwnerKey = new NamespacedKey(plugin, "chunk_claim_owner");

			  TSSCorePlugin core = plugin.getCore();
			  MessageManager messageManager = core.getMessageManager();

			  UUID playerUuid = player.getUniqueId();

			  if (!whereToSearch.equals("all")) {
				String ownerUuidString = container.get(chunkClaimOwnerKey, PersistentDataType.STRING);

				if (ownerUuidString == null) {
				  messageManager.sendMessage(player, Message.CHUNK_NOT_CLAIMED);
				  return;
				}

				UUID ownerUuid = UUID.fromString(ownerUuidString);

				if (!ownerUuid.equals(playerUuid)) {
				  messageManager.sendMessage(player, Message.NOT_YOUR_CHUNK);
				  return;
				}
			  }

			  Player trustedPlayer = (Player) args.get("player");
			  assert trustedPlayer != null;

			  UUID trustedUuid = trustedPlayer.getUniqueId();

			  if (trustedUuid.equals(playerUuid)) {
				messageManager.sendMessage(player, Message.CANT_TRUST_YOURSELF);
				return;
			  }

			  World world = player.getWorld();

			  PlayerProfile profile = core.getPlayerManager().getProfile(player);
			  ArrayList<ClaimedChunk> claimedChunks = profile.getSurvivalData().getClaims().get(world.getName());

			  NamespacedKey trustKey = new NamespacedKey(plugin, trustedUuid + "_is_trusted");

			  List<int[]> chunksToTrustPlayerIn = new ArrayList<>();

			  int x = currentChunk.getX();
			  int z = currentChunk.getZ();

			  switch (whereToSearch) {
				case "all" -> {
				  for (ClaimedChunk claimedChunk : claimedChunks) {
					ArrayList<UUID> trustedPlayers = claimedChunk.getTrustedPlayers();

					if (trustedPlayers.contains(trustedUuid)) {
					  continue;
					}

					trustedPlayers.add(trustedUuid);

					Chunk chunk = world.getChunkAt(claimedChunk.getX(), claimedChunk.getZ(), false);

					PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
					chunkContainer.set(trustKey, PersistentDataType.BOOLEAN, true);
				  }

				  messageManager.sendMessage(player, Message.PLAYER_SUCCESSFULLY_TRUSTED);
				  return;
				}
				case "connected" ->
				  chunksToTrustPlayerIn = ClaimUtil.getConnectedClaims(chunkClaimOwnerKey, trustKey, player, true, chunksToTrustPlayerIn, new int[] {x, z});
				case "chunk" -> {
				  chunksToTrustPlayerIn = List.of(new int[]{x, z});
				  container.set(trustKey, PersistentDataType.BOOLEAN, true);
				}
			  }

			  for (ClaimedChunk claimedChunk : claimedChunks) {
				assert chunksToTrustPlayerIn != null;

				boolean contains = false;
				for (int[] chunk : chunksToTrustPlayerIn) {
				  if (chunk[0] == claimedChunk.getX() && chunk[1] == claimedChunk.getZ()) {
					contains = true;
					break;
				  }
				}

				if (!contains) {
				  continue;
				}

				ArrayList<UUID> trustedPlayers = claimedChunk.getTrustedPlayers();

				if (trustedPlayers.contains(trustedUuid)) {
				  continue;
				}

				trustedPlayers.add(trustedUuid);
			  }

			  messageManager.sendMessage(player, Message.PLAYER_SUCCESSFULLY_TRUSTED);
			})
			.register();
  }
}