import time
from net.swordie.ms.connection.packet import FieldPacket
from net.swordie.ms.world.field import ClockPacket
# Gollux Portals
fields = {
# Main map id : main portal id : [to field id, to field portal]
863010100 : {
    3 : [863010300, 2],
    6 : [863010300, 2],
    7 : [863010400, 2],
    8 : [863010200, 1],
    9 : [863010220, 1],
},
863010200 : {
    0 : [863010100, 2],
    1 : [863010100, 8],
    2 : [863010210, 1],
},
863010210 : {
    1 : [863010200, 2],
    2 : [863010240, 2],
},
863010220 : {
    1 : [863010100, 9],
    2 : [863010230, 1],
},
863010230 : {
    1 : [863010220, 2],
    2 : [863010240, 1],
},
863010240 : {
    1 : [863010230, 2],
    2 : [863010210, 2],
},
863010300 :{
    1 : [863010310, 1],
    2 : [863010100, 6],
},
863010310 : {
    1 : [863010300, 1],
    2 : [863010320, 2],
},
863010320 : {
    1 : [863010500, 1],
    2 : [863010310, 2],
    3 : [863010330, 1],
},
863010330 : {
    1 : [863010320, 3],
},
863010400 : {
    1 : [863010410, 1],
    2 : [863010100, 7],
},
863010410 : {
    1 : [863010400, 1],
    2 : [863010420, 1],
},
863010420 : {
    1 : [863010410, 2],
    2 : [863010500, 5],
    3 : [863010430, 1],
},
863010430 : {
    1 : [863010420, 3],
},
863010500 : {
    1 : [863010320, 1],
    2 : [863010240, 3],
    3 : [863010600, 2],
    4 : [863010330, 3],
    5 : [863010420, 2],
    6 : [863010430, 3],
},
863010600 : {
    2 : [863010500, 3],
},
}


fieldID = sm.getFieldID()

if parentID == 3 and (fieldID == 863010240 or fieldID == 863010330 or fieldID == 863010430):
    if fieldID == 863010240:
        if chr.getField().getLifeByTemplateId(9390612) is not None:
            sm.chatScript("You must defeat Gollux's Abdomen before you are able to move forward through this portal.")
        else:
            sm.warp(863010500, 2)

    if fieldID == 863010330:
        if chr.getField().getLifeByTemplateId(9390610) is not None:
            sm.chatScript("You must defeat Gollux's Right Shoulder before you are able to move forward through this portal.")
        else:
            sm.warp(863010500, 4)
                
    if fieldID == 863010430:
        if chr.getField().getLifeByTemplateId(9390611) is not None:
            sm.chatScript("You must defeat Gollux's Left Shoulder before you are able to move forward through this portal.")
        else:
            sm.warp(863010500, 6)
                
else:
    innerDict = fields[fieldID]
    if parentID not in innerDict and not fieldID == 863010240 and not fieldID == 863010330 and not fieldID == 863010430:
        sm.chat("This portal (giantBossPortal, " + str(parentID) + ") is not yet coded for this map (" + str(fieldID) + ")")
    elif chr.getField().getMobs().size() > 3 and fieldID != 863010600:
        sm.chat("This portal is locked until all monsters are defeated, there is " + str(chr.getField().getMobs().size()-3) +" monster(s) remaning.")
    else:
        sm.warp(innerDict[parentID][0], innerDict[parentID][1])
