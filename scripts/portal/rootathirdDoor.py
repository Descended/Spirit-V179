sm.setSpeakerID(1064014) # Third Seal Foor
mapId = 0
selection = sm.sendNext(".\r\n#b"
                        "#L0#I want to challenge the Crimson Queen.#l\r\n"
                        "#L1#I want to challnge chaos Crimson Queen#l\r\n")

if selection == 0:
    mapId = 105200300
elif selection == 1:
    mapId = 105200700

if sm.getParty() is None:
    sm.sendSayOkay("Please create a party before going in.")
elif not sm.isPartyLeader():
    sm.sendSayOkay("Please have your party leader enter if you wish to face Crimson Queen.")
elif sm.checkParty():
    sm.warpInstanceIn(mapId, True) # South Garden