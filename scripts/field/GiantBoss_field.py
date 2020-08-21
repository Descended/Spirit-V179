import time

fieldID = sm.getFieldID()
if chr.getOrCreateFieldByCurrentInstanceType(863010600).getLifeByTemplateId(9390602) is not None:
    chr.getField().broadcastPacket(FieldPacket.clock(ClockPacket.timerGauge(chr.getInstance().getRemainingTime() * 1000, 90 * 1000)));
sm.addCurrentGolluxMap()
if chr.getField().getMobs().size() == 3:
    sm.addClearedGolluxMap()
    if fieldID == 863010310 or fieldID == 863010410:
        sm.openGolluxPortal("open", 2)
        sm.openGolluxPortal("clear", 1)
else:
    while chr.getField().getMobs().size() >= 3:
        sm.waitForMobDeath()
        sm.chatScript("There are " + str(chr.getField().getMobs().size() -3) + " doses of evil energy remaining in this area.")
        if chr.getField().getMobs().size() == 3:
            sm.addClearedGolluxMap()
            #GMS-like field effect
            #sm.showFieldEffect("Map/EffectTW.img/arisan/clear")
            #Much nicer/smaller profile for clearing side/monster maps
            sm.showFieldEffect("Map/Effect.img/monsterPark/clear")
            if fieldID == 863010310 or fieldID == 863010410:
                sm.openGolluxPortal("open", 1)
                time.sleep(.5)
                sm.openGolluxPortal("clear", 1)
            else:
                break
            