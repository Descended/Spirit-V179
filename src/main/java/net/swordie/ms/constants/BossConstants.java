package net.swordie.ms.constants;


/**
 * Created on 2-8-2018.
 */
public class BossConstants {

    //  Lotus ----------------------------------------------------------------------------------------------------------
    public final static int LOTUS_MOBID = 8950000;
    public static final long[][] LOTUS_HP_PHASE_DIFFICULTY = {{400000000000L, 1600000000000L}, {400000000000L, 7000000000000L}, {700000000000L, 24000000000000L}};
    public static final int LOTUS_BOUNCING_BALL_DURATION = 20000;
    //      Obstacle Atoms
    public static final int LOTUS_OBSTACLE_ATOM_VELOCITY = 15; // Velocity at which the Obstacle Atoms fall down.

    public static final int LOTUS_BLUE_ATOM_EXECUTION_DELAY = 1500; // in ms. Delay between method executions
    public static final int LOTUS_BLUE_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_BLUE_ATOM_PROP = 30; // % chance of actually spawning in
    public static final int LOTUS_BLUE_ATOM_DAMAGE = 25; // % of Max HP

    public static final int LOTUS_YELLOW_ATOM_EXECUTION_DELAY = 1750; // in ms. Delay between method executions
    public static final int LOTUS_YELLOW_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_YELLOW_ATOM_PROP = 25; // % chance of actually spawning in
    public static final int LOTUS_YELLOW_ATOM_DAMAGE = 50; // % of Max HP

    public static final int LOTUS_PURPLE_ATOM_EXECUTION_DELAY = 2000; // in ms. Delay between method executions
    public static final int LOTUS_PURPLE_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_PURPLE_ATOM_PROP = 20; // % chance of actually spawning in
    public static final int LOTUS_PURPLE_ATOM_DAMAGE = 100; // % of Max HP
    //      Stage 3
    public static final int LOTUS_ROBOT_ATOM_EXECUTION_DELAY = 2000; // in ms. Delay between method executions
    public static final int LOTUS_ROBOT_ATOM_AMOUNT = 2; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_ROBOT_ATOM_PROP = 15; // % chance of actually spawning in
    public static final int LOTUS_ROBOT_ATOM_DAMAGE = 100; // % of Max HP

    public static final int LOTUS_CRUSHER_ATOM_EXECUTION_DELAY = 4000; // in ms. Delay between method executions
    public static final int LOTUS_CRUSHER_ATOM_AMOUNT = 1; // max amount of Atoms spawning attempts in 1 call
    public static final int LOTUS_CRUSHER_ATOM_PROP = 40; // % chance of actually spawning in
    public static final int LOTUS_CRUSHER_ATOM_DAMAGE = 100; // % of Max HP



    //  Magnus ---------------------------------------------------------------------------------------------------------

    //      General
    public static final int MAGNUS_TIME = 20  *60; // 20 minutes
    public static final int MAGNUS_DEATHCOUNT = 20; // 20 death count


    //      Obstacle Atoms
    public static final int MAGNUS_OBSTACLE_ATOM_VELOCITY = 5; // Velocity at which the Obstacle Atoms fall down.

    public static final int MAGNUS_GREEN_ATOM_EXECUTION_DELAY = 1000; // in ms. Delay between method executions
    public static final int MAGNUS_GREEN_ATOM_AMOUNT = 4; // max amount of Atoms spawning attempts in 1 call
    public static final int MAGNUS_GREEN_ATOM_PROP = 35; // % chance of actually spawning in
    public static final int MAGNUS_GREEN_ATOM_DAMAGE = 25; // % of Max HP

    public static final int MAGNUS_BLUE_ATOM_EXECUTION_DELAY = 750; // in ms. Delay between method executions
    public static final int MAGNUS_BLUE_ATOM_AMOUNT = 4; // max amount of Atoms spawning attempts in 1 call
    public static final int MAGNUS_BLUE_ATOM_PROP = 30; // % chance of actually spawning in
    public static final int MAGNUS_BLUE_ATOM_DAMAGE = 50; // % of Max HP

    public static final int MAGNUS_PURPLE_ATOM_EXECUTION_DELAY = 2000; // in ms. Delay between method executions
    public static final int MAGNUS_PURPLE_ATOM_AMOUNT = 3; // max amount of Atoms spawning attempts in 1 call
    public static final int MAGNUS_PURPLE_ATOM_PROP = 25; // % chance of actually spawning in
    public static final int MAGNUS_PURPLE_ATOM_DAMAGE = 100; // % of Max HP


    //  Horntail -------------------------------------------------------------------------------------------------------

    //      General
    public static final int EASY_HORNTAIL_TIME = 75 * 60; // 1 hr, 15 min timer
    public static final int CHAOS_HORNTAIL_TIME = 150 * 60; // 2 hrs, 30 min timer

//  Hilla --------------------------------------------------------------------------------------------------------------

    //      General
    public static final int EASY_HILLA_TIME = 30 * 60; // 1 hr, 15 min timer
    public static final int NORMAL_HILLA_TIME = 150 * 60; // 2 hrs, 30 min timer

//  Von Leon -----------------------------------------------------------------------------------------------------------
    public static final int VON_LEON_TIME = 30 * 60; // 30 min timer

//  Cygnus -------------------------------------------------------------------------------------------------------------
    public static final int CYGNUS_TIME = 30 * 60; // 30 min timer

//  Cygnus -------------------------------------------------------------------------------------------------------------
    public static final int ARKARIUM_TIME = 30 * 60; // 30 min timer



//  Demian -------------------------------------------------------------------------------------------------------------
    public static final int BRAND_OF_SACRIFICE = 80001974; // Skill ID
    public static final long DEMIAN_HP = 840000000000L;
    public static final int DEMIAN_NORMAL_TEMPLATE_ID = 8880110;

    // Sword
    public static final int DEMIAN_SWORD_VELOCITY = 30; // default velocity
    public static final int DEMIAN_SWORD_TARGETING_VELOCITY = 60; // default velocity when targeting

    // Stigma
    public static final int DEMIAN_MAX_STIGMA = 7; // max stigma
    public static final int DEMIAN_PASSIVE_STIGMA_TIME = 30 * 1000; // Every 30 seconds, users are hit with +1 stigma
    public static final int DEMIAN_STIGMA_INCINERATE_OBJECT_RESPAWN_TIME = 20 * 1000; // Stigma Pillar spawns every 20seconds
    public static final int DEMIAN_STIGMA_INCINERATE_OBJECT_DURATION_TIME = 10 * 1000; // Stigma Pillar lasts 10 seconds
    //  Gollux ---------------------------------------------------------------------------------------------------------
    public static final int[][] GOLLUX_HP_MULTIPLIERS = {{1, 60, 300, 500}, {1, 10, 150, 3000}, {1, 10, 300, 6000}};
    public static final int GOLLUX_FIRST_MAP = 863010100;
    public static final int GOLLUX_RIGHT_SHOULDER = 863010330;
    public static final int GOLLUX_LEFT_SHOULDER = 863010430;
    public static final int GOLLUX_ABDOMEN = 863010240;
    public static final int[] GOLLUX_RIGHT_HAND_SKILLS = new int[]{3, 5, 6, 8, 10};
    public static final int[] GOLLUX_LEFT_HAND_SKILLS = new int[]{2, 4, 7, 9, 11};
    public static final int GOLLUX_BREATH_ATTACK = 1;
    public static final int GOLLUX_DROP_STONE_CHANCE = 25;
    public static final int[] GOLLUX_MAPS = new int[]{863010100, 863010200, 863010210, 863010220, 863010230, 863010240, 863010300, 863010310, 863010320, 863010330, 863010400, 863010410, 863010420, 863010430, 863010500, 863010600};

    // Balrog
    public static final int BALROG_ENTRY_MAP = 105100100; // lobby map
    public static final short BALROG_TIME_LIMIT = 20 * 60; // 20 min
    public static final short BALROG_SPAWN_X = 412;
    public static final short BALROG_SPAWN_Y = 258;

    public static final int BALROG_EASY_DMGSINK = 8830010; // has the hp bar, dmg gets splashed to this from all the below mobs
    public static final int BALROG_EASY_BODY = 8830007;
    public static final int BALROG_EASY_LARM = 8830008;
    public static final int BALROG_EASY_RARM = 8830009;
    public static final int BALROG_EASY_BATTLE_MAP = 105100400;
    public static final int BALROG_EASY_WIN_MAP = 105100401;

    public static final int BALROG_HARD_DMGSINK = 8830003;
    public static final int BALROG_HARD_BODY = 8830000;
    public static final int BALROG_HARD_LARM = 8830001;
    public static final int BALROG_HARD_RARM = 8830002;
    public static final int BALROG_HARD_BATTLE_MAP = 105100300;
    public static final int BALROG_HARD_WIN_MAP = 105100301;

    // Zakum
    public static final int ZAKUM_JQ_MAP_1 = 280020000; // first  stage of JQ
    public static final int ZAKUM_JQ_MAP_2 = 280020001; // second and final stage of JQ
    public static final int ZAKUM_DOOR_TO_ENTRANCE = 211042300; // jq warps to this corridor map, npc exists in this map that warps to entrances based on difficulty
    public static final int ZAKUM_TIME_BASE = 15 * 60; // 15 min * difficulty (15 easy, 30 normal, 45 chaos)

    public static final int ZAKUM_SPAWN_X = -54;
    public static final int ZAKUM_SPAWN_Y = 86;

    public static final int ZAKUM_EASY_SPAWN_ITEM = 4001796; // Eye of fire chunk
    public static final int ZAKUM_EASY_ENTRANCE = 211042402;
    public static final int ZAKUM_EASY_ALTAR = 280030200;
    public static final int ZAKUM_EASY_BODY = 8800020;
    public static final int ZAKUM_EASY_ARM = 8800023;

    public static final int ZAKUM_HARD_SPAWN_ITEM = 4001017; // Eye of fire
    public static final int ZAKUM_HARD_ENTRANCE = 211042400;
    public static final int ZAKUM_HARD_ALTAR = 280030100;
    public static final int ZAKUM_HARD_BODY = 8800000;
    public static final int ZAKUM_HARD_ARM = 8800003;

    public static final int ZAKUM_CHAOS_SPAWN_ITEM = 4001017; // Eye of fire
    public static final int ZAKUM_CHAOS_ENTRANCE = 211042401;
    public static final int ZAKUM_CHAOS_ALTAR = 280030000;
    public static final int ZAKUM_CHAOS_BODY = 8800100;
    public static final int ZAKUM_CHAOS_ARM = 8800103;

    //Von Bon
    public static final String [] VON_BON_PORTAL_NAMES = {"Pt01", "Pt02", "Pt04gate", "Pt05gate", "Pt06gate", "Pt08gate", "Pt09gate", "Pt03gate", "Pt07gate"};

    // TODO More bosses to be noted down...
}
