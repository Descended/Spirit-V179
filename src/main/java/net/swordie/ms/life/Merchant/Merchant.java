package net.swordie.ms.life.Merchant;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.db.DatabaseManager;
import net.swordie.ms.connection.packet.MiniroomPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.life.Life;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;

public class Merchant extends Life {

    private List<MerchantItem> items = new ArrayList<>();
    private List<BoughtItem> boughtitems = new ArrayList<>();
    private long money;
    private int ownerID;
    private int itemID;
    private long startTime;
    private boolean open;
    private String message;
    private String ownerName;
    private Boolean shopHasPassword;
    private ArrayList<Char> visitors;
    private EmployeeTrunk employeeTrunk;
    private int worldId;


    public Merchant(int templateId) {
        super(templateId);
        this.visitors = new ArrayList<Char>();
        this.money = 0;
        this.shopHasPassword = true;
        this.items = new ArrayList<MerchantItem>();
        this.boughtitems = new ArrayList<BoughtItem>();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getTimeLeft() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    public void addVisitor(Char visitor) {
        if (visitor.getId() == getOwnerID()) {
            return;
        }
        int newCharSlot = visitors.size();
        for (int k = 0; k < visitors.size(); k++) {
            visitors.get(k).write(MiniroomPacket.shopVisitorAdd(visitor, newCharSlot));
        }
        visitors.add(visitor);
    }

    public void removeVisitor(Char visitor) {
        visitor.setVisitingmerchant(null);
        int slot = visitors.indexOf(visitor) + 1;
        visitors.remove(visitor);
        visitor.getClient().write(MiniroomPacket.shopVisitorRemove(visitor, slot));
        for (Char visitor1 : visitors) {
            visitor1.getClient().write(MiniroomPacket.shopVisitorRemove(visitor, slot));
        }
    }

    public List<BoughtItem> getBoughtitems() {
        return boughtitems;
    }

    public void setBoughtitems(List<BoughtItem> boughtitems) {
        this.boughtitems = boughtitems;
    }

    public ArrayList<Char> getVisitors() {
        return visitors;
    }

    public void setVisitors(ArrayList<Char> visitors) {
        this.visitors = visitors;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(boolean o) {
        open = o;
    }


    public Boolean getShopHasPassword() {
        return shopHasPassword;
    }

    public void setShopHasPassword(Boolean shopHasPassword) {
        this.shopHasPassword = shopHasPassword;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }

    public long getMesos() {
        return money;
    }

    public void setMesos(long money) {
        if (money < 0 || money > GameConstants.MAX_MONEY) {
            return;
        }

        this.money = money;
    }

    public void addMesos(long amount) {
        setMesos(getMesos() + amount);
    }

    public List<MerchantItem> getItems() {
        return items;
    }

    public void setItems(List<MerchantItem> items) {
        this.items = items;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public Merchant deepCopy() {
        Merchant copy = new Merchant(getTemplateId());
        copy.setLifeType(getLifeType());
        copy.setX(getX());
        copy.setY(getY());
        copy.setMobTime(getMobTime());
        copy.setFlip(isFlip());
        copy.setHide(isHide());
        copy.setFh(getFh());
        copy.setCy(getCy());
        copy.setRx0(getRx0());
        copy.setRx1(getRx1());
        copy.setLimitedName(getLimitedName());
        copy.setUseDay(isUseDay());
        copy.setUseNight(isUseNight());
        copy.setHold(isHold());
        copy.setNoFoothold(isNoFoothold());
        copy.setDummy(isDummy());
        copy.setSpine(isSpine());
        copy.setMobTimeOnDie(isMobTimeOnDie());
        copy.setRegenStart(getRegenStart());
        copy.setOwnerID(getOwnerID());
        copy.setItemID(getItemID());
        copy.setItems(getItems());
        copy.setMesos(getMesos());
        return copy;
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        Field field = getField();
        for (Char chr : field.getChars()) {
            chr.write(MiniroomPacket.openShop(this));
        }
    }

    public void tidyMerchant(Char chr) {
        Long earnings = chr.getMerchant().getMesos();
        if (!chr.canAddMoney(earnings)) {
            chr.chatMessage("You cannot hold that much mesos.");
            return;
        }
        chr.addMoney(earnings);
        chr.getMerchant().setMesos(0);
        chr.getAccount().getEmployeeTrunk().setMoney(0);

        ArrayList<MerchantItem> itemsToRemove = new ArrayList<>();
        for (MerchantItem mi : items) {
            if (mi.bundles <= 0) {
                itemsToRemove.add(mi);
            }
        }
        items.remove(itemsToRemove);
        getEmployeeTrunk().getItems().remove(itemsToRemove);
        for (MerchantItem merchantItem : itemsToRemove) {
            DatabaseManager.deleteFromDB(merchantItem);
        }

        chr.getClient().write(MiniroomPacket.shopItemUpdate(chr.getMerchant()));
    }

    public void buyItem(Char customer, int itemPos, short quantity) {
        long price = quantity * getItems().get(itemPos).price;
        MerchantItem itemToBuy = getItems().get(itemPos);
        Item itemCopy = itemToBuy.item.deepCopy();
        if (customer.getMoney() < price || !customer.getInventoryByType(itemCopy.getInvType()).canPickUp(itemCopy) || !canAddMoney(price)) { //customer does not have enough mesos or cannot hold item, merchant can't hold that much mesos
            return;
        }
        EmployeeTrunk ownersEmployeeTrunk = getEmployeeTrunk();
        itemToBuy.bundles -= quantity;
        addMesos(price);
        ownersEmployeeTrunk.addMoney(price);
        customer.deductMoney(price);
        itemCopy.setQuantity(quantity * itemToBuy.item.getQuantity());
        customer.addItemToInventory(itemCopy);

        for (Char visitor1 : visitors) {
            visitor1.getClient().write(MiniroomPacket.shopItemUpdate(this));
        }
        addBoughtItem(itemToBuy.item, price, customer.getName());

        DatabaseManager.saveToDB(ownersEmployeeTrunk);
        DatabaseManager.saveToDB(itemToBuy);
        DatabaseManager.saveToDB(customer);
    }

    @Override
    public void broadcastLeavePacket() {
        Field field = getField();
        for (Char chr : field.getChars()) {
            field.broadcastPacket(MiniroomPacket.destroyShop(this));
        }
    }

    public void addBoughtItem(Item item, long price, String name) {
        BoughtItem bi = new BoughtItem(item.getItemId(), item.getQuantity(), price, name);
        boughtitems.add(bi);
    }

    public boolean canAddMoney(long amount) {
        return getMesos() + amount <= GameConstants.MAX_MONEY;
    }

    public EmployeeTrunk getEmployeeTrunk() {
        return employeeTrunk;
    }

    public void setEmployeeTrunk(EmployeeTrunk employeeTrunk) {
        this.employeeTrunk = employeeTrunk;
    }

    public int getWorldId() {
        return worldId;
    }

    public void setWorldId(int worldId) {
        this.worldId = worldId;
    }


    public void closeMerchant() {
        setOpen(false);
        Server.getInstance().getWorldById(getWorldId()).getMerchants().remove(this);
        getField().removeLife(this);
        getField().broadcastPacket(MiniroomPacket.destroyShop(this));
    }

    public void broadCastPacket(OutPacket outPacket) {
        getVisitors().forEach(chr -> chr.write(outPacket));
    }
}
