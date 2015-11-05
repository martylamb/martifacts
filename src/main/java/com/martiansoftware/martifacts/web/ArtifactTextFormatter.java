package com.martiansoftware.martifacts.web;

import com.martiansoftware.martifacts.model.Artifact;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class ArtifactTextFormatter {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private static final TableFormatter<ArtifactWithUrl> _formatter =
        new TableFormatter<ArtifactWithUrl>()
            .left("ID", a -> a.id())
            .left("TIME", a-> sdf.format(a.time()))
            .right("SIZE", a -> String.format("%d", a.size()))
            .left("NAME", a -> a.name())
            .left("", a -> "#")
            .left("TAGS", a -> a.tags().stream().collect(Collectors.joining(" ")))
            .left("URL", a -> a.url());
            
    public static String format(Collection<ArtifactWithUrl> artifacts) {
        return _formatter.format(artifacts);
    }
}
