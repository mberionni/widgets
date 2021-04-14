package com.miro;

public class TestUtils {

    static void msg(String m) {
        System.out.println(m);
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // nothing to do
        }
    }
}
