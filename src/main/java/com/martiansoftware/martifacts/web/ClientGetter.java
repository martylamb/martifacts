package com.martiansoftware.martifacts.web;

import com.martiansoftware.boom.BoomResponse;
import static com.martiansoftware.boom.Boom.q;
import static com.martiansoftware.boom.Boom.request;
import static com.martiansoftware.boom.Boom.response;
import com.martiansoftware.boom.MimeType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public class ClientGetter {
    
    private static final Logger log = LoggerFactory.getLogger(ClientGetter.class);
    
    // returns a command line client with the URL properly set for this
    // server instance
    public Object getClient() throws IOException, URISyntaxException {
        log.debug("getClient() => site='{}'", q("site"));
        log.debug("getClient() => url='{}'", request().url());        

        String site = q("site").orElse(request().url()).replaceAll("[?#].*", "").replaceAll("/(index\\.html|client)$", "").replaceAll("/+$", "");
        String URLdef = String.format("URL=\"%s\"\n", site);

        log.debug("getClient() => URLdef='{}'", URLdef);

        Path p = Paths.get(ClientGetter.class.getResource("/client/martifacts").toURI());
        
        StringBuilder s = new StringBuilder();
        Files.lines(p)
            .map(line -> line.startsWith("URL") ? URLdef : line + "\n")
            .forEach(s::append);

        response().header("Content-Disposition", "inline; filename=\"martifacts\"");
        return new BoomResponse(s.toString()).as(MimeType.BIN);
    }
}
