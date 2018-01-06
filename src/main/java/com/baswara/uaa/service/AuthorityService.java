package com.baswara.uaa.service;

import com.baswara.uaa.domain.Authority;
import com.baswara.uaa.domain.Menu;
import com.baswara.uaa.repository.AuthorityRepository;
import com.baswara.uaa.repository.MenuRepository;
import com.baswara.uaa.service.dto.AuthorityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorityService {

    private final Logger log = LoggerFactory.getLogger(AuthorityService.class);

    private static final String AUTHORITIES_CACHE = "authorities";

    private final AuthorityRepository authorityRepository;

    private final MenuRepository menuRepository;

    private final CacheManager cacheManager;

    public AuthorityService(AuthorityRepository authorityRepository, MenuRepository menuRepository, CacheManager cacheManager) {
        this.authorityRepository = authorityRepository;
        this.menuRepository = menuRepository;
        this.cacheManager = cacheManager;
    }

    public Authority createAuthority(AuthorityDTO authorityDTO) {
        Authority authority = new Authority();
        authority.setName(authorityDTO.getName());
        if (authorityDTO.getMenus() != null) {
            Set<Menu> menus = authorityDTO.getMenus().stream()
                .map(menuRepository::findOne)
                .collect(Collectors.toSet());
            authority.setMenus(menus);
        }
        authorityRepository.save(authority);
        log.debug("Created Information for Authority: {}", authority);
        return authority;
    }

    public Optional<AuthorityDTO> updateAuthority(AuthorityDTO authorityDTO) {
        return Optional.of(authorityRepository
            .findOne(authorityDTO.getName()))
            .map(authority -> {
                authority.setName(authorityDTO.getName());
                Set<Menu> managedMenus = authority.getMenus();
                managedMenus.clear();
                authorityDTO.getMenus().stream()
                    .map(menuRepository::findOne)
                    .forEach(managedMenus::add);
                cacheManager.getCache(AUTHORITIES_CACHE).evict(authority.getName());
                log.debug("Changed Information for Authority: {}", authority);
                return authority;
            })
            .map(AuthorityDTO::new);
    }

    public void deleteAuthority(String name) {
        authorityRepository.findOneByName(name).ifPresent(authority -> {
            authorityRepository.delete(authority);
            cacheManager.getCache(AUTHORITIES_CACHE).evict(name);
            log.debug("Deleted Authority: {}", authority);
        });
    }

    @Transactional(readOnly = true)
    public Page<AuthorityDTO> getAllAuthorities(Pageable pageable) {
        return authorityRepository.findAll(pageable).map(AuthorityDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<Authority> getAuthorityWithMenusByName(String name) {
        return authorityRepository.findOneWithMenusByName(name);
    }

    @Transactional(readOnly = true)
    public Optional<Authority> getUserWithAuthorities(String name) {
        return authorityRepository.findOneWithMenusByName(name);
    }

    public List<String> getMenus() {
        return menuRepository.findAll().stream().map(Menu::getId).collect(Collectors.toList());
    }

}
