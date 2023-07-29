package io.github.caseforge.awaken.assignment;

import java.util.Arrays;
import java.util.Iterator;

import io.github.caseforge.awaken.util.BeanUtil;

public class MultipleNode extends AbstractNode {

    public void assign(Object target) throws Exception {
        Object oldValue = BeanUtil.get(target, name);
        Object newValue = null;
        if (provider != null) {
            newValue = provider.provide(oldValue);
            BeanUtil.set(target, name, newValue);
        } else {
            newValue = oldValue;
        }

        if (children != null && newValue != null) {
            for (Node child : children) {
                child.assign(newValue);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    protected Iterator toIterator(Object target) {
        if (target instanceof Iterable) {
            return ((Iterable) target).iterator();
        } else if (target.getClass().isArray()) {
            return Arrays.asList(target).iterator();
        }
        throw new UnsupportedOperationException("*");
    }
}
