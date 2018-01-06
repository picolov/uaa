package com.baswara.uaa.web.rest.errors;

public class MobileAlreadyUsedException extends BadRequestAlertException {

    public MobileAlreadyUsedException() {
        super(ErrorConstants.MOBILE_ALREADY_USED_TYPE, "Mobile phone already in use", "userManagement", "mobileexists");
    }
}
