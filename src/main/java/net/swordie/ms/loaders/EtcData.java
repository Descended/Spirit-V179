package net.swordie.ms.loaders;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.items.ItemOption;
import net.swordie.ms.client.character.items.SetEffect;
import net.swordie.ms.enums.ScrollStat;
import net.swordie.ms.loaders.containerclasses.AndroidInfo;
import net.swordie.ms.util.Loader;
import net.swordie.ms.util.Saver;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;
import net.swordie.ms.util.container.Tuple;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EtcData {

    private static final Logger log = Logger.getLogger(EtcData.class);
    private static final Map<Integer, Integer> familiarSkills = new HashMap<>();
    private static final Map<Integer, SetEffect> setEffects = new HashMap<>();
    private static final Map<Integer, Integer> characterCards = new HashMap<>();
    private static Map<Integer, AndroidInfo> androidInfo = new HashMap<>();

    private static final String SCROLL_STAT_ID = "1";
    private static final String ITEM_OPTION_ID = "2";

    public static void loadAndroidsFromWz() {
        String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/Android";
        File dir = new File(wzDir);
        for (File file : dir.listFiles()) {
            AndroidInfo ai = new AndroidInfo(Integer.parseInt(file.getName().replace(".img.xml", "")));
            Node node = XMLApi.getAllChildren(XMLApi.getRoot(file)).get(0);
            List<Node> nodes = XMLApi.getAllChildren(node);
            for (Node mainNode : nodes) {
                String mainName = XMLApi.getNamedAttribute(mainNode, "name");
                switch (mainName) {
                    case "costume":
                        for (Node n : XMLApi.getAllChildren(mainNode)) {
                            String nName = XMLApi.getNamedAttribute(n, "name");
                            switch (nName) {
                                case "face":
                                    for (Node inner : XMLApi.getAllChildren(n)) {
                                        ai.addFace(Integer.parseInt(XMLApi.getNamedAttribute(inner, "value")) % 10000);
                                    }
                                    break;
                                case "hair":
                                    for (Node inner : XMLApi.getAllChildren(n)) {
                                        ai.addHair(Integer.parseInt(XMLApi.getNamedAttribute(inner, "value")) % 10000);
                                    }
                                    break;
                                case "skin":
                                    for (Node inner : XMLApi.getAllChildren(n)) {
                                        ai.addSkin(Integer.parseInt(XMLApi.getNamedAttribute(inner, "value")) % 1000);
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
            androidInfo.put(ai.getId(), ai);
        }
    }

    public static void loadSetEffectsFromWz() {
        //String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/SetItemInfo";
        //Node root = XMLApi.getRoot(new File(wzDir));
        File file = new File(String.format("%s/Etc.wz/SetItemInfo.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int setId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            Node effectNode = XMLApi.getFirstChildByNameBF(node, "Effect");
            List<Node> nodes1 = XMLApi.getAllChildren(effectNode);
            for (Node node1 : nodes1) {
                int level = Integer.parseInt(XMLApi.getNamedAttribute(node1, "name"));
                SetEffect setEffect = setEffects.getOrDefault(setId, new SetEffect());
                List<Node> nodes2 = XMLApi.getAllChildren(node1);
                for (Node node2 : nodes2) {
                    String ssName = XMLApi.getNamedAttribute(node2, "name");
                    ScrollStat stat = ScrollStat.getScrollStatByString(ssName);
                    if (!ssName.equals("Option") && stat != null) {
                        int statAmount = Integer.parseInt(XMLApi.getNamedAttribute(node2, "value"));
                        setEffect.addScrollStat(level, stat, statAmount);
                    } else if (ssName.equals("Option")) {
                        List<Node> nodes3 = XMLApi.getAllChildren(node2);
                        for (Node node3 : nodes3) {
                            Node optionLevel = XMLApi.getFirstChildByNameDF(node3, "level");
                            Node option = XMLApi.getFirstChildByNameDF(node3, "option");
                            ItemOption io = new ItemOption();
                            io.setId(Integer.parseInt(XMLApi.getNamedAttribute(option, "value")));
                            io.setReqLevel(Integer.parseInt(XMLApi.getNamedAttribute(optionLevel, "value")));
                            setEffect.addOption(level, io);
                        }
                    }
                }
                setEffects.put(setId, setEffect);
            }
        }
    }

    public static void loadCharacterCardsFromWz() {
        File file = new File(String.format("%s/Etc.wz/CharacterCard.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node firstNode = XMLApi.getAllChildren(root).get(0);
        Node mainNode = XMLApi.getFirstChildByNameBF(firstNode, "Card");
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int jobId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name")) * 10;
            Node skillNode = XMLApi.getFirstChildByNameBF(firstNode, "skillID");
            int skillId = Integer.parseInt(XMLApi.getNamedAttribute(skillNode, "value"));
            characterCards.put(jobId, skillId);
        }
    }

    public static void saveCharacterCards(String dir) {
        Util.makeDirIfAbsent(dir);
        try  {
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/charactercards.dat")));
            dataOutputStream.writeInt(characterCards.size());
            for (Map.Entry<Integer, Integer> entry : characterCards.entrySet()) {
                dataOutputStream.writeInt(entry.getKey());
                dataOutputStream.writeInt(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void saveSetEffects(String dir) {
        Util.makeDirIfAbsent(dir);
        for (Map.Entry<Integer, SetEffect> entry : setEffects.entrySet()) {
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/" + entry.getKey() + ".dat")))) {
                dataOutputStream.writeShort(entry.getValue().getEffectsToLevel().size()); //
                for (Map.Entry<Integer, List<Object>> level : entry.getValue().getEffectsToLevel().entrySet()) {
                    dataOutputStream.writeInt(level.getKey()); //levels non consistent
                    dataOutputStream.writeShort(level.getValue().size());
                    for (Object stat : level.getValue()) {
                        if (stat instanceof Tuple) {
                            dataOutputStream.writeUTF(SCROLL_STAT_ID);
                            dataOutputStream.writeUTF(((Tuple) stat).getLeft().toString());
                            dataOutputStream.writeInt(Integer.parseInt(((Tuple) stat).getRight().toString()));
                        } else if (stat instanceof ItemOption) {
                            dataOutputStream.writeUTF(ITEM_OPTION_ID);
                            dataOutputStream.writeInt(((ItemOption) stat).getId());
                            dataOutputStream.writeInt(((ItemOption) stat).getReqLevel());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadCharacterCards(String dir) {
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(dir))) {
            short size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                int jobId = dataInputStream.readInt();
                int skillId = dataInputStream.readInt();
                characterCards.put(jobId, skillId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getCharacterCardSkillByJob(int jobId) {
        if (characterCards.isEmpty()) {
            loadCharacterCards(String.format("%s/etc/%d.dat", ServerConstants.DAT_DIR, "charactercards"));
        }
        return characterCards.getOrDefault(jobId,0);
    }

    public static SetEffect loadSetEffectByFile(String file) {
        SetEffect setEffect = new SetEffect();
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            short levelSize = dataInputStream.readShort();
            for (int i = 0; i < levelSize; i++) {
                int level = dataInputStream.readInt();
                short statSize = dataInputStream.readShort();
                for (int j = 0; j < statSize; j++) {
                    String type = dataInputStream.readUTF();
                    if (type.equals(SCROLL_STAT_ID)) {
                        Tuple<ScrollStat, Integer> ss = new Tuple<>(ScrollStat.getScrollStatByString(dataInputStream.readUTF()), dataInputStream.readInt());
                        setEffect.addScrollStat(level, ss.getLeft(), ss.getRight());
                    } else if (type.equals(ITEM_OPTION_ID)) {
                        ItemOption io = new ItemOption();
                        io.setId(dataInputStream.readInt());
                        io.setReqLevel(dataInputStream.readInt());
                        setEffect.addOption(level, io);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return setEffect;
    }

    public static SetEffect getSetEffectInfoById(int setID) {
        if (setEffects.containsKey(setID)) {
            return setEffects.get(setID);
        }
        return loadSetEffectByFile(String.format("%s/etc/setEffects/%d.dat", ServerConstants.DAT_DIR, setID));
    }

    public static void saveAndroidInfo(String dir) {
        Util.makeDirIfAbsent(dir);
        for (AndroidInfo ai : androidInfo.values()) {
            File file = new File(String.format("%s/%d.dat", dir, ai.getId()));
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(ai.getId());
                dos.writeInt(ai.getHairs().size());
                for (int hair : ai.getHairs()) {
                    dos.writeInt(hair);
                }
                dos.writeInt(ai.getFaces().size());
                for (int face : ai.getFaces()) {
                    dos.writeInt(face);
                }
                dos.writeInt(ai.getSkins().size());
                for (int skin : ai.getSkins()) {
                    dos.writeInt(skin);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static AndroidInfo getAndroidInfoById(int androidId) {
        if (androidInfo.containsKey(androidId)) {
            return androidInfo.get(androidId);
        }
        return loadAndroidInfoFromFile(String.format("%s/etc/android/%d.dat", ServerConstants.DAT_DIR, androidId));
    }

    private static AndroidInfo loadAndroidInfoFromFile(String file) {
        AndroidInfo ai = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            ai = new AndroidInfo(dis.readInt());
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                ai.addHair(dis.readInt());
            }
            size = dis.readInt();
            for (int i = 0; i < size; i++) {
                ai.addFace(dis.readInt());
            }
            size = dis.readInt();
            for (int i = 0; i < size; i++) {
                ai.addSkin(dis.readInt());
            }
            androidInfo.put(ai.getId(), ai);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ai;
    }

    public static void generateDatFiles() {
        log.info("Started generating etc data.");
        Util.makeDirIfAbsent(ServerConstants.DAT_DIR + "/etc");
        long start = System.currentTimeMillis();
        loadAndroidsFromWz();
        saveAndroidInfo(ServerConstants.DAT_DIR + "/etc/android");
        loadSetEffectsFromWz();
        saveSetEffects(ServerConstants.DAT_DIR + "/etc/setEffects");
        loadCharacterCardsFromWz();
        saveCharacterCards(ServerConstants.DAT_DIR + "/etc");
        log.info(String.format("Completed generating etc data in %dms.", System.currentTimeMillis() - start));
    }

    public static void clear() {
        androidInfo.clear();
    }

    public static void main(String[] args) {
        generateDatFiles();
    }

    private static void loadFamiliarSkillsFromWz() {
        File file = new File(String.format("%s/Etc.wz/FamiliarInfo.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int id = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            Node skillIDNode = XMLApi.getFirstChildByNameBF(node, "passive");
            if (skillIDNode != null) {
                int skillID = Integer.parseInt(XMLApi.getNamedAttribute(skillIDNode, "value"));
                familiarSkills.put(id, skillID);
            }
        }
    }


    @Saver(varName = "familiarSkills")
    private static void saveFamiliarSkills(File file) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(familiarSkills.size());
            for (Map.Entry<Integer, Integer> entry : familiarSkills.entrySet()) {
                dos.writeInt(entry.getKey()); // familiar ID
                dos.writeInt(entry.getValue()); // skill ID
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Loader(varName = "familiarSkills")
    public static void loadFamiliarSkills(File file, boolean exists) {
        if (!exists) {
            loadFamiliarSkillsFromWz();
            saveFamiliarSkills(file);
        } else {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                int gradeSkillSize = dis.readInt();
                for (int j = 0; j < gradeSkillSize; j++) {
                    int familiarID = dis.readInt();
                    int skillID = dis.readInt();
                    familiarSkills.put(familiarID, skillID);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getSkillByFamiliarID(int familiarID) {
        return familiarSkills.get(familiarID);
    }
}
