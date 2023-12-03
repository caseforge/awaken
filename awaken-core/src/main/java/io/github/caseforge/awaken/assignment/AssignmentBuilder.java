package io.github.caseforge.awaken.assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.caseforge.awaken.Provider;
import io.github.caseforge.awaken.ResourceProvider;
import io.github.caseforge.awaken.util.BeanUtil;

public class AssignmentBuilder {

    private SingletonNode rootNode = new SingletonNode();

    private Map<String, AbstractNode> nodeMap = new HashMap<String, AbstractNode>();

    private JsonObject config;

    private ResourceProvider resourceProvider;

    private Gson gson = new Gson();

    public static Assignment build(JsonObject config, ResourceProvider resourceProvider) throws Exception {
        if (config == null) {
            return null;
        }
        AssignmentBuilder builder = new AssignmentBuilder();
        builder.config = config;
        builder.resourceProvider = resourceProvider;
        builder.build();
        return builder.rootNode;
    }

    public AssignmentBuilder() {
        rootNode.setName("");
        this.nodeMap.put("", rootNode);
    }

    public void build() throws Exception {
        Set<String> keys = config.keySet();
        for (String key : keys) {
            AbstractNode node = createNodeIfAbsent(key);
            JsonElement jsonElement = config.get(key);
            Provider provider = buildProvider(jsonElement);
            node.setProvider(provider);
        }
    }

    public Provider buildProvider(JsonElement jsonElement) throws Exception {
        if (jsonElement.isJsonNull()) {
            return null;
        }

        if (jsonElement instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) jsonElement;
            JsonElement providerTypeElement = jsonObject.get("&");
            
            if (providerTypeElement == null) {
                throw new Exception("Please use & to specify the provider type in " + jsonElement);
            }
            
            String type = providerTypeElement.getAsString();
            
            jsonObject.remove("@");
            
            Provider bean = null;
            
            try {
                bean = (Provider) resourceProvider.getBean(type);
            } catch (Exception e) {
                bean = (Provider) resourceProvider.getBean(type + "Provider");
            }
            
            Provider newBean = gson.fromJson(jsonObject, bean.getClass());

            BeanUtil.copy(newBean, bean);

            return bean;
        }

        if (jsonElement.isJsonPrimitive()) {
            String beanName = jsonElement.getAsString();
            return (Provider) resourceProvider.getBean(beanName);
        }

        throw new Exception("error in config " + jsonElement.getAsString());
    }

    public AbstractNode createNodeIfAbsent(String path) {
        if (nodeMap.containsKey(path)) {
            return nodeMap.get(path);
        }

        String nodeName = nodeName(path);
        String parentPath = parentPath(path);

        AbstractNode parent = createNodeIfAbsent(parentPath);
        List<Node> children = parent.getChildren();
        if (children == null) {
            children = new ArrayList<Node>();
            parent.setChildren(children);
        }

        AbstractNode node = null;

        boolean multiNode = isMultipleNode(nodeName);
        if (multiNode) {
            node = new MultipleNode();
        } else {
            node = new SingletonNode();
        }

        node.setName(nodeName);
        children.add(node);

        return node;
    }

    public String formatPath(String path) {
        path = path.replaceAll("/+", "/");
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public String nodeName(String path) {
        int splitIndex = path.lastIndexOf('/');
        if (splitIndex == -1) {
            return path;
        }

        return path.substring(splitIndex + 1);
    }

    public String parentPath(String path) {
        int splitIndex = path.lastIndexOf('/');
        if (splitIndex == -1) {
            return "";
        }
        return path.substring(0, splitIndex);
    }

    public boolean isMultipleNode(String nodeName) {
        return "*".equals(nodeName);
    }
}
