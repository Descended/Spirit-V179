import time
from net.swordie.ms.enums import GolluxDifficultyType
from net.swordie.ms.connection.packet import FieldPacket
from net.swordie.ms.world.field import ClockPacket
from net.swordie.ms.life.mob.boss.demian.stigma import DemianStigma
firstPhaseHeadID = 9390600
secondPhaseHeadID = 9390601
thirdPhaseHeadID = 9390602
golluxLeftSideMobID = 9390622
#This Mob in particular needs to be turned around, I am not sure of the means to do so... he is getting placed on the left side as well :komegalul:
golluxRightSideMobID = 9390623

sm.updateGolluxMap()
sm.blockGolluxAttacks()
if sm.golluxMapAlreadyVisited() is not True:
    sm.spawnGollux(0)
    chr.getField().spawnMobRespawnable(golluxRightSideMobID, -650, 0, True, 1, 10)
    chr.getField().spawnMobRespawnable(golluxLeftSideMobID, -650, 0, True, 1, 10)
    
sm.addCurrentGolluxMap()
if sm.hasMobById(secondPhaseHeadID):
    sm.changeFootHold("phase2-1", True)
    sm.changeFootHold("phase2-2", True)
if sm.hasMobById(thirdPhaseHeadID):
    sm.changeFootHold("phase2-1", True)
    sm.changeFootHold("phase2-2", True)
    sm.changeFootHold("phase3", True)
if sm.hasMobById(firstPhaseHeadID):
    sm.waitForMobDeath(firstPhaseHeadID)
    sm.changeFootHold("phase2-1", True)
    sm.changeFootHold("phase2-2", True)
    sm.spawnGollux(1)
if sm.hasMobById(secondPhaseHeadID):
    sm.waitForMobDeath(secondPhaseHeadID)
    sm.changeFootHold("phase3", True)
    sm.spawnGollux(2)
    #Timer Gauge defaults to Von-Bon, thus always displaying his message instead of Gollux's
    sm.createTimerGauge(90)
    chr.getInstance().setTimeout(90)
if sm.hasMobById(thirdPhaseHeadID):
    chr.getField().broadcastPacket(FieldPacket.clock(ClockPacket.timerGauge(chr.getInstance().getRemainingTime() * 1000, 90 * 1000)));
    sm.waitForMobDeath(thirdPhaseHeadID)
    difficulty = sm.getGolluxDifficulty()
    sm.showFieldEffect("Map/EffectTW.img/arisan/clear")
    sm.clearGolluxClearedMaps()
    DemianStigma.resetStigma(chr)
    chr.setDeathCount(-1)
    chr.getInstance().setTimeout(305)#5 Minute and 5 second time out after defeating boss
    time.sleep(2.5)
    sm.warpInstanceIn(863010700)
    reactorId = int(str(863000) + str(difficulty.getVal()))
    sm.spawnReactorInState(reactorId, 95, 67, 1);
    chr.chatScriptMessage("Clear Difficulty : " + str(difficulty))