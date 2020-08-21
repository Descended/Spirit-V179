#2432462 - Profession Coupon

sm.setSpeakerID(2007) #Maple Administrator

herbalism = 92000000
mining = 92010000
smithing = 92020000
accessoryCrafting = 92030000
alchemy = 92040000

herbalismLevel = chr.getMakingSkillLevel(herbalism)
miningLevel = chr.getMakingSkillLevel(mining)
smithingLevel = chr.getMakingSkillLevel(smithing)
accessoryCraftingLevel = chr.getMakingSkillLevel(accessoryCrafting)
alchemyLevel = chr.getMakingSkillLevel(alchemy)

skillHerbalism = "\r\n#L0##bHerbalism"
skillMining = "\r\n#L1##bMining"
skillSmithing = "\r\n#L2##bSmithing"
skillAccessoryCrafting = "\r\n#L3##bAccessory Crafting"
skillAlchemy = "\r\n#L3##bAlchemy"

stringList = []

if herbalismLevel > 0:
    stringList.append(skillHerbalism)
if miningLevel > 0:
    stringList.append(skillMining)
if smithingLevel > 0:
    stringList.append(skillSmithing)
if accessoryCraftingLevel > 0:
    stringList.append(skillAccessoryCrafting)
if alchemyLevel > 0:
    stringList.append(skillAlchemy)

#Response Managing the skill increasing
response = sm.sendNext("Using this coupon allows you to upgrade one of your profession skills by 1 level." + ''.join(stringList))
if response == 0:
    if sm.sendAskYesNo("Are you sure you want to upgrade your #bHerbalism #klevel?") == 1:
        sm.sendSayOkay("Your #bHerbalism #klevel has been increased from #r" + str(herbalismLevel) + "#k to #r" + str(herbalismLevel+1) +"#k.")
        chr.setMakingSkillLevel(herbalism,herbalismLevel+1)
if response == 1:
    if sm.sendAskYesNo("Are you sure you want to upgrade your #bMining #klevel?") == 1:
        sm.sendSayOkay("Your #bMining #klevel has been increased from #r" + str(miningLevel) + "#k to #r" + str(miningLevel+1) +"#k.")
        chr.setMakingSkillLevel(mining,miningLevel+1)
if response == 2:
    if sm.sendAskYesNo("Are you sure you want to upgrade your #bSmithing #klevel?") == 1:
        sm.sendSayOkay("Your #bSmithing #klevel has been increased from #r" + str(smithingLevel) + "#k to #r" + str(smithingLevel+1) +"#k.")
        chr.setMakingSkillLevel(smithing,smithingLevel+1)
if response == 3:
    if sm.sendAskYesNo("Are you sure you want to upgrade your #bAccessory Crafting #klevel?") == 1:
        sm.sendSayOkay("Your #bAccessory Crafting #klevel has been increased from #r" + str(skillAccessoryCrafting) + "#k to #r" + str(skillAccessoryCrafting+1) +"#k.")
        chr.setMakingSkillLevel(accessoryCrafting,accessoryCraftingLevel+1)
if response == 4:
    if sm.sendAskYesNo("Are you sure you want to upgrade your #bAlchemy #klevel?") == 1:
        sm.sendSayOkay("Your #bAlchemy #klevel has been increased from #r" + str(skillAlchemy) + "#k to #r" + str(skillAlchemy+1) +"#k.")
        chr.setMakingSkillLevel(alchemy,alchemyLevel+1)

sm.consumeItem(2432462, 1)