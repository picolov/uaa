package com.baswara.uaa.service;

import com.baswara.uaa.domain.Authority;
import com.baswara.uaa.domain.User;
import com.baswara.uaa.repository.AuthorityRepository;
import com.baswara.uaa.config.Constants;
import com.baswara.uaa.repository.UserRepository;
import com.baswara.uaa.security.AuthoritiesConstants;
import com.baswara.uaa.security.SecurityUtils;
import com.baswara.uaa.service.util.RandomUtil;
import com.baswara.uaa.service.dto.UserDTO;
import com.baswara.uaa.web.rest.vm.ManagedUserVM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String USERS_CACHE = "users";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRepository authorityRepository;

    private final CacheManager cacheManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
    }

    public Optional<User> activateRegistration(String key, String login) {
        log.debug("Activating user " + login + " for activation key {}", key);
        return userRepository.findOneByActivationKeyAndLogin(key, login)
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Activated user: {}", user);
                return user;
            });
    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
       log.debug("Reset user password for reset key {}", key);

       return userRepository.findOneByResetKey(key)
           .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
           .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                return user;
           });
    }

    public Optional<User> validatePasswordReset(String key) {
        log.debug("validate user for reset key {}", key);

        return userRepository.findOneByResetKey(key)
            .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)));
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
            .filter(User::getActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                return user;
            });
    }

    public Optional<User> requestPasswordResetMail(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
            .filter(User::getActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                return user;
            });
    }

    public Optional<User> requestPasswordResetSms(String mobile) {
        return userRepository.findOneByMobileIgnoreCase(mobile)
            .filter(User::getActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                return user;
            });
    }

    public User registerUser(UserDTO userDTO, String password) {
        User newUser = new User();
        Authority authority = authorityRepository.findOne(AuthoritiesConstants.UNVERIFIED);
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setMobile(userDTO.getMobile());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        // new user is not active
        newUser.setActivated(false);
        newUser.setTermAgreed(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public User reRegisterUser(UserDTO userDTO, User user, String password) {
        Authority authority = authorityRepository.findOne(AuthoritiesConstants.UNVERIFIED);
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        user.setLogin(userDTO.getLogin());
        // new user gets initially a generated password
        user.setPassword(encryptedPassword);
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setMobile(userDTO.getMobile());
        user.setImageUrl(userDTO.getImageUrl());
        user.setLangKey(userDTO.getLangKey());
        // new user is not active
        user.setActivated(false);
        user.setTermAgreed(false);
        // new user gets registration key
        user.setActivationKey(RandomUtil.generateActivationKey());
        authorities.add(authority);
        user.setAuthorities(authorities);
        userRepository.save(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setLogin(userDTO.getLogin());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setMobile(userDTO.getMobile());
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO.getAuthorities().stream()
                .map(authorityRepository::findOne)
                .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        String password = RandomUtil.generatePassword();
        String encryptedPassword = passwordEncoder.encode(password);
        user.setPassword(encryptedPassword);
        user.setActivated(true);
        user.setTermAgreed(false);
        userRepository.save(user);
        log.debug("Created Information for User: {}", user);
        user.setPassword(password);
        return user;
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user
     * @param lastName last name of user
     * @param email email id of user
     * @param mobile mobile phone number of user
     * @param langKey language key
     * @param imageUrl image URL of user
     */
    public void updateUser(String firstName, String lastName, String email, String mobile, String langKey, String imageUrl) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setMobile(mobile);
                user.setLangKey(langKey);
                user.setImageUrl(imageUrl);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed Information for User: {}", user);
            });
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update
     * @return updated user
     */
    public Optional<UserDTO> updateUser(UserDTO userDTO) {
        return Optional.of(userRepository
            .findOne(userDTO.getId()))
            .map(user -> {
                user.setLogin(userDTO.getLogin());
                user.setFirstName(userDTO.getFirstName());
                user.setLastName(userDTO.getLastName());
                user.setEmail(userDTO.getEmail());
                user.setMobile(userDTO.getMobile());
                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setTermAgreed(userDTO.isTermAgreed());
                user.setLangKey(userDTO.getLangKey());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                userDTO.getAuthorities().stream()
                    .map(authorityRepository::findOne)
                    .forEach(managedAuthorities::add);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed Information for User: {}", user);
                return user;
            })
            .map(UserDTO::new);
    }

    public void setTermAgreed(boolean agreed) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                user.setTermAgreed(agreed);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed Information for User: {}", user);
            });
    }

    public void deleteUser(String login) {
        userRepository.findOneByLogin(login).ifPresent(user -> {
            userRepository.delete(user);
            cacheManager.getCache(USERS_CACHE).evict(login);
            log.debug("Deleted User: {}", user);
        });
    }

    public void changePassword(String password) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                String encryptedPassword = passwordEncoder.encode(password);
                user.setPassword(encryptedPassword);
                cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
                log.debug("Changed password for User: {}", user);
            });
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities(String id) {
        return userRepository.findOneWithAuthoritiesById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getLogin());
            userRepository.delete(user);
            cacheManager.getCache(USERS_CACHE).evict(user.getLogin());
        }
    }

    /**
     * @return a list of all the authorities
     */
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
    }

}
