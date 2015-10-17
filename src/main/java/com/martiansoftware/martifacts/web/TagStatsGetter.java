package com.martiansoftware.martifacts.web;

import com.martiansoftware.boom.Boom;
import com.martiansoftware.boom.MimeType;
import com.martiansoftware.martifacts.model.ArtifactStore;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class TagStatsGetter {
    
    private final ArtifactStore _store;

    public TagStatsGetter(ArtifactStore store) { _store = store; }

    public Object tagstats() {
        Map<String, Long> result = _store.tagStats();

        if (MimeType.JSON == Boom.preferredEncodingOf(MimeType.JSON, MimeType.TEXT)) {
            return Boom.json(result);
        } else {
            return Boom.text(result.entrySet().stream().map(e -> String.format("%s: %d\n", e.getKey(), e.getValue())).collect(Collectors.joining()));
        }

    }
}
