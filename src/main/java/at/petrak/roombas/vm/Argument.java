package at.petrak.roombas.vm;

public sealed class Argument {
    public static final class Literal extends Argument {
        public final short value;
        public Literal(short value) {
            this.value = value;
        }
    }

    public static final class Register extends Argument {
        public final at.petrak.roombas.vm.Register register;
        public Register(at.petrak.roombas.vm.Register register) {
            this.register = register;
        }
    }
}
