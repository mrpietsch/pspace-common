package org.pspace.common.web.validation;

import org.iban4j.IbanFormatException;
import org.iban4j.IbanUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IbanValidator implements ConstraintValidator<Iban, String> {

    private boolean empty;
    private boolean nullable;

    @Override
    public void initialize(Iban constraintAnnotation) {
        empty = constraintAnnotation.empty();
        nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return nullable;
        if (value.isEmpty()) return empty;
        try {
            IbanUtil.validate(value);
            return true;
        } catch (IbanFormatException e) {
            return false;
        }
    }
}
