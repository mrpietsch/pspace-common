package org.pspace.common.web.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=IbanValidator.class)
public @interface Iban {

    String message() default "Invalid IBAN";

    boolean nullable() default true;

    boolean empty() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
