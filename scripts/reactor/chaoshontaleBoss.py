# Chaos horntail gem

from net.swordie.ms.constants import BossConstants
import random

HORNTAIL = 8810130 # HT invis body, dies immediately and spawns all the parts

SPAWN_PX = 95
SPAWN_PY = 260

reactor.incHitCount()
reactor.increaseState()
if reactor.getHitCount() >= 4:
	sm.spawnMob(HORNTAIL, SPAWN_PX, SPAWN_PY, False)
	sm.removeReactor()