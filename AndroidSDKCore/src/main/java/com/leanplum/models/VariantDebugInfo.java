package com.leanplum.models;

import java.util.List;

public class VariantDebugInfo {
    private List<ABTest> abTests;

    public VariantDebugInfo() {
    }

    public VariantDebugInfo(List<ABTest> abTests) {
        this.abTests = abTests;
    }

    public List<ABTest> getAbTests() {
        return abTests;
    }

    public void setAbTests(List<ABTest> abTests) {
        this.abTests = abTests;
    }

}
