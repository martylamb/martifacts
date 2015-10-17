package com.martiansoftware.martifacts.web;

import static com.martiansoftware.boom.Boom.get;
import static com.martiansoftware.boom.Boom.post;
import com.martiansoftware.boom.Json;
import com.martiansoftware.martifacts.orient.OrientArtifactStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public class App {
    
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static OrientArtifactStore _store;

    private static void usageAndExit(int exitCode) {
        System.err.println("\nUsage: martifactsd [-h|--help]     (1st form)");
        System.err.println("  or:  martifactsd DATA_DIRECTORY  (2nd form)");
        System.err.println("  or:  martifactsd                 (3rd form)\n");
        System.err.println("In the first form, print this message and exit.");
        System.err.println("In the second form, start the server, storing all artifacts and data in the specified DATA_DIRECTORY.");
        System.err.println("In the third form, start the server, storing all artifacts and data in $HOME/.martifacts");
        System.exit(exitCode);
    }
    
    private static Throwable rootCauseOf(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }
    
    private static void launch(Path dataDir) {
        try {
            log.info("Starting server with data in {}", dataDir);
            _store = new OrientArtifactStore(dataDir);
            post("/add", new ArtifactAdder(_store)::add);
            get("/get/:id", new ArtifactGetter(_store)::get);
            get("/search", new ArtifactSearcher(_store)::search);
            get("/martifacts", new ClientGetter()::getClient);
            get("/tagstats", new TagStatsGetter(_store)::tagstats);
            
            log.info("Ready for clients.");
        } catch (Exception e) {
            log.error("Unable to launch server: {}", rootCauseOf(e).getLocalizedMessage());
        }
    }
       
    public static void main(String[] args) throws Exception {
        switch(args.length) {
            case 0: Path p = Paths.get(System.getProperty("user.home")).resolve(".martifacts");
                    log.info("No data directory specified; using default {}", p);
                    launch(p);
                    break;
            case 1: if ("-h".equals(args[0]) || "--help".equals(args[0])) usageAndExit(0);
                    launch(Paths.get(args[0]));
                    break;
            default: usageAndExit(1);
        }
    }
}
