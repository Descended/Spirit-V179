package net.swordie.ms.enums;

import java.util.Arrays;

public enum BeastTamerBeasts {
    None(0),
    Bear(1),
    Leopard(2),
    Bird(3),
    Cat(4),
    ;

    private byte val;

    BeastTamerBeasts(int val) {
        this.val = (byte) val;
    }

    public static BeastTamerBeasts getByVal(byte val) {
        return Arrays.stream(values()).filter(gdt -> gdt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
