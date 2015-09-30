package com.martiansoftware.martifacts.model;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class Tags {

    /**
     * Normalizes a tag to its canonical form (trimmed, lowercase, and whitespace-free)
     * @param tag the tag to normalize
     * @return the normalized tag
     */
    public static String normalize(String tag) {
        return tag.trim().replaceAll("\\s", "_").toLowerCase();
    }
    
    /**
     * Normalizes an entire collection of tags, preserving order
     * @param tags the tags to normalize
     * @return the normalized tag collection
     */
    public static Collection<String> normalize(Collection<String> tags) {
        return tags.stream().map(t -> normalize(t)).filter(t -> t.length() > 0).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
