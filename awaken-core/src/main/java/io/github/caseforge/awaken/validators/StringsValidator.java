/**
 * 
 */
package io.github.caseforge.awaken.validators;

import java.util.List;

import io.github.caseforge.awaken.Validator;
import io.github.caseforge.awaken.validation.ValidationException;

/**
 * 
 */
public class StringsValidator implements Validator {

    private String message;

    private List<String> refs;

    @Override
    public void validate(Object target) throws Exception {
        if (!refs.contains(target)) {
            throw new ValidationException(message);
        }
    }

    public List<String> getRefs() {
        return refs;
    }

    public void setRefs(List<String> refs) {
        this.refs = refs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
