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

/**
 *
 * @author mlamb
 */
public class ClientGetter {
    
    // returns a command line client with the URL properly set for this
    // server instance
    public Object getClient() throws IOException, URISyntaxException {        
        String site = q("site", request().url()).replaceAll("[?#].*", "").replaceAll("/(index\\.html|client)$", "").replaceAll("/+$", "");        
        String URLdef = String.format("URL=\"%s\"\n", site);
        Path p = Paths.get(ClientGetter.class.getResource("/client/martifacts").toURI());
        
        StringBuilder s = new StringBuilder();
        Files.lines(p)
            .map(line -> line.startsWith("URL") ? URLdef : line + "\n")
            .forEach(s::append);

        response().header("Content-Disposition", "inline; filename=\"martifacts\"");
        return new BoomResponse(s.toString()).as(MimeType.BIN);
    }
}
