package com.martiansoftware.martifacts.web;

import com.martiansoftware.boom.BoomResponse;
import static com.martiansoftware.boom.Boom.q;
import static com.martiansoftware.boom.Boom.request;
import static com.martiansoftware.boom.Boom.response;
import com.martiansoftware.boom.MimeType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
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

        String site = q("site").orElse(request().url()).replaceAll("[?#].*", "").replaceAll("/(index\\.html|martifacts)$", "").replaceAll("/+$", "");
        String urlDef = String.format("URL=\"%s\"\n", site);

        log.debug("getClient() => URLdef='{}'", urlDef);

        response().header("Content-Disposition", "inline; filename=\"martifacts\"");
        return new BoomResponse(scriptFor(urlDef)).as(MimeType.BIN);
    }
    
    private String scriptFor(String URLdef) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(ClientGetter.class.getResourceAsStream("/client/martifacts")));
        StringWriter sout = new StringWriter();
        try (PrintWriter out = new PrintWriter(sout)) {
            for (String s = in.readLine(); s != null; s = in.readLine()) {
                out.println(s.startsWith("URL") ? URLdef : s);
            }
        }
        return sout.toString();
    }
}
