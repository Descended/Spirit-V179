from net.swordie.ms.constants import BossConstants
import time

RECTANGLE_SIZE = 400

while not sm.zakumAlreadySpawned(sm.getFieldID()):
    drop = sm.getDropInRect(BossConstants.ZAKUM_HARD_SPAWN_ITEM, RECTANGLE_SIZE, BossConstants.ZAKUM_SPAWN_X, BossConstants.ZAKUM_SPAWN_Y)
    if drop is not None:
        field.removeDrop(drop.getObjectId(), 0, False, -1)
        m.spawnZakum(sm.getFieldID())