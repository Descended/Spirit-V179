leftShoulderID = 9390611

sm.openGolluxPortal("phase3", 12)
if not sm.golluxMapAlreadyVisited():
    sm.spawnMob(leftShoulderID, 85, 0, False)
if not sm.hasMobsInField():
    sm.openGolluxPortal("clear", 7)
    sm.openGolluxPortal("phase3", 12)
sm.addCurrentGolluxMap()
sm.waitForMobDeath(leftShoulderID)
sm.showFieldEffect("Map/EffectTW.img/arisan/clear")
sm.addClearedGolluxMap()
sm.openGolluxPortal("clear", 7)