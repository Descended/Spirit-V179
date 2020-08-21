sm.setSpeakerID(9390124) #Heart Tree Guardian

response = sm.sendAskYesNo("Are you sure you want to abandon the quest to cleanse Gollux?")
if response:
    sm.warpInstanceOut(863010000, 2)
else:
    sm.sendSayOkay("Thank you for your bravery, please help cleanse the corruption that has infected the heart tree!")