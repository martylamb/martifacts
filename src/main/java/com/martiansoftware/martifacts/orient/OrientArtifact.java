package com.martiansoftware.martifacts.orient;

import com.martiansoftware.martifacts.model.Artifact;
import com.martiansoftware.martifacts.web.AppRootHelper;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class OrientArtifact implements Artifact {
    
    private final OrientArtifactStore _store;
    private final OrientBackend _backend;
    private final ODocument _doc;

    OrientArtifact(OrientArtifactStore store, OrientBackend backend, ODocument doc) {
        _store = store;
        _backend = backend;
        _doc = doc;
    }

    @Override public String id() { return _doc.field("uuid"); }
    @Override public String name() { return _doc.field("name"); }  
    @Override public String hash() { return _doc.field("sha1"); }
    @Override public long size() { return _doc.field("size"); }
    @Override public Date time() { return _doc.field("time"); }
    @Override public Date timeAdded() { return _doc.field("added"); }

    @Override public SortedSet<String> tags() {
        return _backend.tx(() -> {
            Set<OIdentifiable> tagdocs = _doc.field("tags");
            return tagdocs
                .stream()
                .map(i -> (ODocument) i.getRecord())
                .map(d -> (String) d.field("name"))
                .collect(Collectors.toCollection(TreeSet::new));
        });
    }

    @Override public OrientArtifact tag(Collection<String> tags) {
        _backend.addTagsToArtifactDoc(_doc, tags);
        return this;
    }

    @Override public OrientArtifact untag(Collection<String> tags) {
        _backend.removeTagsFromArtifact(_doc, tags);
        return this;
    }
    
    @Override public InputStream inputStream() throws IOException {
        Optional<InputStream> oi = _store.getInputStreamForHash(hash());
        if (!oi.isPresent()) throw new IOException("No InputStream available for Artifact!");
        return oi.get();
    }
        
    @Override public String toString() {
        SortedSet tags = tags();
        StringBuilder s = new StringBuilder();
        s.append(String.format("%s %s [%s] [%s]\n", getClass().getSimpleName(), _doc.getIdentity(), name(), id()));
        s.append(String.format("   tags: %s", tags.stream().collect(Collectors.joining(", "))));        
        s.append(String.format("   hash: %s\n", hash()));
        s.append(String.format("   size: %d\n", size()));
        s.append(String.format("   time: %s\n", time()));
        s.append(String.format("  added: %s\n", timeAdded()));
        return s.toString();
    }
//    @Override public String toString() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//        return String.format("%s %d %s %s %s\n",
//                                id(),
//                                size(),
//                                sdf.format(time()),
//                                name(),
//                                tags().stream().map(s -> "#" + s).collect(Collectors.joining(" ")));
//    }

}
