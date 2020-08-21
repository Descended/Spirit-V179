# normal balrog enter field script
# has three parts, they are all spawned at the same time and place

from net.swordie.ms.constants import BossConstants
from time import sleep

sm.spawnBalrog(False) # True = easy

end = sm.waitForMobDeath(BossConstants.BALROG_HARD_DMGSINK) # we just need this guy to die

sm.showFieldEffect("Map/Effect.img/balog/clear/stone")
sleep(1) # let them marvel at the field effect for a sec 

sm.resetBossMap(BossConstants.BALROG_HARD_BATTLE_MAP)
sm.warpInstanceOut(BossConstants.BALROG_HARD_WIN_MAP, True)