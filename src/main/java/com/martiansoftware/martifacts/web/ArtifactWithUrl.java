package com.martiansoftware.martifacts.web;

import com.martiansoftware.martifacts.model.Artifact;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.SortedSet;

/**
 *
 * @author mlamb
 */
public class ArtifactWithUrl implements Artifact {
    private final Artifact _a;
    public ArtifactWithUrl(Artifact a) { _a = a; }

    @Override public String id() { return _a.id(); }
    @Override public String name() { return _a.name(); }
    @Override public String hash() { return _a.hash(); }
    @Override public long size() { return _a.size(); }
    @Override public Date time() { return _a.time(); }
    @Override public Date timeAdded() { return _a.timeAdded(); }
    @Override public SortedSet<String> tags() { return _a.tags(); }

    @Override public Artifact tag(Collection<String> tags) { throw new UnsupportedOperationException("Not supported."); }
    @Override public Artifact untag(Collection<String> tags) { throw new UnsupportedOperationException("Not supported."); }

    @Override public InputStream inputStream() throws IOException { return _a.inputStream(); }

    public String url() {
        return AppRootHelper.get().map(url -> String.format("%s/get/%s", url, _a.id())).orElse(null);
    }
    
    @Override public Map<String, Object> asMap() {
        Map<String, Object> result = _a.asMap();
        result.put("url", url());
        return result;
    }
}
