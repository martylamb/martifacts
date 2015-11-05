package com.martiansoftware.martifacts.web;

import com.martiansoftware.boom.Boom;
import static com.martiansoftware.boom.Boom.json;
import static com.martiansoftware.boom.Boom.text;
import com.martiansoftware.boom.BoomResponse;
import com.martiansoftware.boom.MimeType;
import com.martiansoftware.martifacts.model.Artifact;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class ArtifactResponse {
    
    public static BoomResponse of(Artifact artifact) {
        return of(artifact == null ? Collections.EMPTY_LIST : Arrays.asList(artifact));
    }
    
    public static BoomResponse of(Optional<Artifact> oartifact) {
        return oartifact.map(a -> of(a)).orElse(of((Artifact) null));
    }
    
    public static BoomResponse of(Collection<Artifact> artifacts) {
        MimeType mt = Boom.preferredEncodingOf(MimeType.JSON, MimeType.TEXT);        
        return mt == MimeType.JSON ? json(artifacts) : text(artifacts);
    }
    
    private Map<String, Object> asMap(Artifact a) {
        Map<String, Object> result = a.asMap();
        result.put("url", AppRootHelper.get().map(url -> String.format("%s/get/%s", url, a.id())).orElse(null));
        return result;
    }
    
    private static BoomResponse json(Collection<Artifact> artifacts) {
        return Boom.json(artifacts.stream().map(a -> new ArtifactWithUrl(a).asMap()).collect(Collectors.toList()));
    }
    
    private static BoomResponse text(Collection<Artifact> artifacts) {
        Collection<ArtifactWithUrl> au = artifacts.stream().map(a -> new ArtifactWithUrl(a)).collect(Collectors.toList());
        return Boom.text(ArtifactTextFormatter.format(au));
    }
}
