package com.baswara.uaa.web.rest;

import com.baswara.uaa.config.Constants;
import com.baswara.uaa.domain.Authority;
import com.baswara.uaa.repository.AuthorityRepository;
import com.baswara.uaa.security.AuthoritiesConstants;
import com.baswara.uaa.service.AuthorityService;
import com.baswara.uaa.service.dto.AuthorityDTO;
import com.baswara.uaa.web.rest.util.HeaderUtil;
import com.baswara.uaa.web.rest.util.PaginationUtil;
import com.codahale.metrics.annotation.Timed;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthorityResource {

    private final Logger log = LoggerFactory.getLogger(AuthorityResource.class);

    private final AuthorityService authorityService;

    public AuthorityResource(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @PostMapping("/authority")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Authority> createAuthority(@Valid @RequestBody AuthorityDTO authorityDTO) throws URISyntaxException {
        Authority authority = authorityService.createAuthority(authorityDTO);
        return new ResponseEntity<>(authority, HttpStatus.OK);
    }

    @PutMapping("/authority")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<AuthorityDTO> updateAuthority(@Valid @RequestBody AuthorityDTO authorityDTO) {
        Optional<AuthorityDTO> updatedAuthority = authorityService.updateAuthority(authorityDTO);

        return ResponseUtil.wrapOrNotFound(updatedAuthority,
            HeaderUtil.createAlert("userManagement.updated", authorityDTO.getName()));
    }

    @GetMapping("/authorities")
    @Timed
    public ResponseEntity<List<AuthorityDTO>> getAllAuthority(Pageable pageable) {
        final Page<AuthorityDTO> page = authorityService.getAllAuthorities(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/authority");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/authority/menus")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getMenus() {
        return authorityService.getMenus();
    }

    @GetMapping("/authority/{name:" + Constants.AUTHORITY_REGEX + "}")
    @Timed
    public ResponseEntity<AuthorityDTO> getAuthority(@PathVariable String name) {
        log.debug("REST request to get Authority : {}", name);
        return ResponseUtil.wrapOrNotFound(
            authorityService.getAuthorityWithMenusByName(name)
                .map(AuthorityDTO::new));
    }

    @DeleteMapping("/authority/{name:" + Constants.AUTHORITY_REGEX + "}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteAuthority(@PathVariable String name) {
        log.debug("REST request to delete Authority: {}", name);
        authorityService.deleteAuthority(name);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert( "authorityManagement.deleted", name)).build();
    }
}
