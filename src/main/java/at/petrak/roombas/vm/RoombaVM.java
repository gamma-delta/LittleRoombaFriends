package at.petrak.roombas.vm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static at.petrak.roombas.api.RoombasModAPI.modLoc;

public abstract class RoombaVM {
    public static final byte MAX_CARD_LENGTH = 16;
    public  static final byte MAX_CARD_COUNT = 16;
    public static final short MAX_PERIPHERAL_COUNT = 4;

    public byte ip = 0;
    public byte cardIdx = 0;

    /**
     * The instructions are indexed instructions[card][row].
     *
     * Instructions are not nullable, but not all 16 slots need be filled.
     *
     * It's the implementor's job to make whatever highlight for what line is executing line up.
     */
    public List<List<Instruction>> instructions = new ArrayList<>();

    public EnumMap<Register, Short> registers;

    /**
     * The roomba VM itself defines its own modes, and it also can get modes set
     * by peripherals or implementors.
     *
     * The implementor might not even call tick
     */
    public ResourceLocation mode = modLoc("executing");
    public CompoundTag modeData = new CompoundTag();

    public abstract @Nullable Peripheral getPeripheral(short index);

    /**
     * Tick the state. If the mode is `roombas:executing`, execute the instructions,
     * otherwise passthru to peripherals.
     */
    public void tick() {
        if (this.mode.equals(modLoc("executing"))) {
            this.execute();
        } else {
            for (short i = (short) 0; i < MAX_PERIPHERAL_COUNT; i++) {
                var perph = this.getPeripheral(i);
                if (perph != null) {
                    var stop = perph.tickMode(this);
                    if (stop) break;
                }
            }
        }
    }

    /** Execute one instruction! */
    public void execute() {
        if (this.cardIdx >= this.instructions.size()) {
            this.cardIdx = 0;
        }
        var card = this.instructions.get(this.cardIdx);

        // Skip instrs with non-matching flags
        var cRegi = this.registers.get(Register.C);
        Instruction instr = null;
        for (int dip = 0; dip < card.size(); dip++) {
            var tryIP = (this.ip + dip) % card.size();
            var tryInstr = card.get(tryIP);
            ConditionalFlag flag = tryInstr.flag();
            if (flag == null || (flag == ConditionalFlag.EQU && cRegi == 0)
                || (flag == ConditionalFlag.NEG && cRegi < 0) || (flag == ConditionalFlag.POS && cRegi > 0)) {
                instr = tryInstr;
                break;
            }
        }

        if (instr == null) {
            // All the conditions failed! Wow!
            return;
        }

        short arg;
        if (instr.argument() instanceof Argument.Literal lit) {
            arg = lit.value;
        } else if (instr.argument() instanceof Argument.Register regi) {
            arg = this.registers.get(regi.register);
        } else {
            throw new IllegalStateException();
        }

        boolean incIP = true;

        switch (instr.opcode()) {
            case ADD -> {
                var a = this.registers.get(Register.A);
                this.registers.put(Register.A, saturate(a + arg));
            }
            case SUB -> {
                var a = this.registers.get(Register.A);
                this.registers.put(Register.A, saturate(a - arg));
            }
            case MUL -> {
                var a = this.registers.get(Register.A);
                this.registers.put(Register.A, saturate(a * arg));
            }
            case DVM -> {
                var a = this.registers.get(Register.A);
                var div = a / arg;
                var mod = a % arg;
                this.registers.put(Register.A, saturate(div));
                this.registers.put(Register.B, saturate(mod));
            }

            case JMP -> {
                this.ip = (byte) Mth.clamp(arg, 0, card.size());
                incIP = false;
            }
            case JBY -> {
                this.ip = (byte) Mth.clamp(this.ip + arg, 0, card.size());
                incIP = false;
            }
            case CRD -> {
                if (arg < this.instructions.size()) {
                    this.cardIdx = (byte) arg;
                }
                this.ip = 0;
                incIP = false;
            }
            case CRJ -> {
                if (arg < this.instructions.size()) {
                    this.cardIdx = (byte) arg;
                }
                incIP = false;
            }

            case LDA -> {
                this.registers.put(Register.A, saturate(arg));
            }
            case RLB -> {
                var oldA = this.registers.get(Register.A);
                this.registers.put(Register.A, saturate(arg));
                this.registers.put(Register.B, oldA);
            }
            case RLC -> {
                var oldA = this.registers.get(Register.A);
                this.registers.put(Register.A, saturate(arg));
                this.registers.put(Register.C, oldA);
            }
            case RLD -> {
                var oldA = this.registers.get(Register.A);
                this.registers.put(Register.A, saturate(arg));
                this.registers.put(Register.D, oldA);
            }

            case MOV -> {
                // Executor picks up on this, saves the current location to the tag...
                this.mode = modLoc("move/start");
                this.modeData = new CompoundTag();
                this.modeData.putShort("distance", arg);
            }
            case ROT -> {
                this.mode = modLoc("rotate/start");
                this.modeData = new CompoundTag();
                this.modeData.putShort("angle", arg);
            }
            case PHL -> {
                var phl = this.getPeripheral(arg);
                if (phl != null) {
                    incIP = phl.executePHL(this);
                }
            }
            case SLP -> {
                this.mode = modLoc("sleep/start");
                this.modeData = new CompoundTag();
                this.modeData.putShort("time", arg);
            }
        }

        if (incIP) {
            this.ip++;
        }

        if (this.cardIdx > this.instructions.size()) {
            this.cardIdx = 0;
        }
        this.ip = (byte) Math.floorMod(this.ip, this.instructions.get(this.cardIdx).size());
    }

    public static short saturate(int x) {
        return (short) Mth.clamp(x, -999, 999);
    }

    /**
     * The implementor is responsible for serializing peripherals.
     */
    public CompoundTag serialize() {
        var tag = new CompoundTag();

        tag.putByte("ip", this.ip);
        tag.putByte("cardIdx", this.cardIdx);

        var cards = new ListTag();
        for (var instrs : this.instructions) {
            var instrTag = new ListTag();
            for (Instruction instr : instrs) {
                if (instr != null) {
                    instrTag.add(instr.serialize());
                } else {
                    instrTag.add(new CompoundTag());
                }
            }
            cards.add(instrTag);
        }
        tag.put("cards", cards);

        var regis = new CompoundTag();
        for (var regi : Register.values()) {
            if (this.registers.containsKey(regi)) {
                regis.putShort(regi.name(), this.registers.get(regi));
            } else {
                regis.putShort(regi.name(), (short) 0);
            }
        }
        tag.put("registers", regis);

        tag.putString("mode", this.mode.toString());
        tag.putString("modeData", this.modeData.toString());

        return tag;
    }

    /**
     * Load and mutate self from the tag
     */
    public void deserializeLoad(CompoundTag tag) {
        this.ip = tag.getByte("ip");
        this.cardIdx = tag.getByte("cardIdx");

        this.instructions.clear();
        var cardTag = tag.getList("cards", Tag.TAG_LIST);
        for (Tag subtagAny : cardTag) {
            var subtag = (ListTag) subtagAny;
            List<Instruction> instrs = new ArrayList<>();
            for (Tag value : subtag) {
                var subsubtag = (CompoundTag) value;
                instrs.add(Instruction.deserialize(subsubtag));
            }
            this.instructions.add(instrs);
        }

        this.registers.clear();
        var regiTag = tag.getCompound("registers");
        for (Register regi : Register.values()) {
            if (regiTag.contains(regi.name())) {
               this.registers.put(regi, saturate(regiTag.getShort(regi.name())));
            } else {
                this.registers.put(regi, (short) 0);
            }
        }

        this.mode = new ResourceLocation(tag.getString("mode"));
        this.modeData = tag.getCompound("modeData");
    }
}
