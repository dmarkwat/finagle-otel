package com.twitter.finagle;

public class InitExtern {

    static {
        // so finagleVersion will read correctly
        Init.apply();
    }

    private InitExtern() {
    }

    public static String finagleVersion() {
        return com.twitter.finagle.Init.finagleVersion();
    }
}
