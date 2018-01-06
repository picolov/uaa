package com.baswara.uaa.service.dto;

import com.baswara.uaa.domain.Authority;
import com.baswara.uaa.domain.Menu;

import java.util.Set;
import java.util.stream.Collectors;

public class AuthorityDTO {

    private String name;

    private Set<String> menus;

    public AuthorityDTO() {
        // Empty constructor needed for Jackson.
    }

    public AuthorityDTO(Authority authority) {
        this.name = authority.getName();
        this.menus = authority.getMenus().stream()
            .map(Menu::getId)
            .collect(Collectors.toSet());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getMenus() {
        return menus;
    }

    public void setMenus(Set<String> menus) {
        this.menus = menus;
    }

    @Override
    public String toString() {
        return "AuthorityDTO{" +
            "name='" + name + '\'' +
            ", menus=" + menus +
            "}";
    }
}
