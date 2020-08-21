# 2431710 - Zakum - consume_2431710

import random

NPC = 9000174 # TODO find real scripting verbiage for this

ITEM = 2431710
REQUIRED = 10

MAGNIFICENT = 2591163 # todo
LEFT = 2591171
RIGHT = 2591178

reward = random.randint(LEFT, RIGHT)

sm.setSpeakerID(NPC)

list = ""
for i in range(LEFT, RIGHT):
    list += "\r\n#i" + i + "# #v" + i + "#"

if not sm.hasItem(ITEM, REQUIRED):
    text = "You need at least " + REQUIRED + " #i" + ITEM + "# #r#v" + ITEM + "##n.\r\nYou currently only have #c" + ITEM + "#.\r\n\r\nWhen you have the required items, you can trade them for one of the following random items:"

    sm.sendSayOkay(text + list)
elif not sm.canHold(reward, 1):
    sm.sendSayOkay("Make sure you have space in your inventory.")
elif sm.sendAskYesNo("Would you like to trade " + REQUIRED + " #i" + ITEM + "# #r#v" + ITEM + "##n\r\nFor one of the following items?" + list):
    sm.consumeItem(ITEM, REQUIRED)
    sm.giveItem(reward, 1)
else:
    sm.sendSayOkay("Don't waste my time or I may waste yours...") # he's the embodiment of death so he's gotta be scary sounding
