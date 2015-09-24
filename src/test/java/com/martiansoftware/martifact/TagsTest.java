package com.martiansoftware.martifact;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mlamb
 */
public class TagsTest {
    
    public TagsTest() {
    }
    
    @Test public void testNormalizeCollection() {
        Collection<String> tags = Tags.normalize(Arrays.asList("tag1", "hello world", "ABC", "abc  ", "    tag1"));
        assertEquals(3, tags.size());
        Iterator<String> i = tags.iterator();
        assertEquals("tag1", i.next());
        assertEquals("hello_world", i.next());
        assertEquals("abc", i.next());
    }
    
}
