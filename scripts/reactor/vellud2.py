import time, random

golluxPenny = 4310098 #Gollux Penny
golluxCoin = 4310097 #Gollux Coin
crackedBelt = 1132243 #Cracked Engraved Gollux Belt
crackedPendant = 1122264 #Cracked Engraved Gollux Pendant
solidBelt = 1132244 #Solid Engraved Gollux Belt
solidPendant = 1122265 #Solid Engraved Gollux Pendant
reinforcedBelt = 1132245 #Reinforced Engraved Gollux Belt
reinforcedPendant = 1122266 #Reinforced Engraved Gollux Pendant
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

#1 singular Gollux Penny drop and 4 with random quantities between 1-5
items.append(golluxPenny)
quantitys.append(1)
items.append(golluxPenny)
quantitys.append(random.randint(1,5))
items.append(golluxPenny)
quantitys.append(random.randint(1,5))
items.append(golluxPenny)
quantitys.append(random.randint(1,5))
items.append(golluxPenny)
quantitys.append(random.randint(1,5))

#1 singular Gollux Coin drop and 1 with a random quantity between 10-19
items.append(golluxCoin)
quantitys.append(1)
items.append(golluxCoin)
quantitys.append(random.randint(10,19))

if random.randint(1,100) <= 10:
    items.append(crackedBelt)
    quantitys.append(1)
if random.randint(1,100) <= 5:
    items.append(crackedBelt)
    quantitys.append(1)

if random.randint(1,100) <= 10:
    items.append(crackedPendant)
    quantitys.append(1)
if random.randint(1,100) <= 5:
    items.append(crackedPendant)
    quantitys.append(1)

if random.randint(1,100) <= 15:
    items.append(solidBelt)
    quantitys.append(1)
if random.randint(1,100) <= 5:
    items.append(solidBelt)
    quantitys.append(1)

if random.randint(1,100) <= 15:
    items.append(solidPendant)
    quantitys.append(1)
if random.randint(1,100) <= 5:
    items.append(solidPendant)
    quantitys.append(1)

if random.randint(1,100) <= 20:
    items.append(reinforcedBelt)
    quantitys.append(1)
if random.randint(1,100) <= 15:
    items.append(reinforcedBelt)
    quantitys.append(1)

if random.randint(1,100) <= 30:
    items.append(reinforcedPendant)
    quantitys.append(1)
if random.randint(1,100) <= 15:
    items.append(reinforcedPendant)
    quantitys.append(1)



if reactor.getHitCount() == 0:
    reactor.incHitCount()
    
    sm.removeReactor()
    time.sleep(.75)
    sm.spawnReactorInState(8630004, 95, 67, 1)
    chr.getField().dropItemsAlongLine(items, quantitys, True, 115, 95, 75, 125)