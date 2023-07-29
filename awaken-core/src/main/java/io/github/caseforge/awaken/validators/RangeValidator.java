/**
 * 
 */
package io.github.caseforge.awaken.validators;

import io.github.caseforge.awaken.Validator;
import io.github.caseforge.awaken.validation.ValidationException;

/**
 * 
 */
public class RangeValidator implements Validator {

    private String message;

    private Double max;

    private Double min;

    @Override
    public void validate(Object target) throws Exception {
        double v = ((Number) target).doubleValue();
        if (max != null && max < v) {
            throw new ValidationException(message);
        }
        
        if (min != null && min > v) {
            throw new ValidationException(message);
        }
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
