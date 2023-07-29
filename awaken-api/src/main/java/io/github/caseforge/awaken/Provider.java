package io.github.caseforge.awaken;

public interface Provider {

    Object provide(Object oldValue) throws Exception;
    
}
