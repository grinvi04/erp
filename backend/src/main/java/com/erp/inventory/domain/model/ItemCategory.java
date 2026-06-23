package com.erp.inventory.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_category", schema = "inventory")
public class ItemCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_category_seq")
    @SequenceGenerator(name = "item_category_seq", sequenceName = "inventory.item_category_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ItemCategory parent;

    protected ItemCategory() {}

    public static ItemCategory of(String code, String name, ItemCategory parent) {
        ItemCategory cat = new ItemCategory();
        cat.code = code.toUpperCase();
        cat.name = name;
        cat.parent = parent;
        return cat;
    }

    public void update(String name, ItemCategory parent) {
        this.name = name;
        this.parent = parent;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public ItemCategory getParent() { return parent; }
}
