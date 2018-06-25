package com.leanplum.models;

import java.util.HashMap;
import java.util.Map;

public class ABTest {
    private String id;
    private String variantId;
    private Map<String, Object> vars;

    public ABTest() {
    }

    public ABTest(String id, String variantId, Map<String, Object> vars) {
        this.id = id;
        this.variantId = variantId;
        this.vars = vars;
    }

    //todo: move to framework
    public ABTest(Map<String, Object> dict) {
        Map<String, Object> vars = (Map<String, Object>) dict.get("vars");
        this.id = (String) dict.get("id");
        this.variantId = (String) dict.get("variantId");
        this.vars = vars;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVariantId() {
        return id;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }

    //todo: move to framework
    public Map<String, Object> asDictionary() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("variantId", variantId);
        dict.put("vars", vars);
        return dict;
    }

}
