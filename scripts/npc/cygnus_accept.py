# Mark of the Squad(2083004) | Cave of Life, Entrance to Horntail's Cave
from net.swordie.ms.constants import BossConstants
from net.swordie.ms.constants import GameConstants
if sm.getFieldID() == 271040100 or sm.getFieldID() == 211070102:
    if sm.sendAskYesNo("Would you like to leave the fight?"):
        sm.warpInstanceOut(271040000, 0)
else:
    if sm.isPartyLeader():
        sm.sendNext("#e<Boss:Cygnus Expedition>#n \r\n Are you ready to fight Empress Cygnus?#b\r\n \r\n"
                    "#L0#Request to join a Cygnus Expedition.#l\r\n")
        selection = sm.sendNext("#e<Boss:Von Leon>#n \r\n Select a mode. \r\n \r\n"
                                "#L0#Easy(Level 150+) #l \r\n"
                                "#L1#Normal (Level 175+) #l \r\n")
        if selection == 0:
            sm.warpInstanceIn(271040100, True)
            sm.setInstanceTime(BossConstants.CYGNUS_TIME)
        elif selection == 1:
            sm.warpInstanceIn(211070102, True)
            sm.setInstanceTime(BossConstants.CYGNUS_TIME)
    else:
        sm.sendSayOkay("Please have your party leader speak to me.")