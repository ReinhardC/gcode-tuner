package com.specularity.printing;

public class TestData {
    private Double a, b;

    public Double getA() {
        return a;
    }

    public void setA(Double a) {
        this.a = a;
    }

    public Double getB() {
        return b;
    }

    public void setB(Double b) {
        this.b = b;
    }

    TestData(Double i, Double j) {
        a = i; b=j;
    }
}