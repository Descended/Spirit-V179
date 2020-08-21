# reward reactor in easy balrog win map

from net.swordie.ms.constants import BossConstants

# TODO drops lol
DROPLIST = {
	BossConstants.BALROG_EASY_WIN_MAP : [4001261, 4001261, 4001261], 
	BossConstants.BALROG_HARD_WIN_MAP : [4001261, 4001261, 4001261]
}

RANGE = 300
PX = 398 # beginning position X
PY = 258 # beginning position Y
DELAY = 250 # milliseconds

reactor.incHitCount()
reactor.increaseState()
if reactor.getHitCount() >= 4:
	sm.dropItemsAlongLine(DROPLIST[chr.getFieldID()], RANGE, PX, PY, DELAY)
	sm.removeReactor()