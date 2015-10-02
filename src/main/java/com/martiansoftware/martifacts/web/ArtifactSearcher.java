package com.martiansoftware.martifacts.web;

import com.martiansoftware.martifacts.model.ArtifactStore;
import static com.martiansoftware.boom.Boom.q;
import com.martiansoftware.boom.BoomResponse;
import com.martiansoftware.martifacts.model.Artifact;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author mlamb
 */
public class ArtifactSearcher {

    private final ArtifactStore _store;
    public ArtifactSearcher(ArtifactStore store) { _store = store; }

    private static final Pattern ID_PATTERN = Pattern.compile("^\\p{XDigit}{8}-(?:\\p{XDigit}{4}-){3}\\p{XDigit}{12}$");
    private static final Pattern SHA1_PATTERN = Pattern.compile("^\\p{XDigit}{40}$");
    
    public Object search() {
        Collection<String> queryIds = new java.util.HashSet<>();
        Collection<String> queryHashes = new java.util.HashSet<>();
        Collection<String> queryTags = new java.util.HashSet<>();
        
        // parse query string (in parameter "q") to ids, hashes, and tags
        q("q").ifPresent(q -> 
            Arrays.asList(q.split("\\s+"))
                .stream().filter(s -> !s.isEmpty())
                .forEach(
                    w -> {
                        if (ID_PATTERN.matcher(w).matches()) queryIds.add(w);
                        else if (SHA1_PATTERN.matcher(w).matches()) queryHashes.add(w);
                        else queryTags.add(w);
                    }
                )
        );
        
        // no search params means "give me everything!"
        if (queryHashes.isEmpty() && queryIds.isEmpty() && queryTags.isEmpty()) return ArtifactResponse.of(_store.all());
        
        // search by hash or ID means "include all matches in result"  (then filtered to only include any specified tags)
        if (!queryHashes.isEmpty() || !queryIds.isEmpty()) {
            Map<String, Artifact> resultsById = new java.util.HashMap<>();
            
            Stream.concat( // all artifacts satisfying any of the id or hash query parameters
                queryHashes.stream().flatMap(h -> _store.findByHash(h).stream()),
                queryIds.stream().map(i -> _store.findById(i)).filter(oi -> oi.isPresent()).map(oa -> oa.get())
            ).forEach(a -> resultsById.put(a.id(), a));
            
            if (queryTags.isEmpty()) {
                return ArtifactResponse.of(resultsById.values());
            } else { // use tags as a filter
                Set<String> idsWithTags = _store.findByTags(queryTags).stream().map(a -> a.id()).collect(Collectors.toCollection(java.util.HashSet::new));
                System.out.format("found %d tag matches\n", idsWithTags.size());
                return ArtifactResponse.of(resultsById.entrySet().stream().filter(e -> idsWithTags.contains(e.getKey())).map(e -> e.getValue()).collect(Collectors.toList()));
            }
        }
        
        // no hashes or ids specified, but tags are.  in this case they're not a filter - just return all matches
        return ArtifactResponse.of(_store.findByTags(queryTags));
    }

}
