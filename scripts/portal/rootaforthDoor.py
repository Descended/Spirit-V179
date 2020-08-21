sm.setSpeakerID(1064015) # Fourth Seal Door
mapId = 105200400 #north garden
selection = sm.sendNext(".\r\n#b"
                        "#L0#I want to challenge Vellum.#l\r\n"
                        "#L1#I want to challenge chaos Vellum.#l\r\n")

if selection == 0:
    mapId = 105200400
elif selection == 1:
    mapId = 105200800 #north garden chaos

if sm.getParty() is None:
    sm.sendSayOkay("Please create a party before going in.")
elif not sm.isPartyLeader():
    sm.sendSayOkay("Please have your party leader enter if you wish to face Crimson Queen.")
elif sm.checkParty():
    sm.warpInstanceIn(mapId, True)
