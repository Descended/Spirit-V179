# 863010000 - Gollux

sm.setSpeakerID(9390124) #Heart Tree Guardian
response = sm.sendNext("<#e#rRoad to Gollux#n#k>\r\nThis is where #rGollux#k, the Heart Tree corrupted by rage, is located.\r\n#r(Your entry record will reset 22 hours after your previous entry.)\r\n#L0##bEnter to fight Gollux now (Lv. 140 Required)\r\n#L1##bChallenge later.")


if response == 0:
    if sm.getParty() is None:
        sm.sendSayOkay("You must be in a party before attempting to challenge the boss.")
    elif not sm.isPartyLeader():
        sm.sendSayOkay("Please have your party leader enter if you wish to face Gollux.")
    elif sm.checkParty():
        sm.warpInstanceIn(863010100, 5, True)
else:
    sm.sendSayOkay("Gollux is on the brink of being entirely corrupted, please find the time to join the cause.")
