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

/** 8-72 characters (BCrypt ignores everything after 72 bytes), at least one letter and one digit. */
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
