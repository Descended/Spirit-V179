package net.swordie.ms.life.Merchant;

import net.swordie.ms.client.character.items.Item;

public class BoughtItem extends Item {

    public int id;
    public int quantity;
    public long totalPrice;
    public String buyer;


    public BoughtItem(final int id, final int quantity, final long totalPrice, final String buyer) {
        this.id = id;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.buyer = buyer;
    }
}
