package com.martiansoftware.martifact.web;

import com.martiansoftware.boom.Boom;
import static com.martiansoftware.boom.Boom.json;
import static com.martiansoftware.boom.Boom.text;
import com.martiansoftware.boom.BoomResponse;
import com.martiansoftware.boom.MimeType;
import com.martiansoftware.martifact.Artifact;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    
    private static BoomResponse json(Collection<Artifact> artifacts) {
        return Boom.json(artifacts.stream().map(a -> a.asMap()).collect(Collectors.toList()));
    }
    
    private static BoomResponse text(Collection<Artifact> artifacts) {
        return Boom.text(ArtifactTextFormatter.format(artifacts));
    }
}
