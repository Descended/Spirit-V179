if sm.getSlotsLeftToAddByInvType(1) < 8:
    sm.dispose()
sm.addInventorySlotsByInvType(8, 1)
sm.consumeItem()