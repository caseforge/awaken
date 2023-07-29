package io.github.caseforge.awaken.validation;

import java.util.List;

public interface Node extends Validation {

    String getName();
    
    List<Node> getChildren();
    
}
