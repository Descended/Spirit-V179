# Lion King's Castle | Audience Room Corridor
# Entry Portal to Von Leon boss quest

from net.swordie.ms.constants import BossConstants
from net.swordie.ms.constants import GameConstants

maps = [211070100, 211070102]

WARP_OUT = 211070000

if sm.getFieldID() in maps:
    if sm.sendAskYesNo("Would you like to leave the fight?"):
        sm.warpInstanceOut(WARP_OUT, 2)
elif sm.isPartyLeader():
	sm.sendNext("#e<Boss:Von Leon>#n \r\n Are you brave enough to face Von Leon?#b\r\n \r\n"
				"#L0#Apply to enter Von Leon expedition.#l\r\n")
	selection = sm.sendNext("#e<Boss:Von Leon>#n \r\n Select a mode. \r\n \r\n"
							"#L0#Easy(Level 125+) #l \r\n"
							"#L1#Normal (Level 130+) #l \r\n")
	sm.warpInstanceIn(maps[selection], 3, True)
	sm.setInstanceTime(BossConstants.VON_LEON_TIME, WARP_OUT, 2)
else:
	sm.sendSayOkay("Please have your party leader speak to me.")