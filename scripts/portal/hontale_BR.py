# horntail - Cave of Life - Cave of trial 1
from net.swordie.ms.constants import GameConstants

maps = {
	240060000 : 240060100,
	240060100 : 240060300,
	240060002 : 240060102,
	240060102 : 240060200,
	240060001 : 240060101,
	240060101 : 240060201
}

QR = int(sm.getQRValue(GameConstants.EASY_HORNTAIL_QUEST))

if not sm.isPartyLeader():
	sm.systemMessage("Only your party leader may proceed to the next room..")

elif sm.getChr().getField().getMobs().size() == 0:
    if QR == 4:
        sm.setPartyDeathCount(10) # set death count for big boi
    sm.setPartyQRValue(GameConstants.EASY_HORNTAIL_QUEST, str(QR + 1))
    sm.invokeForParty("warp", maps[sm.getFieldID()], 0)
else:
	sm.chat("Please eliminate all monsters")