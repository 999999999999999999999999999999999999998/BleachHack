/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDrinker420/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package bleach.hack.module.mods;

import java.util.ArrayList;
import java.util.List;

import bleach.hack.eventbus.BleachSubscribe;

import bleach.hack.event.events.EventSendPacket;
import bleach.hack.event.events.EventTick;
import bleach.hack.module.ModuleCategory;
import bleach.hack.module.Module;
import bleach.hack.setting.base.SettingMode;
import bleach.hack.setting.base.SettingSlider;
import bleach.hack.setting.base.SettingToggle;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class FakeLag extends Module {

	public List<PlayerMoveC2SPacket> queue = new ArrayList<>();
	public long startTime = 0;

	public FakeLag() {
		super("FakeLag", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Stores up movement packets",
				new SettingMode("Mode", "Always", "Pulse").withDesc("Lag mode"),
				new SettingToggle("Limit", false).withDesc("Disable lag after x seconds").withChildren(
						new SettingSlider("Limit", 0, 15, 5, 1).withDesc("How muny seconds before disabling")),
				new SettingSlider("Pulse", 0, 5, 1, 1).withDesc("Pulse interval"));
	}

	@Override
	public void onEnable() {
		startTime = System.currentTimeMillis();
		queue.clear();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		sendPackets();
		super.onDisable();
	}

	@BleachSubscribe
	public void sendPacket(EventSendPacket event) {
		if (event.getPacket() instanceof PlayerMoveC2SPacket) {
			queue.add((PlayerMoveC2SPacket) event.getPacket());
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (getSetting(0).asMode().mode == 0) {
			if (getSetting(1).asToggle().state &&
					System.currentTimeMillis() - startTime > getSetting(1).asToggle().getChild(0).asSlider().getValue() * 1000)
				setEnabled(false);
		} else if (getSetting(0).asMode().mode == 1) {
			if (System.currentTimeMillis() - startTime > getSetting(2).asSlider().getValue() * 1000) {
				setEnabled(false);
				setEnabled(true);
			}
		}
	}

	public void sendPackets() {
		for (PlayerMoveC2SPacket p : new ArrayList<>(queue)) {
			if (!(p instanceof PlayerMoveC2SPacket.LookOnly)) {
				mc.player.networkHandler.sendPacket(p);
			}
		}

		queue.clear();
	}
}
