package org.pspace.common.web.validation;

import org.pspace.common.web.mvc.ObjectWithIdEditor;

import javax.validation.Constraint;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=ObjectWithIdValidator.class)
public @interface ValidObjectWithId {
}
