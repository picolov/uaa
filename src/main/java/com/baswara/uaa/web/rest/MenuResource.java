package com.baswara.uaa.web.rest;

import com.baswara.uaa.domain.Menu;
import com.baswara.uaa.repository.MenuRepository;
import com.baswara.uaa.web.rest.errors.BadRequestAlertException;
import com.baswara.uaa.web.rest.util.HeaderUtil;
import com.codahale.metrics.annotation.Timed;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Menu.
 */
@RestController
@RequestMapping("/api")
public class MenuResource {

    private final Logger log = LoggerFactory.getLogger(MenuResource.class);

    private static final String ENTITY_NAME = "menu";

    private final MenuRepository menuRepository;

    public MenuResource(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * POST  /menu : Create a new pagelayout.
     *
     * @param menu the menu to create
     * @return the ResponseEntity with status 201 (Created) and with body the new menu, or with status 400 (Bad Request) if the menu has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/menu")
    @Timed
    public ResponseEntity<Menu> createMenu(@RequestBody Menu menu) throws URISyntaxException {
        log.debug("REST request to save Menu : {}", menu);
        if (menu.getId() != null) {
            throw new BadRequestAlertException("A new menu cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Menu result = menuRepository.save(menu);
        return ResponseEntity.created(new URI("/api/menu/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /menu : Updates an existing menu.
     *
     * @param menu the menu to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated menu,
     * or with status 400 (Bad Request) if the menu is not valid,
     * or with status 500 (Internal Server Error) if the menu couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/menu")
    @Timed
    public ResponseEntity<Menu> updateMenu(@RequestBody Menu menu) throws URISyntaxException {
        log.debug("REST request to update Menu : {}", menu);
        if (menu.getId() == null) {
            return createMenu(menu);
        }
        Menu result = menuRepository.save(menu);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, menu.getId()))
            .body(result);
    }

    /**
     * GET  /menu : get all the menues.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of menues in body
     */
    @GetMapping("/menu")
    @Timed
    public List<Menu> getAllMenues() {
        log.debug("REST request to get all Menues");
        return menuRepository.findAll();
        }

    /**
     * GET  /menu/:id : get the "id" menu.
     *
     * @param id the id of the menu to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the menu, or with status 404 (Not Found)
     */
    @GetMapping("/menu/{id}")
    @Timed
    public ResponseEntity<Menu> getMenu(@PathVariable String id) {
        log.debug("REST request to get Menu : {}", id);
        Menu menu = menuRepository.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(menu));
    }

    /**
     * DELETE  /menu/:id : delete the "id" menu.
     *
     * @param id the id of the menu to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/menu/{id}")
    @Timed
    public ResponseEntity<Void> deleteMenu(@PathVariable String id) {
        log.debug("REST request to delete Menu : {}", id);
        menuRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id)).build();
    }

    @PostMapping("/menu/bulk")
    @Timed
    public ResponseEntity<Object> createMenuList(@RequestBody List<Menu> menuList) throws URISyntaxException {
        log.debug("REST request to save MenuList : {}", menuList);
        List<Menu> menuResultList = menuRepository.save(menuList);
        return new ResponseEntity<>(menuResultList, HttpStatus.OK);
    }
}
