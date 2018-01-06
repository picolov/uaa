package com.baswara.uaa.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.BatchSize;

/**
 * An authority (a security role) used by Spring Security.
 */
@Entity
@Table(name = "jhi_authority")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)

public class Authority implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    private String name;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "authority_menu",
        joinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "name")},
        inverseJoinColumns = {@JoinColumn(name = "menu_id", referencedColumnName = "id")})
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @BatchSize(size = 20)
    private Set<Menu> menus = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Menu> getMenus() {
        return menus;
    }

    public void setMenus(Set<Menu> menus) {
        this.menus = menus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Authority authority = (Authority) o;

        return !(name != null ? !name.equals(authority.name) : authority.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Authority{" +
            "name='" + name + '\'' +
            "}";
    }
}
