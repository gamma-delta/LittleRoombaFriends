package at.petrak.roombas.common.entity;

import at.petrak.roombas.RoombasMod;
import at.petrak.roombas.common.lib.ModEntities;
import at.petrak.roombas.vm.Peripheral;
import at.petrak.roombas.vm.RoombaVM;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static at.petrak.roombas.api.RoombasModAPI.modLoc;

public class EntityRoomba extends Entity implements ContainerEntity {
    public static final int MAIN_INVENTORY_SIZE = 27;

    // The addl modes in the base VM are indicators to *start* doing whatever it is.
    public static final ResourceLocation MODE_MOVING = modLoc("entity/moving");
    public static final ResourceLocation MODE_TURNING = modLoc("entity/turning");
    public static final ResourceLocation MODE_SLEEPING = modLoc("entity/sleeping");

    // In blocks/tick
    public static final double MOVEMENT_PER_TICK = 2d / 20d;
    // In radians per tick
    public static final double TURNING_PER_TICK = 6.28d / 20d;

    // An inventory like a chest
    private NonNullList<ItemStack> inventory;
    // 16 punchcard slots; the first must be filled to execute
    private NonNullList<ItemStack> punchcards;
    // 4 peripheral slots, must insert things with the right CC on them
    private NonNullList<ItemStack> peripherals;

    // this is null when not executing
    private @Nullable RoombaVMImpl vm;

    public EntityRoomba(EntityType<?> type, Level level) {
        super(type, level);
        this.inventory = NonNullList.withSize(MAIN_INVENTORY_SIZE, ItemStack.EMPTY);
        this.punchcards = NonNullList.withSize(RoombaVM.MAX_CARD_COUNT, ItemStack.EMPTY);
        this.peripherals = NonNullList.withSize(RoombaVM.MAX_PERIPHERAL_COUNT, ItemStack.EMPTY);

        this.vm = null;
    }

    /**
     * Ctor used when placing it presumably
     */
    public EntityRoomba(Level level, double x, double y, double z) {
        this(ModEntities.ROOMBA, level);
        this.setPos(x, y, z);
    }

    private double facingRadians() {
        var look = this.getLookAngle();
        return Mth.atan2(look.z, look.x);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.vm != null) {
            // VM modes
            ResourceLocation mode = this.vm.mode;
            if (mode.equals(RoombaVM.MODE_MOVE)) {
                this.vm.mode = MODE_MOVING;
                this.vm.modeData.putDouble("startX", this.getX());
                this.vm.modeData.putDouble("startZ", this.getZ());
            } else if (mode.equals(RoombaVM.MODE_TURN)) {
                this.vm.mode = MODE_TURNING;
                // we LIE and store things as radians bwahaha
                this.vm.modeData.putDouble("startAngle", this.facingRadians());
            } else if (mode.equals(RoombaVM.MODE_SLEEP)) {
                this.vm.mode = MODE_SLEEPING;
            }
            // Entity modes
            else if (mode.equals(MODE_MOVING)) {
                var startX = this.vm.modeData.getDouble("startX");
                var startZ = this.vm.modeData.getDouble("startZ");
                var dist = this.vm.modeData.getShort("distance") * 16d;

                var dx = this.getX() - startX;
                var dz = this.getZ() - startZ;
                if (dx * dx + dz * dz >= dist) {
                    // all done!
                    this.vm.mode = RoombaVM.MODE_EXECUTING;
                    this.vm.modeData = new CompoundTag();
                } else {
                    var movement = new Vec3(dx, 0, dz).normalize().scale(MOVEMENT_PER_TICK);
                    this.move(MoverType.SELF, movement);
                }
            } else if (mode.equals(MODE_TURNING)) {
                var startAngle = this.vm.modeData.getDouble("startAngle");
                var angle = this.facingRadians();

                if (Math.abs(startAngle - angle) < 0.0001) {
                    // all done!
                    this.vm.mode = RoombaVM.MODE_EXECUTING;
                    this.vm.modeData = new CompoundTag();
                } else {
                    var dth = Mth.clamp(startAngle - angle, -TURNING_PER_TICK, TURNING_PER_TICK);
                    this.turn(dth, 0);
                }
            } else if (mode.equals(MODE_SLEEPING)) {
                var time = this.vm.modeData.getShort("time");
                if (time <= 0) {
                    // all done!
                    this.vm.mode = RoombaVM.MODE_EXECUTING;
                    this.vm.modeData = new CompoundTag();
                } else {
                    this.vm.modeData.putShort("time", (short) (time - 1));
                }
            } else {
                // Either it's execution, or a peripheral...
                // something the VM should handle in any case
                this.vm.tickInherent();
            }
        }
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        saveInventory(tag, "inventory", this.inventory);
        saveInventory(tag, "punchcards", this.punchcards);
        saveInventory(tag, "inventory", this.inventory);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.inventory.clear();
        readInventory(tag, "inventory", this.inventory);

        this.punchcards.clear();
        readInventory(tag, "punchcards", this.punchcards);

        this.peripherals.clear();
        readInventory(tag, "peripherals", this.peripherals);
    }

    private static void saveInventory(CompoundTag tag, String key, NonNullList<ItemStack> src) {
        var out = new ListTag();

        for (int i = 0; i < src.size(); ++i) {
            var stack = src.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                out.add(itemTag);
            }
        }

        tag.put(key, out);
    }

    private static void readInventory(CompoundTag tag, String key, NonNullList<ItemStack> dest) {
        var items = tag.getList("key", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); ++i) {
            var itemTag = items.getCompound(i);
            int j = itemTag.getByte("Slot") & 255;
            if (j < dest.size()) {
                dest.set(j, ItemStack.of(itemTag));
            }
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        // TODO
        return null;
    }

    // region Container boilerplate

    @Nullable
    @Override
    public ResourceLocation getLootTable() {
        return null;
    }

    @Override
    public void setLootTable(@Nullable ResourceLocation resourceLocation) {
        // NO-OP
    }

    @Override
    public long getLootTableSeed() {
        return 0;
    }

    @Override
    public void setLootTableSeed(long l) {
        // NO-OP
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        // No interacting with the non-inventory inventory
        return this.inventory;
    }

    // Specifically clear the ContainerEntity stacks.
    @Override
    public void clearItemStacks() {
        this.inventory.clear();
    }

    @Override
    public int getContainerSize() {
        return MAIN_INVENTORY_SIZE;
    }

    @Override
    public ItemStack getItem(int i) {
        return this.inventory.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        return ContainerHelper.removeItem(this.inventory, i, j);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        // "no update"? i think this means like, "remove the entire thing and don't replace it"
        var stacc = this.inventory.get(i);
        if (stacc.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.inventory.set(i, ItemStack.EMPTY);
            return stacc;
        }
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.inventory.set(i, itemStack);
    }

    @Override
    public void setChanged() {
        // uhhhh
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isChestVehicleStillValid(player);
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }

    // endregion

    public class RoombaVMImpl extends RoombaVM {
        @Nullable
        @Override
        public Peripheral getPeripheral(short index) {
            if (index < EntityRoomba.this.peripherals.size()) {
                var stack = EntityRoomba.this.peripherals.get(index);
                // may or may not be null
                // well, it *shouldn't* be null, but what do I know.
                return RoombasMod.PERIPHERALS.find(stack, null);
            }
            return null;
        }
    }
}
