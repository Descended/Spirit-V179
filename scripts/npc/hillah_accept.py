# Medina (2183003) | Azwan Refuge Zone : Azwan
from net.swordie.ms.constants import BossConstants
from net.swordie.ms.constants import GameConstants
if 262030100 <= sm.getFieldID() <= 262031300:
    if sm.sendAskYesNo("Would you like to leave the boss fight?"):
        sm.warpInstanceOut(262000000, 0)


else:
    sm.sendNext("#e<Boss:Hilla>#n \r\n \r\n Are you ready to defeat Hilla and liberate Azwan? Make sure everyone in your party is here!\r\n\r\n \r\n#L0##bRequest to enter <Boss: Hilla>#l#n")
    selection = sm.sendNext("#e<Boss:Hilla>#n \r\n \r\n Choose a mode. \r\n \r\n \r\n#L0#Normal(level 120+)#l \r\n #L1#Hard(Level 170+)#l")
    if selection == 0:
        if sm.isPartyLeader():
            for partyMembers in sm.getParty().getMembers():
                if partyMembers.getLevel() < 120:
                    sm.sendSayOkay("You have to be above level 120 to face normal Hilla")
                else:
                    sm.warpInstanceIn(262030100, True)
                    sm.setInstanceTime(BossConstants.EASY_HILLA_TIME, 262030000)
                    sm.createQuestWithQRValue(GameConstants.EASY_HILLA_QUEST, "1")
        else:
            sm.sendSayOkay("Please have your party leader speak to me.")

    elif selection == 1:
        if sm.isPartyLeader():
            for partyMembers in sm.getParty().getMembers():
                if partyMembers.getLevel() < 170:
                    sm.sendSayOkay("You have to be above level 170 to face hard Hilla")
                else:
                    sm.warpInstanceIn(262031100, True)
                    sm.setInstanceTime(BossConstants.EASY_HILLA_TIME, 262030000)
                    sm.createQuestWithQRValue(GameConstants.EASY_HILLA_QUEST, "1")
        else:
            sm.sendSayOkay("Please have your party leader speak to me.")