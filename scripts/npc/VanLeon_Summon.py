# Von Leon : Lion King
# GMS-like
# https://www.youtube.com/watch?v=zT86z7pRnkQ

VON_LEON_NPC = 2161000
SPAWNX = 28
SPAWNY = -181

dic = { 
	211070100 : 8840013,
	211070102 : 8840010
}

if sm.sendAskYesNo("Are you the warriors who came to defeat me? Or are you from the Anti Black Mage Alliance? It doesn't matter who you are ...There's no need for chitchatting if we are sure about each other's purpose...\r\nBring it on, you fools!"):
	sm.removeNpc(VON_LEON_NPC)
	sm.spawnMob(dic[sm.getFieldID()], SPAWNX, SPAWNY, False)
	
	for c in sm.getPartyMembersInSameField(chr):
		c.completeQuest(3173) # Queen Ifia's quest -> needed to proceed to Ani's Jail