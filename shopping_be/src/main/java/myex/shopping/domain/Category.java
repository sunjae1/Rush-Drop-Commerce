package myex.shopping.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "category")
    private List<Item> items = new ArrayList<>();

    public void addItem(Item item) {
        if (item == null) {
            return;
        }
        item.changeCategory(this);
    }

    public void removeItem(Item item) {
        if (item == null) {
            return;
        }
        if (item.getCategory() == this) {
            item.changeCategory(null);
        }
    }
}
