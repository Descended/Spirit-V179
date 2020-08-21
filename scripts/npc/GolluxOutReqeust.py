sm.setSpeakerID(9390124) #Heart Tree Guardian

response = sm.sendAskYesNo("Are you sure you want to abandon the quest to cleanse the heart tree?")
if response:
    sm.clearGolluxClearedMaps()
    sm.warpInstanceOut(863010000, 2)
else:
    sm.sendSayOkay("You will be rewarded for your bravery, please help cleanse the corruption that has infected the heart tree.")