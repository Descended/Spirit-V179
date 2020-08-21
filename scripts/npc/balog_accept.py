# Mu Young
# Balrog Entry

from net.swordie.ms.constants import BossConstants
from net.swordie.ms.constants import GameConstants

options = {
	0 : BossConstants.BALROG_EASY_BATTLE_MAP,
	1 : BossConstants.BALROG_HARD_BATTLE_MAP
}
if not sm.isPartyLeader():
	sm.sendSayOkay("Please have your party leader speak to me..")
elif sm.sendAskYesNo("Greetings, o weary traveller, you have arrived at the Balrog temple.\r\nDo you wish to battle the Balrog?"):
	choice = sm.sendNext("Your strength should surpass level 50 for the easy Balrog, and level 70 for the hard Balrog.\r\n#L0#Easy#l\r\n#L1#Hard#l")
	sm.sendSayOkay("Good luck, traveller.")
	sm.warpInstanceIn(options[choice], True)
	sm.setInstanceTime(BossConstants.BALROG_TIME_LIMIT, BossConstants.BALROG_ENTRY_MAP)
else:
    sm.sendSayOkay("I am disappointed, but I respect you for knowing your limitations.")