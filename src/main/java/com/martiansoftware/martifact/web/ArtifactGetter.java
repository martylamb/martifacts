package com.martiansoftware.martifact.web;

import com.martiansoftware.martifact.ArtifactStore;
import static com.martiansoftware.boom.Boom.halt;
import static com.martiansoftware.boom.Boom.request;
import static com.martiansoftware.boom.Boom.response;
import com.martiansoftware.boom.BoomResponse;
import com.martiansoftware.boom.MimeType;
import com.martiansoftware.martifact.Artifact;
import java.io.IOException;
import java.util.Optional;

/**
 *
 * @author mlamb
 */
public class ArtifactGetter {

    private final ArtifactStore _store;
    
    public ArtifactGetter(ArtifactStore store) { _store = store; }
    
    public Object get() throws IOException {
        Optional<Artifact> oa = _store.findById(request().params(":id"));
        if (!oa.isPresent()) halt(404);
        Artifact a = oa.get();
        response().header("Content-Disposition", String.format("inline; filename=\"%s\"", a.name()));        
        return new BoomResponse(a.inputStream()).as(MimeType.BIN);
    }
}
