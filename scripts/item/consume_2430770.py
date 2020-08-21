if sm.getSlotsLeftToAddByInvType(3) < 8:
    sm.dispose()
sm.addInventorySlotsByInvType(8, 3)
sm.consumeItem()