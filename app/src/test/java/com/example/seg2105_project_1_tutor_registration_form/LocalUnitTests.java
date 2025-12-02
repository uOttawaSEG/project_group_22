package com.example.seg2105_project_1_tutor_registration_form;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Simple local unit tests that run on the JVM (no Android / Firebase).
 * These are intentionally small and fast.
 */
public class LocalUnitTests {

    @Test
    public void addition_twoPlusThree_equalsFive() {
        int result = 2 + 3;
        assertEquals("2 + 3 should equal 5", 5, result);
    }

    @Test
    public void string_concatenation_buildsExpectedText() {
        String hello = "Hello";
        String world = "World";
        String combined = hello + " " + world;

        assertEquals("Hello World", combined);
    }

    @Test
    public void arrayList_addItems_updatesSize() {
        ArrayList<String> list = new ArrayList<>();
        list.add("SEG2105");
        list.add("OTAMS");

        assertEquals("List should contain 2 items after adds", 2, list.size());
        assertTrue("List should contain 'SEG2105'", list.contains("SEG2105"));
    }

    @Test
    public void hashMap_putAndGet_returnsStoredValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("students", 4);

        assertTrue("Map should contain key 'students'", map.containsKey("students"));
        assertEquals("Value for 'students' should be 4",
                Integer.valueOf(4), map.get("students"));
    }
}
