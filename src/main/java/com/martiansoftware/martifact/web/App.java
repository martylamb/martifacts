package com.martiansoftware.martifact.web;

import static com.martiansoftware.boom.Boom.get;
import static com.martiansoftware.boom.Boom.post;
import com.martiansoftware.martifact.orient.OrientArtifactStore;
import java.nio.file.Paths;

/**
 *
 * @author mlamb
 */
public class App {
    
    private static OrientArtifactStore _store;

    public static void main(String[] args) throws Exception {
        _store = new OrientArtifactStore(Paths.get(System.getProperty("user.home")).resolve(".martifact"));

        post("/add", new ArtifactAdder(_store)::add);
        get("/get/:id", new ArtifactGetter(_store)::get);
        get("/search", new ArtifactSearcher(_store)::search);
        get("/client", new ClientGetter()::getClient);
    }
}
