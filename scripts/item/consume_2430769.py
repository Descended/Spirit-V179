if sm.getSlotsLeftToAddByInvType(2) < 8:
    sm.dispose()
sm.addInventorySlotsByInvType(8, 2)
sm.consumeItem()