package com.futuretech.dto.auth;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Our password rules in one place, used on register and on change-password:
 * 8-72 characters (BCrypt only uses the first 72 bytes) with at least one letter and one digit.
 * It's a "composed" constraint: the three annotations below do the actual checks.
 */
@NotBlank
@Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "must contain at least one letter and one digit")
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface StrongPassword {

    String message() default "is not a strong password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
