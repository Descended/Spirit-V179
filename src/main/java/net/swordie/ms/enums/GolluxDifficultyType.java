package net.swordie.ms.enums;

import java.util.Arrays;

public enum GolluxDifficultyType {
    Easy(0),
    Normal(1),
    Hard(2),
    Hell(3),
    ;

    private byte val;

    GolluxDifficultyType(int val) {
        this.val = (byte) val;
    }

    public static GolluxDifficultyType getByVal(byte val) {
        return Arrays.stream(values()).filter(gdt -> gdt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
