package com.baswara.uaa.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class InvalidCaptchaException extends AbstractThrowableProblem {

    public InvalidCaptchaException() {
        super(ErrorConstants.INVALID_PASSWORD_TYPE, "Incorrect captcha", Status.BAD_REQUEST);
    }
}
