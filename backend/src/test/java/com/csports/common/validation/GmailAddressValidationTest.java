package com.csports.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.csports.auth.dto.RegisterRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class GmailAddressValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void registrationAcceptsGmailAddressesCaseInsensitively() {
        RegisterRequest request = new RegisterRequest(
                "Abdelrahman",
                "Abdelrahman.Gomaa+csports@GMAIL.COM",
                "01150480561",
                "A.gomaa_2004",
                22);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void registrationRejectsNonGmailAddresses() {
        RegisterRequest request = new RegisterRequest(
                "Abdelrahman",
                "abdelrahman@example.com",
                "01150480561",
                "A.gomaa_2004",
                22);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(GmailAddress.MESSAGE);
    }
}
