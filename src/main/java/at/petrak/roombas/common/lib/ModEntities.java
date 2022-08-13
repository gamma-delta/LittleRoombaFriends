package at.petrak.roombas.common.lib;

import at.petrak.roombas.common.entity.EntityRoomba;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import static at.petrak.roombas.api.RoombasModAPI.modLoc;

public class ModEntities {
    public static final EntityType<EntityRoomba> ROOMBA = Registry.register(
        Registry.ENTITY_TYPE,
        modLoc("roomba"),
        FabricEntityTypeBuilder.<EntityRoomba>create(MobCategory.CREATURE, EntityRoomba::new)
            .dimensions(EntityDimensions.fixed(0.75f, 0.25f))
            .build()
    );
}
