import random
from net.swordie.ms.enums import ChatType
from net.swordie.ms.loaders import StringData

# 2431233 - Suspicious Herb Pouch (Gollux and Root Abyss(?))
sm.setSpeakerID(2007) #Maple Administrator

herbAmount = random.randint(5,30)

herbs = []

herbs = [
    #Herb Seeds
    4022000, #Marjoram Seed
    4022002, #Lavender Seed
    4022004, #Rosemary Seed
    4022006, #Mandarin Seed
    4022008, #Lemon Balm Seed
    4022011, #Jasmine Seed
    4022013, #Tea Tree Seed
    4022015, #Chamomile Seed
    4022017, #Patchouli Seed
    4022019, #Juniper Berry Seed
    #Herb Flowers
    4022001, #Marjoram Flower"
    4022003, #Lavender Flower"
    4022005, #Rosemary Flower"
    4022007, #Mandarin Flower"
    4022009, #Lemon Balm Flower"
    4022010, #Peppermint Flower"
    4022012, #Jasmine Flower"
    4022014, #Tea Tree Flower"
    4022016, #Chamomile Flower"
    4022018, #Patchouli Flower"
    4022020, #Juniper Berry Flower"
    4022021  #"Hyssop Flower"
]

rewardID = random.choice(herbs)


if sm.getEmptyInventorySlots(4) >= 1:
    sm.consumeItem(2431233)
    sm.giveItem(rewardID, herbAmount)
    chr.chatMessage(ChatType.GameDesc,"You have gained items in the Etc tab (" + StringData.getItemStringById(rewardID) + " " + str(herbAmount) + ")")

else:
    sm.sendSayOkay("You lack the required inventory space to use this item.\r\nSlots Required: #b1 Etc")
