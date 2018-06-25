package com.leanplum.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariantDebugInfo {
    private List<ABTest> abTests;

    public VariantDebugInfo() {
    }

    public VariantDebugInfo(List<ABTest> abTests) {
        this.abTests = abTests;
    }

    //todo: move to framework
    public VariantDebugInfo(Map<String, Object> dict) {
        List<Map<String, Object>> abTestDicts = (List<Map<String, Object>>) dict.get("abTests");
        List<ABTest> abTests = new ArrayList<>();
        for (Map<String, Object> abTestDict : abTestDicts) {
            abTests.add(new ABTest(abTestDict));
        }
        this.abTests = abTests;
    }

    public List<ABTest> getAbTests() {
        return abTests;
    }

    public void setAbTests(List<ABTest> abTests) {
        this.abTests = abTests;
    }

    //todo: move to framework
    public Map<String, Object> asDictionary() {
        List<Map<String, Object>> abTestDicts = new ArrayList<>();
        for (ABTest abTest : abTests) {
            abTestDicts.add(abTest.asDictionary());
        }
        Map<String, Object> dict = new HashMap<>();
        dict.put("abTests", abTestDicts);
        return dict;
    }

}
