package at.petrak.roombas;

import at.petrak.roombas.api.RoombasModAPI;
import at.petrak.roombas.vm.Peripheral;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static at.petrak.roombas.api.RoombasModAPI.modLoc;

public class RoombasMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(RoombasModAPI.MOD_ID);

	public static final ItemApiLookup<Peripheral, Void> PERIPHERALS = ItemApiLookup.get(modLoc("peripherals"),
		Peripheral.class, Void.class);

	@Override
	public void onInitialize() {
		PERIPHERALS.registerSelf();
	}
}
