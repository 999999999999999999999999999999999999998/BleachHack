/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDrinker420/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package bleach.hack.util;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;

import bleach.hack.gui.option.Option;
import bleach.hack.util.io.BleachOnlineMang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

public class BleachPlayerManager {

	private static MinecraftClient mc = MinecraftClient.getInstance();
	private static final Pattern UUID_ADD_DASHES_PATTERN = Pattern.compile("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)");

	private ScheduledExecutorService pingExecutor;
	private ScheduledExecutorService playerExecutor;

	private Set<UUID> players = new HashSet<>();
	private Set<UUID> playerQueue = new HashSet<>();

	public BleachPlayerManager() {
		playerExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
		playerExecutor.scheduleAtFixedRate(() -> {
			if (!playerQueue.isEmpty()) {
				JsonArray playersJson = new JsonArray();
				playerQueue.forEach(p -> playersJson.add(p.toString()));
				playerQueue.clear();

				String response = BleachOnlineMang.sendApiPost("online/inlistbin", playersJson.toString());

				if (response != null) {
					boolean[] binary = toBinaryArray(response);
					for (int i = 0; i < playersJson.size(); i++) {
						if (binary.length <= i) {
							break;
						}

						if (binary[i]) {
							players.add(UUID.fromString(playersJson.get(i).getAsString()));
						}
					}
				}
			}
		}, 0L, 10L, TimeUnit.SECONDS);

		playerExecutor.scheduleAtFixedRate(() -> {
			if (mc.player == null) {
				players.clear();
				return;
			}

			players.removeIf(p -> !mc.player.getUuid().equals(p) && !mc.player.networkHandler.getPlayerUuids().contains(p));

			if (!players.isEmpty() && !(players.size() == 1 && players.contains(mc.player.getUuid()))) {
				JsonArray playersJson = new JsonArray();
				players.forEach(p -> playersJson.add(p.toString()));

				String response = BleachOnlineMang.sendApiPost("online/inlistbin", playersJson.toString());

				if (response != null) {
					boolean[] binary = toBinaryArray(response);
					for (int i = 0; i < playersJson.size(); i++) {
						if (binary.length <= i) {
							break;
						}

						if (binary[i]) {
							players.add(UUID.fromString(playersJson.get(i).getAsString()));
						}
					}
				}
			}
		}, 0L, 5L, TimeUnit.MINUTES);
	}

	public void startPinger() {
		if (pingExecutor == null || pingExecutor.isShutdown()) {
			pingExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
			pingExecutor.scheduleAtFixedRate(() -> BleachOnlineMang.sendApiGet("online/ping?uuid=" + toProperUUID(mc.getSession().getUuid())), 0L, 14L, TimeUnit.MINUTES);
		}
	}

	public void stopPinger() {
		if (pingExecutor != null && !pingExecutor.isShutdown()) {
			pingExecutor.execute(() -> BleachOnlineMang.sendApiGet("online/disconnect?uuid=" + toProperUUID(mc.getSession().getUuid().replace("-", ""))));
			pingExecutor.shutdown();
		}
	}

	public void addQueueEntries(Collection<PlayerListS2CPacket.Entry> entries) {
		UUID playerUuid = UUID.fromString(toProperUUID(mc.getSession().getUuid()));
		entries.stream()
		.map(e -> e.getProfile().getId())
		.filter(e -> !players.contains(e))
		.forEach(e -> {
			if (playerUuid.equals(e)) {
				if (Option.PLAYERLIST_SHOW_AS_BH_USER.getValue()) {
					players.add(e);
				}
			} else {
				playerQueue.add(e);
			}
		});
	}

	public void removeQueueEntries(Collection<PlayerListS2CPacket.Entry> entries) {
		entries.forEach(e -> playerQueue.remove(e.getProfile().getId()));
	}

	public Set<UUID> getPlayers() {
		return players;
	}

	public static String toProperUUID(String uuid) {
		if (uuid.contains("-")) {
			return uuid;
		}

		return UUID_ADD_DASHES_PATTERN.matcher(uuid).replaceFirst("$1-$2-$3-$4-$5");
	}

	private static boolean[] toBinaryArray(String string) {
		boolean[] array = new boolean[string.length() * 8];
		int index = 0;
		for (byte b: string.getBytes(StandardCharsets.ISO_8859_1)) {
			for (byte bit = 7; bit >= 0; bit--) {
				array[index] = ((b >> bit) & 1) == 1;
				index++;
			}
		}

		return array;
	}
}
