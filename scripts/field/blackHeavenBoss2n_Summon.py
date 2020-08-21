lotusSecondPhase = 8950001
lotusThirdPhaseMapId = 350060900

sm.spawnLotus(1, 0)
sm.waitForMobDeath(lotusSecondPhase)
sm.warp(lotusThirdPhaseMapId, 0, True)