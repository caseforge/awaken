/**
 * 
 */
package io.github.caseforge.awaken.validators;

import io.github.caseforge.awaken.validation.ForceValidator;
import io.github.caseforge.awaken.validation.ValidationException;

/**
 * 
 */
public class RequiredValidator implements ForceValidator {

    private String message;
    
    @Override
    public void validate(Object target) throws Exception {
        if (target == null) {
            throw new ValidationException(message);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
