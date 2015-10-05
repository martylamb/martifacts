package com.martiansoftware.martifacts.web;

import com.martiansoftware.martifacts.model.ArtifactStore;
import static com.martiansoftware.boom.Boom.q;

/**
 *
 * @author mlamb
 */
public class ArtifactSearcher {

    private final ArtifactStore _store;

    public ArtifactSearcher(ArtifactStore store) { _store = store; }

    public Object search() {
        return ArtifactResponse.of(q("q").map(_store::findByQuery).orElse(_store.all()));
    }

}
