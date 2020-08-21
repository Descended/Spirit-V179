from net.swordie.ms.constants import BossConstants

# Amon - (Easy/Chaos) Zakum's Altar

mapText = {
BossConstants.ZAKUM_JQ_MAP_2 : "I am impressed. Now, for the final stage.\r\nProceed, if you dare..",
BossConstants.ZAKUM_JQ_MAP_1 : "Intimidated? It's not too late to turn around."
}

optionOne = "#L0#Take me back to El Nath!!#l"
optionTwo = "#L1#I will continue.#l"

if sm.getFieldID() in mapText:
	reply = sm.sendNext(mapText[sm.getFieldID()] + "\r\n" + optionOne + "\r\n" + optionTwo)
	if reply == 0:
		if sm.sendAskYesNo("Are you sure you want to leave?\r\nYou will lose any progress you have made."):
			sm.warpNoReturn(211000000, 11) # El Nath Town
		else:
			sm.sendSayOkay("Brave choice, I commend you.")
	elif reply == 1:
		sm.sendSayOkay("I'll be here when you return.")
else:
	if sm.sendAskYesNo("Are you ready to leave? Your whole party will be warped out and will not be allowed back in."):
		sm.stopEvents()
        sm.warpNoReturn(211042300, 1)