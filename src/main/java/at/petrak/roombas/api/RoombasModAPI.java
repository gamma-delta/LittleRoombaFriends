package at.petrak.roombas.api;

import net.minecraft.resources.ResourceLocation;

public interface RoombasModAPI {
    String MOD_ID = "roombas";

    static ResourceLocation modLoc(String s) {
        return new ResourceLocation(MOD_ID, s);
    }
}
