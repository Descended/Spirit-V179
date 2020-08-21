package net.swordie.ms.life.Merchant;

import net.swordie.ms.constants.GameConstants;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employeetrunk")
public class EmployeeTrunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private long money;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "employeetrunkid")
    private List<MerchantItem> items = new ArrayList<>();

    public EmployeeTrunk() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void removeItem(int pos) {
        getItems().remove(pos);
    }

    public List<MerchantItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<MerchantItem> items) {
        this.items = items;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        if (money < 0 || money > GameConstants.MAX_MONEY) {
            return;
        }
        this.money = money;
    }

    public boolean canAddMoney(long amount) {
        return getMoney() + amount <= GameConstants.MAX_MONEY;
    }

    public void addMoney(long reqMoney) {
        if (canAddMoney(reqMoney)) {
            setMoney(getMoney() + reqMoney);
        }
    }


}
