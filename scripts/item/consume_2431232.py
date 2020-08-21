import random
from net.swordie.ms.enums import ChatType
from net.swordie.ms.loaders import StringData

# 2431232 - Suspicious Mineral Pouch (Gollux and Root Abyss(?))
sm.setSpeakerID(2007) #Maple Administrator

oreAmount = random.randint(5,30)

ores = [
    #Metal and Metal Alloys
    4010000, #Bronze Ore
    4010001, #Steel Ore
    4010002, #Mithril Ore
    4010003, #Adamantium Ore
    4010004, #Silver Ore
    4010005, #Orihalcon Ore
    4010006, #Gold Ore
    4010007, #Lidium Ore
    #First Crystal/Precious Jewels
    4020000, #Garnet Ore
    4020001, #Amethyst Ore
    4020002, #AquaMarine Ore
    4020003, #Emerald Ore
    4020004, #Opal Ore
    4020005, #Sapphire Ore
    4020006, #Topaz Ore
    4020007, #Diamond Ore
    4020008, #Black Crystal Ore
    #Secondary Crystal/Precious Jewels
    4004000, #Power Crystal Ore
    4004001, #Wisdom Crystal Ore
    4004002, #DEX Crystal Ore
    4004003, #LUK Crystal Ore
    4004004  #Dark Crystal Ore
]

rewardID = random.choice(ores)


if sm.getEmptyInventorySlots(4) >= 1:
    sm.consumeItem(2431232)
    sm.giveItem(rewardID, oreAmount)
    chr.chatMessage(ChatType.GameDesc,"You have gained items in the Etc tab (" + StringData.getItemStringById(rewardID) + " " + str(oreAmount) + ")")

else:
    sm.sendSayOkay("You lack the required inventory space to use this item.\r\nSlots Required: #b1 Etc")
