# 101020400
if sm.hasQuest(21201):
    sm.warpInstanceIn(914021000, 1)
    sm.addQRValue(21203, "0")
    sm.setInstanceTime(15*60)
if sm.hasQuest(21302):
    sm.warpInstanceIn(914022100, 0)
    sm.setQRValue(21203, "1", False)
    sm.setInstanceTime(20*60)
