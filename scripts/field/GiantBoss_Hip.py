import time
abdomenID = 9390612;

if not sm.golluxMapAlreadyVisited():
    sm.spawnMob(abdomenID, -4, -20, False)
if not sm.hasMobsInField():
    sm.openGolluxPortal("clear1", 12)
    sm.openGolluxPortal("clear2", 12)

sm.addCurrentGolluxMap()
sm.waitForMobDeath(abdomenID)
sm.showFieldEffect("Map/EffectTW.img/arisan/clear")
sm.addClearedGolluxMap()
sm.openGolluxPortal("clear2", 12)
time.sleep(1)
sm.openGolluxPortal("clear1", 12)