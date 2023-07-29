package io.github.caseforge.awaken;

public interface FaultHandler {

    Fault handle(Throwable t);
    
}
