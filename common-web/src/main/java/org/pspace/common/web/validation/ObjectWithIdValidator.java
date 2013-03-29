package org.pspace.common.web.validation;

import org.pspace.common.api.ObjectWithID;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Created at 17.11.11 - 23:30
 *
 * @author peach
 */
public class ObjectWithIdValidator implements ConstraintValidator<ValidObjectWithId, ObjectWithID>{
    @Override
    public void initialize(ValidObjectWithId constraintAnnotation) {

    }

    @Override
    public boolean isValid(ObjectWithID value, ConstraintValidatorContext context) {
        // TODO
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
