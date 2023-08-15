/**
 * 
 */
package io.github.caseforge.awaken.validators;

import java.lang.reflect.Array;
import java.util.Collection;

import io.github.caseforge.awaken.Validator;
import io.github.caseforge.awaken.validation.ValidationException;

/**
 * 
 */
public class LengthValidator implements Validator {

    private String message;

    private Integer maxLength;

    private Integer minLength;

    @Override
    public void validate(Object target) throws Exception {
        int len = lengthOf(target);

        if (maxLength != null && len > maxLength) {
            throw new ValidationException(message);
        }

        if (minLength != null && len < minLength) {
            throw new ValidationException(message);
        }

    }

    private int lengthOf(Object target) {
        if (target instanceof String) {
            return ((String) target).length();
        }

        if (target.getClass().isArray()) {
            return Array.getLength(target);
        }

        if (target instanceof Collection) {
            return ((Collection<?>) target).size();
        }
        return 0;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
