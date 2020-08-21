
response = sm.sendAskYesNo("Are you sure you want to leave?")

if response:
    sm.warpInstanceOut(401060000)
sm.dispose()
