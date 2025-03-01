package bleach.hack.module.mods;

import bleach.hack.event.events.EventSendPacket;
import bleach.hack.event.events.EventTick;
import bleach.hack.eventbus.BleachSubscribe;
import bleach.hack.module.Module;
import bleach.hack.module.ModuleCategory;
import bleach.hack.setting.base.SettingMode;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class Sneak extends Module {

	private boolean packetSent;

	public Sneak() {
		super("Sneak", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Makes you automatically sneak.",
				new SettingMode("Mode", "Legit", "Packet").withDesc("Mode for sneaking (Only other players will see u sneaking with packet mode)."));
	}

	@Override
	public void onDisable() {
		super.onDisable();

		packetSent = false;
		mc.options.keySneak.setPressed(false);
		mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
	}

	@Override
	public void onEnable() {
		super.onEnable();

		if (getSetting(0).asMode().mode == 1) {
			mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
			packetSent = true;
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (getSetting(0).asMode().mode == 0) {
			mc.options.keySneak.setPressed(true);
		} else if (getSetting(0).asMode().mode == 1 && !packetSent) {
			mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
			packetSent = true;
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventSendPacket event) {
		if (event.getPacket() instanceof ClientCommandC2SPacket) {
			ClientCommandC2SPacket p = (ClientCommandC2SPacket) event.getPacket();
			if (p.getMode() == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY)
				event.setCancelled(true);
		}
	}
}
