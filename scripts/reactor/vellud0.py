import time, random

golluxPenny = 4310098 #Gollux Penny
golluxCoin = 4310097 #Gollux Coin
crackedBelt = 1132243 #Cracked Engraved Gollux Belt
crackedPendant = 1122264 #Cracked Engraved Gollux Pendant
powerElixir = 2000005 #Power Elixir

items = []
quantitys = []

#3 Power Elixir drops with random quantities between 1-4
items.append(powerElixir)
quantitys.append(random.randint(1,4))
items.append(powerElixir)
quantitys.append(random.randint(1,4))
items.append(powerElixir)
quantitys.append(random.randint(1,4))

#3 singular Gollux Penny drops and 2 with random quantities between 1-3
items.append(golluxPenny)
quantitys.append(1)
items.append(golluxPenny)
quantitys.append(1)
items.append(golluxPenny)
quantitys.append(1)
items.append(golluxPenny)
quantitys.append(random.randint(1,3))
items.append(golluxPenny)
quantitys.append(random.randint(1,3))

#1 singular Gollux Coin drop and 1 with a random quantity between 1-4
items.append(golluxCoin)
quantitys.append(random.randint(1,3))
items.append(golluxCoin)
quantitys.append(random.randint(1,4))


if random.randint(1,100) <= 35:
    items.append(crackedBelt)
    quantitys.append(1)
if random.randint(1,100) <= 5:
    items.append(crackedBelt)
    quantitys.append(1)

if random.randint(1,100) <= 45:
    items.append(crackedPendant)
    quantitys.append(1)
if random.randint(1,100) <= 5:
    items.append(crackedPendant)
    quantitys.append(1)



if reactor.getHitCount() == 0:
    reactor.incHitCount()
    
    sm.removeReactor()
    time.sleep(.75)
    sm.spawnReactorInState(8630004, 95, 67, 1)
    chr.getField().dropItemsAlongLine(items, quantitys, True, 115, 95, 75, 125)