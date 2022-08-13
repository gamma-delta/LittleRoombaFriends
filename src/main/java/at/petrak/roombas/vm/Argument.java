package at.petrak.roombas.vm;

public sealed class Argument {
    public final class Literal extends Argument {
        public final int value;
        public Literal(int value) {
            this.value = value;
        }
    }

    public final class Register extends Argument {

    }
}
