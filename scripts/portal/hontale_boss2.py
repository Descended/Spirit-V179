# horntail entrace - The Cave of Trial 1
from net.swordie.ms.constants import GameConstants

mobs = {
	240060100 : 8810213,
	240060102 : 8810025,
	240060101 : 8810129
}

if int(sm.getQRValue(GameConstants.EASY_HORNTAIL_QUEST)) != 4:
    sm.spawnMob(mobs[sm.getFieldID()], -317, 230, False) # does the spawn animation
	
    sm.setPartyQRValue(GameConstants.EASY_HORNTAIL_QUEST, "4")