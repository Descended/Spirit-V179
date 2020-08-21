package net.swordie.ms.client.character.keys;

import net.swordie.ms.connection.OutPacket;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 1/4/2018.
 */
@Entity
@Table(name = "funckeymap")
public class FuncKeyMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "fkMapId")
    private List<Keymapping> keymap = new ArrayList<>();

    @Transient
    private static final int MAX_KEYBINDS = 89;


    public FuncKeyMap() {

    }

    public List<Keymapping> getKeymap() {
        return keymap;
    }

    public void setKeymap(List<Keymapping> keymap) {
        this.keymap = keymap;
    }

    public Keymapping getMappingAt(int index) {
        for (Keymapping km : getKeymap()) {
            if (km.getIndex() == index) {
                return km;
            }
        }
        return null;
    }

    public void encode(OutPacket outPacket) {
        for (int i = 0; i < MAX_KEYBINDS; i++) {
            Keymapping tuple = getMappingAt(i);
            if (tuple == null) {
                outPacket.encodeByte(0);
                outPacket.encodeInt(0);
            } else {
                outPacket.encodeByte(tuple.getType());
                outPacket.encodeInt(tuple.getVal());
            }
        }
    }

    public void putKeyBinding(int index, byte type, int value) {
        Keymapping km = getMappingAt(index);
        if (km == null) {
            km = new Keymapping();
            km.setIndex(index);
            km.setType(type);
            km.setVal(value);
            getKeymap().add(km);
        } else {
            km.setType(type);
            km.setVal(value);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static FuncKeyMap getDefaultMapping(int keySettingType) {
        FuncKeyMap fkm = new FuncKeyMap();
        int[] array1;
        int[] array2;
        int[] array3;

        if (keySettingType == 0) {
            array1 = new int[]{ 2, 3, 64, 4, 65, 5, 6, 7, 8, 13, 17, 16, 19, 18, 21, 20, 23, 22, 25, 24, 27, 26, 29, 31, 34, 35, 33, 38, 39, 37, 43, 40, 41, 46, 47, 44, 45, 51, 50, 49, 48, 59, 57, 56, 63, 62, 61, 60};
            array2 = new int[]{ 4, 4, 6, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 6, 5, 5, 6, 6, 6, 6};
            array3 = new int[]{10, 12, 105, 13, 106, 18, 24, 21, 29, 33, 5, 8, 4, 0, 31, 28, 1, 34, 19, 25, 15, 14, 52, 2, 17, 11, 26, 20, 27, 3, 9, 16, 23, 6, 32, 50, 51, 35, 7, 22, 30, 100, 54, 53, 104, 103, 102, 101};
        }
        else {
            array1 = new int[]{12, 20, 21, 22, 23, 25, 26, 27, 29, 34, 35, 36, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 49, 50, 52, 56, 57, 71, 73, 79, 82, 83};
            array2 = new int[]{ 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4};
            array3 = new int[]{36, 27, 30, 0, 1, 19, 14, 15, 52, 17, 11, 8, 3, 20, 26, 16, 22, 9, 50, 51, 2, 31, 29, 5, 7, 4, 53, 54, 12, 13, 23, 10, 18};
        }
        for (int i = 0; i < array1.length; i++) {
            fkm.putKeyBinding(array1[i], (byte) array2[i], array3[i]);
        }
        return fkm;
    }
}
