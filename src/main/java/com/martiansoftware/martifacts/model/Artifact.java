package com.martiansoftware.martifacts.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public interface Artifact {
    
    /**
     * Returns this Artifact's unique ID
     * @return this Artifact's unique ID
     */
    public String id();
    
    /**
     * Returns this Artifact's name (does NOT have to be unique!)
     * @return this Artifact's name (does NOT have to be unique!)
     */
    public String name();
    
    /**
     * Returns this Artifact's hash (aka blob id: does NOT have to be unique!)
     * @return this Artifact's hash (aka blob id: does NOT have to be unique!)
     */
    public String hash();
    
    /**
     * Returns this Artifact's size, in bytes
     * @return this Artifact's size, in bytes
     */
    public long size();
    
    /**
     * Returns the time of the Artifact (e.g., time file was created or last modified)
     * @return the time of the Artifact (e.g., time file was created or last modified)
     */
    public Date time();

    /**
     * Returns the time at which the Artifact was added to the ArtifactStore
     * @return the time at which the Artifact was added to the ArtifactStore
     */    
    public Date timeAdded();
    
    /**
     * Returns all of this Artifact's tags
     * @return all of this Artifact's tags
     */
    public SortedSet<String> tags();
    
    /**
     * Adds the specified tags to this Artifact
     * @param tags the tags to add
     * @return this Artifact
     */
    public Artifact tag(Collection<String> tags);

    /**
     * Adds the specified tags to this Artifact
     * @param tags the tags to add
     * @return this Artifact
     */
    public default Artifact tag(String... tags) { return tag(Arrays.asList(tags)); }
    
    /**
     * Removes the specified tags from this Artifact
     * @param tags the tags to remove
     * @return this Artifact
     */
    public Artifact untag(Collection<String> tags);

    /**
     * Removes the specified tags from this Artifact
     * @param tags the tags to remove
     * @return this Artifact
     */
    public default Artifact untag(String... tags) { return untag(Arrays.asList(tags)); }
    
    /**
     * Returns this Artifact's data
     * @return this Artifact's data
     * @throws IOException 
     */
    public InputStream inputStream() throws IOException;
    
    /**
     * Stores attributes in a map for easy json-ing
     * @return a Map representation of this Artifact
     */
    public default Map<String, Object> asMap() {
        Map<String, Object> result = new java.util.TreeMap<>();
        result.put("id", id());
        result.put("sha1", hash());
        result.put("name", name());
        result.put("size", size());
        result.put("tags", tags());
        result.put("addedtime", timeAdded());
        result.put("filetime", time());
        return result;
    }

}
