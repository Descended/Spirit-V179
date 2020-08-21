sm.setSpeakerID(1064013) # Second Seal Door
mapId = 105200100 #east garden
selection = sm.sendNext(".\r\n#b"
                        "#L0#I want to challenge Von Bon.#l\r\n"
                        "#L1#I want to challnge chaos Von Bon.#l\r\n")

if selection == 0:
    mapId = 105200100
elif selection == 1:
    mapId = 105200500 #east garden chaos

if sm.getParty() is None:
    sm.sendSayOkay("Please create a party before going in.")
elif not sm.isPartyLeader():
    sm.sendSayOkay("Please have your party leader enter if you wish to face Crimson Queen.")
elif sm.checkParty():
    sm.warpInstanceIn(mapId, True)