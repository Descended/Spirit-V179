package net.swordie.ms.world.field;

import net.swordie.ms.util.Util;

import java.awt.*;
import java.util.Arrays;

public enum AreaBossInfo {
    Mano(2220000, 104010200, new Point(268, 515), AreaBossInfo.BASE_CD_MIN, 0), // port road: forest trail 2
    Mushmom(2600208, 100020101, new Point(-858, 215), AreaBossInfo.BASE_CD_MIN, 0), // mushmom forest trail
    BlueMushmom(8220007, 100020301, new Point(162, -685), AreaBossInfo.BASE_CD_MIN, 0), // blue mushmom forest 2
    ZombieMushmom(6300005, 105010301, new Point(749, 1458), AreaBossInfo.BASE_CD_MIN, 0), // ant tunnel square 2
    Stumpy(3220000, 102020500, new Point(1448, 2145), AreaBossInfo.BASE_CD_MIN, 0), // gusty peak perion
    Dyle(6220000, 103030400, new Point(169, 118), AreaBossInfo.BASE_CD_MIN, 0), // deep mire
    CptDarkgoo(2600222, 914200000, new Point(-164, -169), AreaBossInfo.BASE_CD_MIN, 0), // shady beach
    JrBalrog(2600231, 690000071, new Point(314, 130), AreaBossInfo.BASE_CD_MIN, 0),
    SnackBar(8220008, 105020401, new Point(1038, 395), AreaBossInfo.BASE_CD_MIN, 0), // spawns 8220009 after death, drops need to be assigned to the later
    Eliza(8220000, 200010302, new Point(-95, 83), AreaBossInfo.BASE_CD_MIN, 0), // garden of darkness II
    SnowWitch(6090001, 211050000, new Point(1079, 34), AreaBossInfo.BASE_CD_MIN, 0), // icy cold field
    Snowman(8220001, 211040500, new Point(-66, -466), AreaBossInfo.BASE_CD_MIN, 0), // sharp cliff 3, el nath
    PrisonGuardAni(8210013, 211061100, new Point(809, -215), AreaBossInfo.BASE_CD_MIN, 0), // ani's jail, has a map script but idc about it cuz we running off this now
    Seruf(4220001, 230020101, new Point(-277, 580), AreaBossInfo.BASE_CD_MIN, 0), // spawns 4220000 after death, any drops need to be assigned to the later
    KingRombot(9300299, 221020701, new Point(-2, 1513), AreaBossInfo.BASE_CD_MIN, 0), // hidden room off ludi tower 4th floor
    Timer(2600613, 220050200, new Point(-1, 1032), AreaBossInfo.BASE_CD_MIN, 0), // lost time <2>
    TargaScarboss(-1, 223020210, new Point(), AreaBossInfo.BASE_CD_MIN * 2, 0), // this one uses a map script so the point and mob id dont matter -> we only check the timer against the map id
    MasterDummy(5090001, 250020300, new Point(1113, -944), AreaBossInfo.BASE_CD_MIN, 0), // mu lung - practice field advanced level
    TaeRoon(7220000, 250010304, new Point(-518, 393), AreaBossInfo.BASE_CD_MIN, 0),
    BambooWarrior(6090002, 251010101, new Point(362, 123), AreaBossInfo.BASE_CD_MIN, 0),
    SnowFro(-1, 925120000, new Point(), AreaBossInfo.BASE_CD_MIN, 0), // also handled by a map script so the point isnt used
    Ravana(9100024, 252030100, new Point(-920, -255), AreaBossInfo.BASE_CD_MIN, 0),
    SecurityCamera(2600509, 261020300, new Point(3, 167), AreaBossInfo.BASE_CD_MIN, 0), // Lab - Area C-1
    GhostwoodStumpy(8620012, 273020400, new Point(1148, 375), AreaBossInfo.BASE_CD_MIN, 0), // twilight perion - tempest grave
    ;

    public static final int BASE_CD_MIN = 4 * 60; // 4 hours

    private int mobId, fieldId, respawnTimeMin;
    private long health;
    private Point p;

    AreaBossInfo(int mobid, int fieldid, Point point, int respawntimemin, long hp) {
        mobId = mobid;
        fieldId = fieldid;
        p = point;
        respawnTimeMin = respawntimemin;
        health = hp;
    }

    public int getBossID() {
        return mobId;
    }

    public int getFieldID() {
        return fieldId;
    }

    public int getRespawnTimeMin() {
        return respawnTimeMin;
    }

    public Point getSpawnPoint() {
        return p;
    }

    public long getHealth() {
        return health;
    }

    public static AreaBossInfo getByFieldId(int fieldid) {
        return Util.findWithPred(Arrays.asList(values()), objs -> objs.getFieldID() == fieldid);
    }

    public static AreaBossInfo getByBossId(int bossid) {
        return Util.findWithPred(Arrays.asList(values()), objs -> objs.getBossID() == bossid);
    }

    public static boolean isAreaBoss(int mobId) {
        return getByBossId(mobId) != null;
    }
}
