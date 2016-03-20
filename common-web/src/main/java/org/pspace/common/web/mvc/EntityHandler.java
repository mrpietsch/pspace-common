package org.pspace.common.web.mvc;

import java.lang.annotation.*;

/**
 * @author peach
 * @deprecated use spring-hateoas
 */
@Target({ElementType.METHOD})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface EntityHandler {

    Class<?> entity();

    org.pspace.common.web.mvc.EntityOperation operation() default org.pspace.common.web.mvc.EntityOperation.READ;

}
