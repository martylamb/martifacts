package com.martiansoftware.martifact.orient;

import com.martiansoftware.blobstore.Blob;
import com.martiansoftware.blobstore.BlobStore;
import com.martiansoftware.blobstore.Ref;
import com.martiansoftware.martifact.Artifact;
import com.martiansoftware.martifact.ArtifactStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class OrientArtifactStore implements ArtifactStore {
    
    private final OrientBackend _backend;
    private final BlobStore _blobstore;
    
    public OrientArtifactStore(Path p) throws IOException {
        _backend = new OrientBackend(p.resolve("db"));
        _blobstore = new BlobStore(p.resolve("blobs"));
    }

    @Override
    public SortedSet<String> tags() {
        return Collections.unmodifiableSortedSet(
            _backend.tagDocs().stream().map(doc -> (String) doc.field("name")).collect(Collectors.toCollection(TreeSet::new))
        );
    }

    private Artifact create(String name, Date fileTime, Blob blob, Collection<String> tags) throws IOException {
        return new OrientArtifact(this, _backend, _backend.createArtifactDoc(name, blob.ref().toString(), blob.size(), fileTime, tags));
    }
        
    @Override
    public Artifact create(String name, Path data, Collection<String> tags) throws IOException {
        if (!Files.isRegularFile(data)) throw new IOException("Cannot add " + data + ": must be a regular file");
        return create(name,
                        new Date(Files.getLastModifiedTime(data).toMillis()),
                        _blobstore.add(data.toFile()),
                        tags);
    }

    @Override
    public Artifact create(String name, InputStream data, Date fileTime, Collection<String> tags) throws IOException {
        return create(name,
                        fileTime,
                        _blobstore.add(data),
                        tags);
    }

    @Override
    public Collection<Artifact> findByTags(Collection<String> tags) {
        return Collections.unmodifiableList(
                    _backend.findArtifactDocsWithAllTags(tags)
                    .stream()
                    .map(doc -> new OrientArtifact(this, _backend, doc))
                    .collect(Collectors.toList())
        );
    }

    @Override
    public Optional<Artifact> findById(String id) {
        return _backend.findArtifactDocWithId(id).map(doc -> new OrientArtifact(this, _backend, doc));
    }

    @Override
    public Collection<Artifact> findByHash(String hash) {
        return Collections.unmodifiableList(
                    _backend.findArtifactDocsWithHash(hash)
                    .stream()
                    .map(doc -> new OrientArtifact(this, _backend, doc))
                    .collect(Collectors.toList())
        );
    }

    @Override
    public Collection<Artifact> all() {
        return Collections.unmodifiableList(
                    _backend.all()
                    .stream()
                    .map(doc -> new OrientArtifact(this, _backend, doc))
                    .collect(Collectors.toList())
        );
    }
    
    Optional<InputStream> getInputStreamForHash(String hash) throws IOException {
        Blob blob = _blobstore.get(new Ref(hash));
        return (blob == null) ? Optional.empty() : Optional.of(blob.inputStream());
    }
}
