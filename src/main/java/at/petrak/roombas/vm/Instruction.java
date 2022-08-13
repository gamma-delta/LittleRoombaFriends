package at.petrak.roombas.vm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/**
 * Compiled bundle of opcode + conditional + argument.
 */
public record Instruction(Opcode opcode, Argument argument, @Nullable ConditionalFlag flag) {
    public CompoundTag serialize() {
        var tag = new CompoundTag();

        tag.putString("opcode", opcode.name());

        if (argument instanceof Argument.Literal lit) {
            tag.putShort("argument", lit.value);
        } else if (argument instanceof Argument.Register regi) {
            tag.putString("argument", regi.register.toString());
        }

        if (flag != null) {
            byte b = switch (flag) {
                case NEG -> -1;
                case EQU -> 0;
                case POS -> 1;
            };
            tag.putByte("flag", b);
        }

        return tag;
    }

    public static Instruction deserialize(CompoundTag tag) {
            var opcode = Opcode.valueOf(tag.getString("opcode"));

            Argument argument;
            var argTag = tag.get("argument");
            if (argTag instanceof StringTag stag) {
                argument = new Argument.Register(Register.valueOf(argTag.getAsString()));
            } else if (argTag instanceof ShortTag shtag) {
                argument = new Argument.Literal(shtag.getAsShort());
            } else {
                // really?
                argument = new Argument.Literal((short) 0);
            }

            ConditionalFlag flag = null;
            if (tag.contains("flag", Tag.TAG_BYTE)) {
                flag = switch (tag.getByte("flag")) {
                    case -1 -> ConditionalFlag.NEG;
                    case 1 -> ConditionalFlag.POS;
                    default -> ConditionalFlag.EQU;
                };
            }

            return new Instruction(opcode, argument, flag);
    }
}
