# Mark of the Squad(2083004) | Cave of Life, Entrance to Horntail's Cave
from net.swordie.ms.constants import BossConstants
from net.swordie.ms.constants import GameConstants
if sm.getFieldID() == 272020210:
    if sm.sendAskYesNo("Would you like to leave the fight?"):
        sm.warpInstanceOut(272020110, 0)
else:
    if sm.isPartyLeader():
        sm.sendNext("#e<Boss: Arkarium>#n \r\n Warriors! Are you ready to battle the Black Mage's evil commander?#b\r\n \r\n"
                    "#L0#Request to enter <Boss: Arkarium>.#l\r\n")
        selection = sm.sendNext("#e<Boss: Arkarium>#n \r\n Select a mode. \r\n \r\n"
                                "#L0#Easy(Level 150+) #l \r\n"
                                "#L1#Normal (Level 150+) #l \r\n")
        if selection == 0:
            sm.warpInstanceIn(272020210, True)
            sm.setInstanceTime(BossConstants.ARKARIUM_TIME)
            sm.createQuestWithQRValue(GameConstants.ARKARIUM_QUEST, "2")
        elif selection == 1:
            sm.warpInstanceIn(272020210, True)
            sm.setInstanceTime(BossConstants.VON_LEON_TIME)
            sm.createQuestWithQRValue(GameConstants.ARKARIUM_QUEST, "3")
    else:
        sm.sendSayOkay("Please have your party leader speak to me.")