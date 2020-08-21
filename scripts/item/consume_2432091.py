# 2432091 - Gollux Head Teleport Rock
from net.swordie.ms.constants import BossConstants

sm.setSpeakerID(9390124) #Heart Tree Guardian
if not chr.getInstance() == None and chr.getFieldID() in BossConstants.GOLLUX_MAPS:
    if chr.getFieldID() == 863010600:
        sm.sendSayOkay("You are already within the map requested.\r\nYou must enter another map if you wish to use the item.")
    else:
        sm.warp(863010600)
        sm.consumeItem(2432091)
else:
    sm.sendSayOkay("You're not currently fighting the Gollux Boss.\r\nThe item has been maintained and you have not been teleported.")