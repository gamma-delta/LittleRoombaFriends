package at.petrak.roombas.common.item;

import at.petrak.roombas.vm.Peripheral;
import net.minecraft.world.item.Item;

public abstract class ItemPeripheral extends Item implements Peripheral {
    public ItemPeripheral(Properties properties) {
        super(properties);
    }

    
}
