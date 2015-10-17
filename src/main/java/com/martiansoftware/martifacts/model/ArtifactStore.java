package com.martiansoftware.martifacts.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 *
 * @author mlamb
 */
public interface ArtifactStore {
    
    /**
     * Get all tags in the db
     * @return all tags in the db
     */
    public SortedSet<String> tags();

    /**
     * Get all tags in the db, along with the number of artifacts using each.
     * @return a map of tags to tag counts
     */
    public SortedMap<String, Long> tagStats();
    
    /**
     * Creates a new Artifact with the specified name and tags
     * @param name the name of the new Artifact
     * @param file a file containing the artifact itself
     * @param tags the tags to assign to the artifact
     * @return the newly created Artifact
     * @throws IOException if the Artifact cannot be created
     */
    public Artifact create(String name, Path file, Collection<String> tags) throws IOException;

    /**
     * Creates a new Artifact with the specified name and tags
     * @param name the name of the new Artifact
     * @param data an InputStream containing the artifact data itself
     * @param fileTime the file timestamp to associate with the data (e.g., created or last modified)Date
     * @param tags the tags to assign to the artifact
     * @return the newly created Artifact
     * @throws IOException if the Artifact cannot be created
     */
    public Artifact create(String name, InputStream data, Date fileTime, Collection<String> tags) throws IOException;
    
    /**
     * Finds all Artifacts that contain all of the specified tags
     * @param tags the tags we're looking for 
     * @return all Artifacts that contain all of the specified tags
     */
    public Collection<Artifact> findByTags(Collection<String> tags);

    /**
     * Finds all Artifacts with the specified hash
     * @param hash the hash we're looking for
     * @return all Artifacts with the specified hash
     */
    public Collection<Artifact> findByHash(String hash);
    
    /**
     * Finds the Artifact with the specified id
     * @param id the id of the Artifact we're looking for
     * @return the Artifact with the specified id, if it exists
     */
    public Optional<Artifact> findById(String id);
    
    /**
     * Finds all Artifacts that match an implementation-specific query string.
     * @param searchQuery an implementation-specific query string.
     * @return all matching Artifacts
     */
    public Collection<Artifact> findByQuery(String searchQuery);
    
    /**
     * Returns all Artifacts
     * @return all Artifacts
     */
    public Collection<Artifact> all();
}
