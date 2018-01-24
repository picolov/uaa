package com.baswara.uaa.web.rest;

import com.baswara.uaa.service.SmsService;
import com.codahale.metrics.annotation.Timed;

import com.baswara.uaa.domain.User;
import com.baswara.uaa.domain.Authority;
import com.baswara.uaa.domain.Menu;
import com.baswara.uaa.repository.UserRepository;
import com.baswara.uaa.repository.AuthorityRepository;
import com.baswara.uaa.security.SecurityUtils;
import com.baswara.uaa.service.MailService;
import com.baswara.uaa.service.UserService;
import com.baswara.uaa.service.dto.UserDTO;
import com.baswara.uaa.web.rest.errors.*;
import com.baswara.uaa.web.rest.vm.KeyAndPasswordVM;
import com.baswara.uaa.web.rest.vm.ManagedUserVM;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    public final String CAPTCHA_SECRET = "6LcN2DsUAAAAAElJu5IxsoBPcChAEpNl3LKmfCno";

    private final UserRepository userRepository;

    private final AuthorityRepository authorityRepository;

    private final UserService userService;

    private final MailService mailService;

    private final SmsService smsService;

    @Autowired
    private RestTemplate restTemplate;

    public AccountResource(UserRepository userRepository, AuthorityRepository authorityRepository, UserService userService, MailService mailService, SmsService smsService) {

        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.smsService = smsService;
    }

    /**
     * POST  /register : register the user.
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws LoginAlreadyUsedException 400 (Bad Request) if the login is already used
     */
    @PostMapping("/register")
    @Timed
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> registerAccount(@RequestBody Map<String, String> params) {
        ManagedUserVM managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(params.get("login"));
        managedUserVM.setEmail(params.get("email"));
        managedUserVM.setMobile(params.get("phone"));
        managedUserVM.setPassword(params.get("password"));
        managedUserVM.setLangKey(params.get("langKey"));
        managedUserVM.setFirstName(params.get("firstName"));
        managedUserVM.setLastName(params.get("lastName"));
        managedUserVM.setImageUrl(params.get("imageUrl"));
        String recaptcha = params.get("g-recaptcha-response");
        if (recaptcha != null) {
            final String captchaUri = "https://www.google.com/recaptcha/api/siteverify";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", CAPTCHA_SECRET);
            map.add("response", recaptcha);
            map.add("remoteip", "127.0.0.1");
            HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(map, headers);

            Map<String, Object> result = restTemplate.postForObject(captchaUri, req, HashMap.class);
            Boolean validCaptcha = (Boolean) result.get("success");
            if (!validCaptcha) {
                throw new InvalidCaptchaException();
            }
        }
        if (!checkPasswordLength(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = null;
        Optional<User> userExist = userRepository.findOneByLogin(managedUserVM.getLogin().toLowerCase());
        if (userExist.isPresent()) {
            user = userExist.get();
            if (user.getActivated()) {
                throw new LoginAlreadyUsedException();
            } else {
                userRepository.findOneByEmailIgnoreCaseAndIdNot(managedUserVM.getEmail(), user.getId()).ifPresent(u -> {throw new EmailAlreadyUsedException();});
                userRepository.findOneByMobileIgnoreCaseAndIdNot(managedUserVM.getMobile(), user.getId()).ifPresent(u -> {throw new MobileAlreadyUsedException();});
                user = userService.reRegisterUser(managedUserVM, user, managedUserVM.getPassword());
            }
        } else {
            userRepository.findOneByEmailIgnoreCase(managedUserVM.getEmail()).ifPresent(u -> {throw new EmailAlreadyUsedException();});
            userRepository.findOneByMobileIgnoreCase(managedUserVM.getMobile()).ifPresent(u -> {throw new MobileAlreadyUsedException();});
            user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        }
        mailService.sendActivationEmail(user);
        smsService.sendActivationSms(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * GET  /activate : activate the registered user.
     *
     * @param key the activation key
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    @Timed
    public ResponseEntity<Object> activateAccount(@RequestParam(value = "key") String key, @RequestParam(value = "login") String login) {
        Optional<User> user = userService.activateRegistration(key, login);
        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this reset key");
        }
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
    }

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the current user
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    public Map<String, Object> getAccount() {
        Map<String, Object> account = new HashMap<>();
        if (userService.getUserWithAuthorities().isPresent()) {
            User user = userService.getUserWithAuthorities().get();
            account.put("id", user.getId());
            account.put("login", user.getLogin());
            account.put("firstName", user.getFirstName());
            account.put("lastName", user.getLastName());
            account.put("email", user.getEmail());
            account.put("mobile", user.getMobile());
            account.put("activated", user.getActivated());
            account.put("termAgreed", user.getTermAgreed());
            account.put("imageUrl", user.getImageUrl());
            account.put("langKey", user.getLangKey());
            account.put("createdBy", user.getCreatedBy());
            account.put("createdDate", user.getCreatedDate());
            account.put("lastModifiedBy", user.getLastModifiedBy());
            account.put("lastModifiedDate", user.getLastModifiedDate());

            Set<Authority> authoritySet = user.getAuthorities();
            Set<String> authoritiesSet = new HashSet<>();
            Map<String, Menu> menuCollection = new HashMap<>();
            Map<String, List<String>> authMenuMap = new HashMap<>();
            List<Menu> menuList = new ArrayList<>();
            for (Authority authority:authoritySet) {
                authoritiesSet.add(authority.getName());
                Authority authMenu = authorityRepository.findOneWithMenusByName(authority.getName()).get();
                List<String> menuIds = new ArrayList<>();
                for (Menu menu:authMenu.getMenus()) {
                    menuIds.add(menu.getId());
                    if (!menuCollection.containsKey(menu.getId())) {
                        menuCollection.put(menu.getId(), menu);
                        menuList.add(menu);
                    }
                }
                authMenuMap.put(authMenu.getName(), menuIds);
            }
            account.put("authorities", authoritiesSet);
            account.put("menus", menuList);
            account.put("authMenus", authMenuMap);
        }
        return account;
    }

    /**
     * POST  /account : update the current user information.
     *
     * @param userDTO the current user information
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws RuntimeException 500 (Internal Server Error) if the user login wasn't found
     */
    @PostMapping("/account")
    @Timed
    public ResponseEntity<Object> saveAccount(@Valid @RequestBody UserDTO userDTO) {
        final String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new InternalServerErrorException("Current user login not found"));
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (!user.isPresent()) {
            throw new InternalServerErrorException("User could not be found");
        }
        userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(), userDTO.getMobile(),
            userDTO.getLangKey(), userDTO.getImageUrl());
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
   }

    @PostMapping(path = "/account/agree-on-term")
    @Timed
    public ResponseEntity<Object> agreeOnTerm() {
        userService.setTermAgreed(true);
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
    }

    @PostMapping(path = "/account/setActive")
    @Timed
    public ResponseEntity<Object> setActive(@RequestParam(value = "login") String login, @RequestParam(value = "active") boolean active) {
        userService.setUserActive(login, active);
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
    }

    /**
     * POST  /account/change-password : changes the current user's password
     *
     * @param password the new password
     * @throws InvalidPasswordException 400 (Bad Request) if the new password is incorrect
     */
    @PostMapping(path = "/account/change-password")
    @Timed
    public ResponseEntity<Object> changePassword(@RequestBody String password) {
        if (!checkPasswordLength(password)) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(password);
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
   }

    /**
     * POST   /account/reset-password/init : Send an email to reset the password of the user
     *
     * @param params reset parameter, either using phone or mail
     * @throws EmailNotFoundException 400 (Bad Request) if the email address is not registered
     */
    @PostMapping(path = "/account/reset-password/init")
    @Timed
    public ResponseEntity<Object> requestPasswordReset(@RequestBody Map<String, String> params) {
        String sendTo = params.get("sendTo");
        String value = params.get("value");
        if (sendTo.equalsIgnoreCase("email")) {
            mailService.sendPasswordResetMail(
                userService.requestPasswordResetMail(value)
                    .orElseThrow(EmailNotFoundException::new)
            );
        } else {
            smsService.sendPasswordResetSms(
                userService.requestPasswordResetSms(value)
                    .orElseThrow(MobileNotFoundException::new)
            );
        }
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
    }

    @PostMapping(path = "/account/reset-password/validateKey")
    @Timed
    public ResponseEntity<Object> validatePasswordReset(@RequestBody Map<String, String> params) {
        String resetKey = params.get("key");
        Optional<User> user =
            userService.validatePasswordReset(resetKey);

        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this reset key");
        }
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
    }

    /**
     * POST   /account/reset-password/finish : Finish to reset the password of the user
     *
     * @param keyAndPassword the generated key and the new password
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws RuntimeException 500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset-password/finish")
    @Timed
    public ResponseEntity<Object> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user =
            userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new InternalServerErrorException("No user was found for this reset key");
        }
        return new ResponseEntity<>(new HashMap<String, Object>(), HttpStatus.OK);
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
}
