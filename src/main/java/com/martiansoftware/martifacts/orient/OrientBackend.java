package com.martiansoftware.martifacts.orient;

import com.martiansoftware.martifacts.model.Tags;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.martiansoftware.martifacts.orient.OrientSupport.db;
import com.orientechnologies.orient.core.id.ORID;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * based on examples from https://groups.google.com/forum/#!msg/orient-database/UjDJPtmvJDc/TaAqtxvUMMgJ
 * @author mlamb
 */
class OrientBackend extends OrientSupport {

    private static final Logger log = LoggerFactory.getLogger(OrientBackend.class);
    
    // TODO: fewer string magic text.  maybe a template processor inside sql
    //       calls?  need to avoid just concatenating strings.
    public OrientBackend(Path path) throws IOException {
        super(path, true);

        noTx(() -> {            
            if (db().getMetadata().getSchema().getClass("Tag") == null) {
                log.info("Defining schema");
//                sql("alter database custom useLightweightEdges=false");
                sql("create class Tag");
                sql("create property Tag.name string");
                sql("alter property Tag.name MANDATORY true");
                sql("create index Tag.name NOTUNIQUE");

                sql("create class Artifact");
                
                sql("create property Artifact.uuid string");
                sql("alter property Artifact.uuid MANDATORY true");
                sql("create index Artifact.uuid UNIQUE");
                
                sql("create property Artifact.name string");
                sql("create index Artifact.name NOTUNIQUE");
                
                sql("create property Artifact.sha1 string"); // TODO: more indexes and mandatories (and validations?)
                sql("create index Artifact.sha1 NOTUNIQUE");
                
                sql("create property Artifact.size long");
                sql("create property Artifact.time datetime");
                sql("create property Artifact.added datetime");
                
                sql("create property Artifact.tags linkset Tag");
                sql("create index Artifact.tags NOTUNIQUE");
                
                log.info("Finished defining schema.\n");
            } else log.info("Schema already defined.\n");
        });
    }

    /**
     * Returns a Tag ODocument for each (unique, normalized) tag in the specified
     * collection, creating them as necessary
     * 
     * @param tags the tags to return or create
     * @return the ODocuments for each (unique, normalized) requested tag
     */
    private Set<ODocument> getOrCreateTagsFor(Collection<String> tags) {
        if (tags.isEmpty()) return Collections.EMPTY_SET;
        Set<ODocument> result = new java.util.LinkedHashSet<>();
        Tags.normalize(tags).stream().forEach(
            tag -> result.add(sql("UPDATE Tag SET name = ? UPSERT RETURN AFTER @this WHERE name = ? ", tag, tag).get(0))
        );
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns a Tag ODocument for each (unique, normalized) tag in the specified
     * collection that is defined in the database
     * 
     * @param tags the tags to return
     * @return the ODocuments for each (unique, normalized) requested tag defined in the db
     */
    List<ODocument> findTagDocsFor(Collection<String> tags) {
        return tx(() -> {
            if (tags.isEmpty()) return Collections.EMPTY_LIST;
            Collection<String> ntags = Tags.normalize(tags);
            String q = "select from Tag where " + ntags.stream().map(t -> "(name = ?) OR (name like ?)").collect(Collectors.joining("OR "));            
            return Collections.unmodifiableList(sql(q, ntags.stream().flatMap(s -> Stream.of(s, "%:" + s)))); // e.g. find tag "version:2" for query term "2"
        });
    }

    /**
     * Adds links to the (unique, normalized) collection of tags to the specified
     * Artifact document, creating any new tag entries as necessary
     * 
     * @param artifactDoc the doc to add the tags to
     * @param tags the tags to add
     */
    void addTagsToArtifactDoc(ODocument artifactDoc, Collection<String> tags) {
        if (tags.isEmpty()) return;
        Collection<String> ntags = Tags.normalize(tags);
        tx(() -> {
            Set<ODocument> tagLinks = artifactDoc.field("tags");
            getOrCreateTagsFor(ntags).forEach(d -> tagLinks.add(d));
            artifactDoc.save();
        });
    }

    void deleteUnusedTags() {
        tx(() -> {
            // this seems much more complicated than it needs to be.  would prefer a single sql statement
            Set<ORID> usedTags = new java.util.HashSet<>();
            sql("select tag from (select tags as tag from artifact unwind tag) group by tag")
                .forEach(d -> usedTags.add(((ODocument) d.field("tag")).getIdentity()));
            sql("select from tag")
                .forEach(d -> { if (!usedTags.contains(d.getIdentity())) d.delete(); });
        });
    }

    /**
     * Removes links to the (unique, normalized) collection of tags from the specified
     * Artifact document
     * 
     * @param artifactDoc the doc to remove the tags from
     * @param tags the tags to remove
     */
    void removeTagsFromArtifact(ODocument artifact, Collection<String> tags) {
        if (tags.isEmpty()) return;
        Collection<String> ntags = Tags.normalize(tags);
        tx(() -> {
            Set<ODocument> tagLinks = artifact.field("tags");
            findTagDocsFor(ntags).forEach(d -> tagLinks.remove(d));
            artifact.save();
            deleteUnusedTags();
        });
    }
//  ----------------------------------------------------------------------------

    /**
     * Returns all Tag ODocuments
     * @return all Tag ODocuments
     */
    public List<ODocument> tagDocs() {
        return Collections.unmodifiableList(tx(() -> sql("select from Tag")));
    }
    
    /**
     * Returns tags and tag counts
     * @return tags and tag counts
     */
    public List<ODocument> tagStats() {
        return Collections.unmodifiableList(tx(() -> sql("select count(*), name from (select expand(tags) from artifact) group by name;")));
    }
    
    /**
     * Creates a new Artifact ODocument with the specified name and tags, and
     * an automatically generated uuid.
     * 
     * @param name the name of the new artifact (does not have to be unique)
     * @param sha1 the sha-1 hash of the artifat being added (blob id)
     * @param size the size of the artifact in bytes
     * @param time the time to associate with the artifact
     * @param tags the tags to add to the artifact (will be created if necessary)
     * @return the newly created Artifact ODocument
     */
    public ODocument createArtifactDoc(String name, String sha1, long size, Date fileTime, Collection<String> tags) {
        return tx(() -> {
            ODocument artifact = new ODocument("Artifact");
            artifact.field("uuid", UUID.randomUUID().toString());
            artifact.field("name", name);
            artifact.field("sha1", sha1);
            artifact.field("size", size);
            artifact.field("time", fileTime);
            artifact.field("added", new Date());
            artifact.field("tags", getOrCreateTagsFor(tags));
            artifact.save();
            return artifact;
        });
    }

    /**
     * Returns all Artifact ODocuments that contain ALL tags in the specified
     * collection
     * 
     * @param tags the tags for which we want to find all associated Artifact
     *             ODocuments (only those with ALL specified tags are returned)
     * @return the Artifact ODocuments with all of the specified tags
     */
    public List<ODocument> findArtifactDocsWithAllTags(Collection<String> tags) {
        return tx(() -> {
            Collection<String> ntags = Tags.normalize(tags);
            List<ODocument> tagDocs = findTagDocsFor(tags);
            if (tagDocs.isEmpty()) return Collections.EMPTY_LIST;
            String q = "select from Artifact where " + tagDocs.stream().map(t -> "tags contains " + t.getIdentity() + " ").collect(Collectors.joining("AND "));
            return Collections.unmodifiableList(sql(q, (Object[]) ntags.toArray(new String[ntags.size()])));
        });
    }

    /**
     * Returns all Artifact ODocuments sorted by time
     * @return all Artifact ODocuments sorted by time
     */
    public List<ODocument> all() {
        return tx(() -> Collections.unmodifiableList(sql("select from Artifact order by time")));
    }

    /**
     * Returns all Artifact ODocuments with the specified hash
     * @param hash the hash we're looking for
     * @return the Artifact ODocuments with the specified hash
     */
    public List<ODocument> findArtifactDocsWithHash(String hash) {
        return tx(() -> {
            return Collections.unmodifiableList(sql("select from Artifact where sha1 = ?", hash));
        });
    }
    
    public List<ODocument> findArtifactDocsByQuery(String query) {
        OrientSearch search = new OrientSearch(query, this);
        if (search.hasNoParams()) return all();
        if (search.hasNoResults()) return Collections.EMPTY_LIST;
        return tx(() -> {
           return Collections.unmodifiableList(sql(search.sql(), search.sqlParams().toArray()));
        });
    }
    
    /**
     * Finds the Artifact ODocument with the specified id if it exists
     * @param id the id of the Artifact we're looking for
     * @return the Artifact ODocument with the specified id if it exists
     */
    public Optional<ODocument> findArtifactDocWithId(String id) {
        return tx(() -> {
           return sql("select from Artifact where uuid = ?", id).stream().findFirst();
        });
    }
}
