package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.MiniRoomType;
import net.swordie.ms.enums.RoomLeaveType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Merchant.BoughtItem;
import net.swordie.ms.life.Merchant.Merchant;
import net.swordie.ms.life.Merchant.MerchantItem;

/**
 * @author Sjonnie
 * Created on 8/10/2018.
 */
public class MiniroomPacket {

    public static OutPacket enterTrade(TradeRoom tradeRoom, Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.EnterTrade.getVal());
        outPacket.encodeByte(4);
        outPacket.encodeByte(2);

        outPacket.encodeByte(1); // client is always on the right side

        Char other = tradeRoom.getOtherChar(chr);
        outPacket.encodeByte(0); // trade partner is always on the left
        other.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(other.getName());
        outPacket.encodeShort(other.getJob());

        outPacket.encodeByte(1);
        chr.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());

        outPacket.encodeByte(-1); // end indicator

        return outPacket;
    }


    public static OutPacket putItem(int user, int pos, Item item) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.PlaceItem.getVal());
        outPacket.encodeByte(user);
        outPacket.encodeByte(pos);
        item.encode(outPacket);

        return outPacket;
    }

    public static OutPacket putMoney(int user, long money) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.SetMesos.getVal());
        outPacket.encodeByte(user);
        outPacket.encodeLong(money);

        return outPacket;
    }

    public static OutPacket tradeRestraintItem() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.TradeRestraintItem.getVal());

        return outPacket;
    }

    public static OutPacket tradeInvite(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.TradeInviteRequest.getVal());
        outPacket.encodeByte(4);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());
        outPacket.encodeArr(new byte[200]);

        return outPacket;
    }

    public static OutPacket cancelTrade() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.ExitTrade.getVal());
        outPacket.encodeByte(0); // other user cancelled
        outPacket.encodeByte(RoomLeaveType.TRLeave_TradeFail_Denied.getVal());

        return outPacket;
    }

    public static OutPacket tradeComplete() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.ExitTrade.getVal());
        outPacket.encodeByte(1);
        outPacket.encodeByte(RoomLeaveType.TRLeave_TradeDone.getVal());

        return outPacket;
    }

    public static OutPacket tradeConfirm() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.Trade.getVal());

        return outPacket;
    }

    public static OutPacket chat(int pos, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.Chat.getVal());
        outPacket.encodeByte(0); // ?
        outPacket.encodeByte(pos);
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket enterMerchant(Char character, Merchant merchant, boolean firsttime) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.EnterTrade.getVal());
        outPacket.encodeByte(6); // action
        outPacket.encodeByte(GameConstants.MAX_MERCHANT_VISITORS + 1); // number of slots
        if (character.getId() == merchant.getOwnerID()) {
            outPacket.encodeShort(0); //my position
        } else {
            outPacket.encodeShort(merchant.getVisitors().indexOf(character) + 1); //my position
        }
        outPacket.encodeInt(merchant.getItemID());
        outPacket.encodeString("Hired Merchant");
        for (byte i = 0; i < merchant.getVisitors().size(); i++) {
            outPacket.encodeByte(i + 1);
            merchant.getVisitors().get(i).getAvatarData().getAvatarLook().encode(outPacket);
            outPacket.encodeString(merchant.getVisitors().get(i).getName());
            outPacket.encodeShort(merchant.getVisitors().get(i).getJob());
        }
        outPacket.encodeByte(-1);
        outPacket.encodeShort(0);
        outPacket.encodeString(merchant.getOwnerName());
        if (merchant.getOwnerID() == character.getId()) {
            int timeleft = merchant.getTimeLeft();
            outPacket.encodeInt(timeleft);
            outPacket.encodeByte(firsttime ? 1 : 0);
            outPacket.encodeByte(merchant.getBoughtitems().size());
            for (BoughtItem item : merchant.getBoughtitems()) {
                outPacket.encodeInt(item.id);
                outPacket.encodeShort(item.quantity);
                outPacket.encodeLong(item.totalPrice);
                outPacket.encodeString(item.buyer);
            }
            outPacket.encodeLong(merchant.getMesos());
        }
        outPacket.encodeInt(263);
        outPacket.encodeString(merchant.getMessage());
        outPacket.encodeByte(GameConstants.MAX_MERCHANT_SLOTS); //size
        outPacket.encodeLong(merchant.getMesos());
        outPacket.encodeByte(merchant.getItems().size());
        for (MerchantItem item : merchant.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeLong(item.price);
            item.item.encode(outPacket);
        }
        outPacket.encodeShort(0);
        return outPacket;
    }

    public static OutPacket shopVisitorAdd(Char chr, int slot) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);


        outPacket.encodeByte(MiniRoomType.Accept.getVal());

        outPacket.encodeByte(slot);
        chr.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());

        return outPacket;
    }

    public static OutPacket shopVisitorRemove(Char chr, int slot) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);


        outPacket.encodeByte(MiniRoomType.Accept.getVal());

        outPacket.encodeByte(MiniRoomType.Accept.getVal());
        outPacket.encodeByte(slot);

        return outPacket;
    }

    public static OutPacket openShop(Merchant merchant) {

        OutPacket outPacket = new OutPacket(OutHeader.EMPLOYEE_ENTER_FIELD);


        outPacket.encodeInt(merchant.getOwnerID());
        outPacket.encodeInt(merchant.getItemID());
        outPacket.encodePosition(merchant.getPosition());
        outPacket.encodeShort(merchant.getFh());
        outPacket.encodeString(merchant.getOwnerName());
        int itemID = merchant.getItemID();
        byte type = 6;
        /*
        if(itemID>=5030000&&itemID<=5030001) //elf
            {type=6;}
        if(itemID>=5030002&&itemID<=5030003) //bear
        {type=20;}
        if(itemID>=5030004&&itemID<=5030005) //robo
        {type=8;}
        if(itemID>=5030008&&itemID<=5030009) //cute girl
        {type=9;}
        if(itemID>=5030010&&itemID<=5030011) //grandma
        {type=10;}
        if(itemID==5030012) //mustach guy
        {type=11;}
        */
        outPacket.encodeByte(type);
        outPacket.encodeInt(merchant.getObjectId());
        outPacket.encodeString(merchant.getMessage());
        outPacket.encodeByte(merchant.getShopHasPassword());
        outPacket.encodeByte(merchant.getItems().size());
        outPacket.encodeByte(GameConstants.MAX_MERCHANT_SLOTS);
        outPacket.encodeByte(merchant.getOpen());


        return outPacket;
    }

    public static OutPacket destroyShop(Merchant merchant) {
        OutPacket outPacket = new OutPacket(OutHeader.EMPLOYEE_LEAVE_FIELD);

        outPacket.encodeInt(merchant.getOwnerID());

        return outPacket;
    }

    public static OutPacket shopItemUpdate(Merchant merchant) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeByte(MiniRoomType.Update.getVal());
        outPacket.encodeLong(0L);
        outPacket.encodeByte(merchant.getItems().size());
        for (MerchantItem item : merchant.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeLong(item.price);
            item.item.encode(outPacket);
        }
        outPacket.encodeShort(0);

        return outPacket;
    }
}
