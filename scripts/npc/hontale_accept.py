# Mark of the Squad(2083004) | Cave of Life, Entrance to Horntail's Cave
from net.swordie.ms.constants import BossConstants
from net.swordie.ms.constants import GameConstants

dict = {
    0 : 240060000,
    1 : 240060002,
    2 : 240060001
}

if sm.isPartyLeader():
    sm.sendNext("#e<Boss:Horntail>#n \r\n Horntail has resurrected. We must stop it before it erupts a volcano and turns all of Minar into hell on earth.#b\r\n \r\n"
            "#L0#Apply to enter <Boss:Horntail>.#l\r\n")
    selection = sm.sendNext("#e<Boss:Horntail>#n \r\n Select a mode. \r\n \r\n"
                "#L0#Easy(Level 130+) #l \r\n"
                "#L1#Normal (Level 130+) #l \r\n"
                "#L2#Chaos(Level 135+) #l \r\n")
    sm.warpInstanceIn(dict[selection], True)
    sm.setInstanceTime(BossConstants.EASY_HORNTAIL_TIME)
    sm.createQuestWithQRValue(GameConstants.EASY_HORNTAIL_QUEST, "1")
    sm.setPartyQRValue(GameConstants.EASY_HORNTAIL_QUEST, "1")
else:
    sm.sendSayOkay("Please have your party leader speak to me.")