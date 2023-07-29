package io.github.caseforge.awaken.assignment;

import java.util.List;

public interface Node extends Assignment {

    String getName();
    
    List<Node> getChildren();
    
    void assign(Object target) throws Exception;
}
