package com.baswara.uaa.repository;

import com.baswara.uaa.domain.Authority;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Authority entity.
 */
public interface AuthorityRepository extends JpaRepository<Authority, String> {
    @EntityGraph(attributePaths = "menus")
    Optional<Authority> findOneWithMenusByName(String name);

    Optional<Authority> findOneByName(String name);
}
