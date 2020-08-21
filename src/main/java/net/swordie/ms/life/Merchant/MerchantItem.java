package net.swordie.ms.life.Merchant;

import net.swordie.ms.client.character.items.Item;

import javax.persistence.*;

@Entity
@Table(name = "merchantitems")
public class MerchantItem {
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "merchantitemid")
    public Item item;
    public short bundles;
    public int price;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    public MerchantItem(Item item, short bundles, int price) {
        this.item = item;
        this.bundles = bundles;
        this.price = price;
    }

    public MerchantItem() {
    }
}
