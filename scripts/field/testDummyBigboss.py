import time

sm.updateGolluxMap()
#Triggers when you first enter the Gollux instance, dropping the right-leg
if chr.getFieldID() == 863010100:
    if sm.golluxMapAlreadyVisited():
        sm.openGolluxPortal("open", 2)
    else:
        chr.getInstance().setTimeout(1800)
        chr.setDeathCount(5)
        time.sleep(4.5)
        sm.openGolluxPortal("open", 1)
        sm.addClearedGolluxMap()
sm.addCurrentGolluxMap()
#Triggers regardless of the 2 maps that utilize this script (Heart - 863010500, Road to Gollux - 863010100)
if chr.getOrCreateFieldByCurrentInstanceType(863010600).getLifeByTemplateId(9390602) is not None:
    chr.getField().broadcastPacket(FieldPacket.clock(ClockPacket.timerGauge(chr.getInstance().getRemainingTime() * 1000, 90 * 1000)));
    