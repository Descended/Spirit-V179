package net.swordie.ms.enums;

public enum ReactorType {
    VEIN(0),
    HERB(1),
    ;

    private byte val;

    ReactorType(int val) {
        this.val = (byte) val;
    }

    public byte getVal() {
        return val;
    }
}
