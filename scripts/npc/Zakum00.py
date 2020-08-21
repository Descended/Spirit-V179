from net.swordie.ms.constants import BossConstants

# Adobis - Door to Zakum field

response = sm.sendAskYesNo("Hey, if you want to fight Zakum, I can provide you with some sacrificial orbs.")

if response:
	if sm.canHold(BossConstants.ZAKUM_EASY_SPAWN_ITEM, 5):
		sm.giveItem(BossConstants.ZAKUM_EASY_SPAWN_ITEM, 5) # yea i know we arent checking for two slots
		sm.giveItem(BossConstants.ZAKUM_HARD_SPAWN_ITEM, 5)
	else:
		sm.sendSayOkay("Please make more space in your ETC inventory.")
else:
	sm.sendSayOkay("Okay, maybe another time.")
