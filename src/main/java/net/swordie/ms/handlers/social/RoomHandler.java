package net.swordie.ms.handlers.social;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.anticheat.Offense;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.db.DatabaseManager;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.MiniroomPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.MiniRoomType;
import net.swordie.ms.enums.PopularityResultType;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.handlers.EventManager;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Merchant.Merchant;
import net.swordie.ms.life.Merchant.MerchantItem;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.World;
import net.swordie.ms.world.field.Field;
import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class RoomHandler {

    private static final Logger log = Logger.getLogger(RoomHandler.class);

    @Handler(op = InHeader.MINI_ROOM)
    public static void handleMiniRoom(Char chr, InPacket inPacket) {
        if (chr.getClient().getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to use trade in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        chr.dispose();
        byte type = inPacket.decodeByte(); // MiniRoom Type value
        MiniRoomType mrt = MiniRoomType.getByVal(type);
        if (mrt == null) {
            log.error(String.format("Unknown miniroom type %d", type));
            return;
        }
        TradeRoom tradeRoom = chr.getTradeRoom();
        switch (mrt) {
            case PlaceItem:
            case PlaceItem_2:
            case PlaceItem_3:
            case PlaceItem_4:
                byte invType = inPacket.decodeByte();
                short bagIndex = inPacket.decodeShort();
                short quantity = inPacket.decodeShort();
                byte tradeSlot = inPacket.decodeByte(); // trade window slot number

                Item item = chr.getInventoryByType(InvType.getInvTypeByVal(invType)).getItemBySlot(bagIndex);
                if (item.getQuantity() < quantity) {
                    chr.getOffenseManager().addOffense(String.format("Character {%d} tried to trade an item {%d} with a higher quantity {%s} than the item has {%d}.", chr.getId(), item.getItemId(), quantity, item.getQuantity()));
                    return;
                }
                if (!item.isTradable()) {
                    chr.getOffenseManager().addOffense(String.format("Character {%d} tried to trade an item {%d} whilst it was trade blocked.", chr.getId(), item.getItemId()));
                    return;
                }
                if (chr.getTradeRoom() == null) {
                    chr.chatMessage("You are currently not trading.");
                    return;
                }
                Item offer = item.deepCopy();
                offer.setQuantity(quantity);
                if (tradeRoom.canAddItem(chr)) {
                    int consumed = quantity > item.getQuantity() ? 0 : item.getQuantity() - quantity;
                    item.setQuantity(consumed + 1); // +1 because 1 gets consumed by consumeItem(item)
                    chr.consumeItem(item);
                    tradeRoom.addItem(chr, tradeSlot, offer);
                }
                Char other = tradeRoom.getOtherChar(chr);
                chr.write(MiniroomPacket.putItem(0, tradeSlot, offer));
                other.write(MiniroomPacket.putItem(1, tradeSlot, offer));

                break;
            case SetMesos:
            case SetMesos_2:
            case SetMesos_3:
            case SetMesos_4:
                long money = inPacket.decodeLong();
                if (tradeRoom == null) {
                    chr.chatMessage("You are currently not trading.");
                    return;
                }
                if (money < 0 || money > chr.getMoney()) {
                    chr.getOffenseManager().addOffense(String.format("Character %d tried to add an invalid amount of mesos(%d, own money: %d)",
                            chr.getId(), money, chr.getMoney()));
                    return;
                }
                chr.deductMoney(money);
                chr.addMoney(tradeRoom.getMoney(chr));
                tradeRoom.putMoney(chr, money);
                other = tradeRoom.getOtherChar(chr);
                chr.write(MiniroomPacket.putMoney(0, money));
                other.write(MiniroomPacket.putMoney(1, money));
                break;
            case Trade:
            case TradeConfirm:
            case TradeConfirm2:
            case TradeConfirm3:
                other = tradeRoom.getOtherChar(chr);
                other.write(MiniroomPacket.tradeConfirm());
                if (tradeRoom.hasConfirmed(other)) {
                    boolean success = tradeRoom.completeTrade();
                    if (success) {
                        chr.write(MiniroomPacket.tradeComplete());
                        other.write(MiniroomPacket.tradeComplete());
                    } else {
                        tradeRoom.cancelTrade();
                        tradeRoom.getChr().write(MiniroomPacket.cancelTrade());
                        tradeRoom.getOther().write(MiniroomPacket.cancelTrade());
                    }
                    chr.setTradeRoom(null);
                    other.setTradeRoom(null);
                } else {
                    tradeRoom.addConfirmedPlayer(chr);
                }
                break;
            case Chat:
                inPacket.decodeInt(); // tick
                String msg = inPacket.decodeString();
                String msgWithName = String.format("%s : %s", chr.getName(), msg);
                if (tradeRoom != null) {
                    chr.write(MiniroomPacket.chat(1, msgWithName));
                    tradeRoom.getOtherChar(chr).write(MiniroomPacket.chat(0, msgWithName));
                    break;
                } else if (chr.getVisitingmerchant() != null) {
                    chr.getVisitingmerchant().broadCastPacket(MiniroomPacket.chat(1, msgWithName));
                    break;
                }
                chr.chatMessage("You are currently not in a room.");
                // this is kinda weird atm, so no different colours

                break;
            case Accept:
                if (tradeRoom == null) {
                    int objectid = inPacket.decodeInt();
                    Life life = chr.getField().getLifeByObjectID(objectid);
                    if (life instanceof Merchant) {
                        Merchant merchant = (Merchant) life;
                        if (!merchant.getOpen()) {
                            chr.chatMessage("This shop is in maintenance");
                            return;
                        } else if (merchant.getVisitors().size() >= GameConstants.MAX_MERCHANT_VISITORS) {
                            chr.chatMessage("Shop is full");
                        } else {
                            merchant.addVisitor(chr);
                            chr.setVisitingmerchant(merchant);
                            chr.getClient().write(MiniroomPacket.enterMerchant(chr, merchant, false));
                        }
                    }

                    return;
                }
                chr.write(MiniroomPacket.enterTrade(tradeRoom, chr));
                other = tradeRoom.getOtherChar(chr); // initiator
                other.write(MiniroomPacket.enterTrade(tradeRoom, other));

                // Start Custom ----------------------------------------------------------------------------------------
                String[] inventoryNames = new String[]{
                        "eqp",
                        "use",
                        "etc",
                        "setup",
                        "cash",};
                for (String invName : inventoryNames) {
                    chr.write(MiniroomPacket.chat(1, String.format("%s has %d free %s slots", other.getName(), other.getInventoryByType(InvType.getInvTypeByString(invName)).getEmptySlots(), invName)));
                    other.write(MiniroomPacket.chat(1, String.format("%s has %d free %s slots", chr.getName(), chr.getInventoryByType(InvType.getInvTypeByString(invName)).getEmptySlots(), invName)));
                }
                // End Custom ------------------------------------------------------------------------------------------

                break;
            case TradeInviteRequest:
                int charID = inPacket.decodeInt();
                other = chr.getField().getCharByID(charID);
                if (other == null) {
                    chr.chatMessage("Could not find that player.");
                    return;
                }
                if (other.getTradeRoom() != null) {
                    chr.chatMessage("That player is already trading.");
                    return;
                }
                other.write(MiniroomPacket.tradeInvite(chr));
                tradeRoom = new TradeRoom(chr, other);
                chr.setTradeRoom(tradeRoom);
                other.setTradeRoom(tradeRoom);
                break;
            case InviteResultStatic: // always decline?
                if (tradeRoom != null) {
                    other = tradeRoom.getOtherChar(chr);
                    other.chatMessage(String.format("%s has declined your trade invite.", chr.getName()));
                    other.setTradeRoom(null);
                }
                chr.setTradeRoom(null);
                break;
            case ExitTrade:
                if (tradeRoom != null) {
                    tradeRoom.cancelTrade();
                    tradeRoom.getOtherChar(chr).write(MiniroomPacket.cancelTrade());
                }
                if (chr.getVisitingmerchant() != null) {
                    chr.getVisitingmerchant().removeVisitor(chr);
                    chr.setVisitingmerchant(null);
                }
                break;
            case TradeConfirmRemoteResponse:
                // just an ack by the client?
                break;
            case Merchant:
                if (chr.getMerchant() != null) {
                    chr.chatMessage("You already got an open merchant.");
                    return;
                }
                if (chr.getAccount().getEmployeeTrunk().getMoney() > 0 || !chr.getAccount().getEmployeeTrunk().getItems().isEmpty()) {
                    chr.chatMessage("You must retrieve your items from fredrick before opening a merchant.");
                    return;
                }
                inPacket.decodeByte(); //tick
                String text = inPacket.decodeString();
                inPacket.decodeByte(); //tick
                byte slot = inPacket.decodeByte();
                inPacket.decodeByte(); //tick
                inPacket.decodeInt();  //tock
                int itemid = chr.getCashInventory().getItemBySlot(slot).getItemId();
                Merchant merchant = new Merchant(0);
                merchant.setStartTime(System.currentTimeMillis());
                merchant.setPosition(chr.getPosition());
                merchant.setOwnerID(chr.getId());
                merchant.setOwnerName(chr.getName());
                merchant.setOpen(false);
                merchant.setMessage(text);
                merchant.setItemID(itemid);
                merchant.setFh(chr.getFoothold());
                merchant.setWorldId(chr.getWorld().getWorldId());
                merchant.setEmployeeTrunk(chr.getAccount().getEmployeeTrunk());
                chr.setMerchant(merchant);
                merchant.setField(chr.getField());
                chr.getField().addLife(merchant);
                chr.getWorld().getMerchants().add(merchant);
                chr.getClient().write(MiniroomPacket.enterMerchant(chr, chr.getMerchant(), true));
                break;
            case OwnerEnterMerchant:
                //
                inPacket.decodeByte();
                type = inPacket.decodeByte();
                String pic = inPacket.decodeString();
                int objId = inPacket.decodeInt();
                Life life = chr.getField().getLifeByObjectID(objId);
                if (life instanceof Merchant) {
                    merchant = (Merchant) life;
                    if (merchant.getOwnerID() != chr.getId()) {
                        return;
                    }
                    merchant.setOpen(false);
                    for (Char visitor : chr.getMerchant().getVisitors()) {
                        chr.getMerchant().removeVisitor(visitor);
                    }
                    chr.getClient().write(MiniroomPacket.enterMerchant(chr, chr.getMerchant(), false));
                }
                //
                break;
            case Open1:
            case Open2:
            case OPEN3:
                merchant = chr.getMerchant();
                merchant.setOpen(true);
                chr.getField().broadcastPacket(MiniroomPacket.openShop(merchant));
                EventManager.addEvent(() -> merchant.closeMerchant(), TimeUnit.HOURS.toMillis(24)); //remove merchant in 24 hours
                break;
            case AddItem1:
            case AddItem2:
            case AddItem3:
            case AddItem4:
                merchant = chr.getMerchant();
                final InvType invType1 = InvType.getInvTypeByVal(inPacket.decodeByte());
                slot = (byte) inPacket.decodeShort();
                final short bundles = inPacket.decodeShort();
                final short perBundle = inPacket.decodeShort();
                final int price = inPacket.decodeInt();
                item = chr.getInventoryByType(invType1).getItemBySlot(slot);
                int totalQuantity = bundles * perBundle;
                if (item == null) {
                    chr.getOffenseManager().addOffense("Tried to add a non-existing item to store.");
                }
                if (totalQuantity > 0 && totalQuantity <= item.getQuantity() && merchant.getItems().size() < GameConstants.MAX_MERCHANT_SLOTS) {
                    Item itemCopy = item.deepCopy();
                    if (item instanceof Equip) {
                        chr.consumeItem(item);
                    } else {
                        chr.consumeItem(item.getItemId(), totalQuantity);
                    }
                    itemCopy.setQuantity(perBundle);
                    MerchantItem mi = new MerchantItem(itemCopy, bundles, price);
                    merchant.getItems().add(mi);
                    chr.getAccount().getEmployeeTrunk().getItems().add(mi);
                    chr.getClient().write(MiniroomPacket.shopItemUpdate(merchant));
                    DatabaseManager.saveToDB(mi);
                    DatabaseManager.saveToDB(chr.getAccount().getEmployeeTrunk());
                }
                break;
            case CloseMerchant:
                if (chr.getMerchant() == null) {
                    return;
                }
                chr.getMerchant().closeMerchant();
                chr.getClient().write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("Please visit fredrick for your items.")));
                chr.setMerchant(null);
                chr.getItemsFromEmployeeTrunk();
                break;
            case OwnerLeaveMerchant:
                if (chr.getMerchant() != null) {
                    chr.getMerchant().setOpen(true);
                }
                break;
            case BuyItem:
            case BuyItem1:
            case BuyItem2:
            case BuyItem3:
                int itempos = inPacket.decodeByte();
                quantity = inPacket.decodeShort();
                merchant = chr.getVisitingmerchant();
                if (merchant == null) {
                    return;
                }
                merchant.buyItem(chr, itempos, quantity);
                break;
            case RemoveItem:
                inPacket.decodeByte();
                slot = (byte) inPacket.decodeShort();
                merchant = chr.getMerchant();
                if (merchant == null || merchant.getOwnerID() != chr.getId() || merchant.getItems().size() == 0) {
                    return;
                }
                MerchantItem merchantItem = merchant.getItems().get(slot);
                if (merchantItem == null || merchantItem.bundles <= 0) {
                    return;
                }
                item = merchantItem.item;
                long amount = merchantItem.bundles * item.getQuantity();
                if (amount <= 0 || amount > 32767) {
                    return;
                }
                Item newCopy = item.deepCopy();
                newCopy.setQuantity((int) amount);
                if (!chr.getInventoryByType(newCopy.getInvType()).canPickUp(newCopy)) {
                    return;
                }
                chr.addItemToInventory(newCopy);
                merchant.getItems().remove(merchantItem);
                chr.getAccount().getEmployeeTrunk().getItems().remove(merchantItem);
                chr.getClient().write(MiniroomPacket.shopItemUpdate(chr.getMerchant()));
                DatabaseManager.deleteFromDB(merchantItem);
                break;
            case TidyMerchant:
                chr.getMerchant().tidyMerchant(chr);
                break;
            default:
                log.error(String.format("Unhandled miniroom type %s", mrt));
        }
    }

    @Handler(op = InHeader.USER_GIVE_POPULARITY_REQUEST)
    public static void handleUserGivePopularityRequest(Char chr, InPacket inPacket) {
        int targetChrId = inPacket.decodeInt();
        boolean increase = inPacket.decodeByte() != 0;

        Field field = chr.getField();
        Char targetChr = field.getCharByID(targetChrId);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();

        if (targetChr == null) { // Faming someone who isn't in the map or doesn't exist
            chr.write(WvsContext.givePopularityResult(PopularityResultType.InvalidCharacterId, targetChr, 0, increase));
            chr.dispose();
        } else if (chr.getLevel() < GameConstants.MIN_LEVEL_TO_FAME || targetChr.getLevel() < GameConstants.MIN_LEVEL_TO_FAME) { // Chr or TargetChr is too low level
            chr.write(WvsContext.givePopularityResult(PopularityResultType.LevelLow, targetChr, 0, increase));
            chr.dispose();
        } else if (!cs.getNextAvailableFameTime().isExpired()) { // Faming whilst Chr already famed within the FameCooldown time
            chr.write(WvsContext.givePopularityResult(PopularityResultType.AlreadyDoneToday, targetChr, 0, increase));
            chr.dispose();
        } else if (targetChrId == chr.getId()) {
            chr.getOffenseManager().addOffense(Offense.Type.Warning,
                    String.format("Character %d tried to fame themselves", chr.getId()));
        } else {
            targetChr.addStatAndSendPacket(Stat.pop, (increase ? 1 : -1));
            int curPop = targetChr.getAvatarData().getCharacterStat().getPop();
            chr.write(WvsContext.givePopularityResult(PopularityResultType.Success, targetChr, curPop, increase));
            targetChr.write(WvsContext.givePopularityResult(PopularityResultType.Notify, chr, curPop, increase));
            cs.setNextAvailableFameTime(FileTime.fromDate(LocalDateTime.now().plusHours(GameConstants.FAME_COOLDOWN)));
            if (increase) {
                Effect.showFameGradeUp(targetChr);
            }
        }
    }

    @Handler(op = InHeader.LIKE_POINT)
    public static void handleLikePoint(Client c, InPacket inPacket) {
        //TODO
    }

    @Handler(op = InHeader.USER_ENTRUSTED_SHOP_REQUEST)
    public static void handleUserEntrustedShopRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        World world = c.getWorld();
        chr.write(WvsContext.merchantResult());
    }
}
