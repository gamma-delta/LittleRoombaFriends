package at.petrak.roombas.vm;

public interface Peripheral {
    /**
     * Do whatever it is the peripheral does to the roomba.
     *
     * Return if the IP should be incremented or not.
     */
    boolean executePHL(RoombaVM roomba);

    /**
     * If the roomba is in a mode it doesn't recognize internally, it will forward to each of its peripherals.
     * Return true from this to indicate that this is the peripheral that handles it.
     */
    boolean tickMode(RoombaVM roomba);
}
