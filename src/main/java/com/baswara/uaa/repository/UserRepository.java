package com.baswara.uaa.repository;

import com.baswara.uaa.domain.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

/**
 * Spring Data JPA repository for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findOneByActivationKey(String activationKey);

    Optional<User> findOneByActivationKeyAndLogin(String activationKey, String login);

    List<User> findAllByActivatedIsFalseAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneByEmailIgnoreCaseAndIdNot(String email, String idExclude);

    Optional<User> findOneByMobileIgnoreCase(String mobile);

    Optional<User> findOneByMobileIgnoreCaseAndIdNot(String mobile, String idExclude);

    Optional<User> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesById(String id);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = "users")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    Page<User> findAllByLoginNot(Pageable pageable, String login);
}
