package io.github.caseforge.awaken.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.caseforge.awaken.ResourceProvider;
import io.github.caseforge.awaken.Validator;
import io.github.caseforge.awaken.util.BeanUtil;

public class ValidationBuilder {

    private SingletonNode rootNode = new SingletonNode();

    private Map<String, AbstractNode> nodeMap = new HashMap<String, AbstractNode>();

    private JsonObject config;

    private ResourceProvider resourceProvider;

    private Gson gson = new Gson();

    public ValidationBuilder() {
        rootNode.setName("");
        nodeMap.put("", rootNode);
    }

    public static Validation build(JsonObject config, ResourceProvider resourceProvider) throws Exception {
        ValidationBuilder builder = new ValidationBuilder();
        builder.config = config;
        builder.resourceProvider = resourceProvider;
        builder.build();
        return builder.rootNode;
    }

    public void build() throws Exception {
        Set<String> keys = config.keySet();
        for (String key : keys) {
            AbstractNode node = createNodeIfAbsent(key);
            JsonArray jsonArray = config.get(key).getAsJsonArray();
            List<Validator> validators = buildValidators(jsonArray);
            node.setValidators(validators);
        }
    }

    private List<Validator> buildValidators(JsonArray jsonArray) throws Exception {
        if (jsonArray == null) {
            return null;
        }
        List<Validator> validators = new ArrayList<Validator>();
        for (int i = 0, len = jsonArray.size(); i < len; i++) {
            JsonObject validatorConfig = jsonArray.get(i).getAsJsonObject();
            JsonElement validatorTypeElement = validatorConfig.get("&");
            
            if (validatorTypeElement == null) {
                throw new Exception("Please use & to specify the validator type in " + validatorConfig);
            }
            
            String type = validatorTypeElement.getAsString();

            Validator bean = null;
            
            try {
                bean = (Validator) resourceProvider.getBean(type);
            } catch (Exception e) {
                bean = (Validator) resourceProvider.getBean(type + "Validator");
            }

            Validator newBean = gson.fromJson(validatorConfig, bean.getClass());

            BeanUtil.copy(newBean, bean);

            validators.add(bean);
        }
        return validators;
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
