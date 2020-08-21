# pink bean exit npc

fid = sm.getFieldID()

if fid == 270050100 or fid == 270051100: # normal/chaos battle maps
	if sm.sendAskYesNo("Are you sure you want to leave?\r\nYou will not be able to return."):
		sm.warpNoReturn(270050300, 2)
else:
	sm.sendSayOkay("The boss squad is about to start.\r\nIf you wish to leave, the portal at the other end of the corridor will take you out.")