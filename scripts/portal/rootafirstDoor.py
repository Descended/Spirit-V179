sm.setSpeakerID(1064012) # First Seal Door
mapId = 105200200 #west garden
selection = sm.sendNext(".\r\n#b"
                        "#L0#I want to challenge Pierre.#l\r\n"
                        "#L1#I want to challnge chaos Pierre.#l\r\n")

if selection == 0:
    mapId = 105200200
elif selection == 1:
    mapId = 105200600 #west garden chaos

if sm.getParty() is None:
    sm.sendSayOkay("Please create a party before going in.")
elif not sm.isPartyLeader():
    sm.sendSayOkay("Please have your party leader enter if you wish to face Crimson Queen.")
elif sm.checkParty():
    sm.warpInstanceIn(mapId, True)
