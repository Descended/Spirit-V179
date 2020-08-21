if sm.getSlotsLeftToAddByInvType(4) < 8:
    sm.dispose()
sm.addInventorySlotsByInvType(8, 4)
sm.consumeItem()