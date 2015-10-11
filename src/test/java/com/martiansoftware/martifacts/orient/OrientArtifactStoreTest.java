package com.martiansoftware.martifacts.orient;

import com.martiansoftware.martifacts.model.Artifact;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public class OrientArtifactStoreTest {
    
    private static Path tmp;
    private static OrientArtifactStore store;
    private static String id1, id2, id3, id4;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger log = LoggerFactory.getLogger(OrientArtifactStoreTest.class);
    
    private static Date d(String s) throws ParseException { return sdf.parse(s); }
    private static InputStream s(String s) { return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)); }
    private static Collection<String> t(String... tags) { return Arrays.asList(tags); }
    @BeforeClass public static void setUpClass() throws IOException, ParseException {
        tmp = Files.createTempDirectory("martifact-test");
        store = new OrientArtifactStore(tmp);
    
        id1 = store.create("file1.txt", s("This is file 1"), d("2015-10-10"), t("txt", "1", "files")).id();
        id2 = store.create("file2.dat", s("This is file 2"), d("2015-03-14"), t("dat", "2", "files")).id();
        id3 = store.create("file3",     s("The third file"), d("2015-10-18"), t("3", "files", "noext")).id();
        id4 = store.create("notags",    s("I have no tags"), d("2016-10-10"), Collections.EMPTY_SET).id();
    }
    
    @AfterClass public static void tearDownClass() throws IOException {
//        org.apache.commons.io.FileUtils.deleteDirectory(tmp.toFile());
    }

    @Test public void testTags() {
        log.debug("testTags()...");
        SortedSet<String> tags = store.tags();
        assertEquals(7, tags.size());
        for (String t : t("txt", "1", "files", "dat", "2", "3", "noext")) {
            assertTrue(tags.contains(t));
        }
    }

    @Test public void testFindByTags() throws ParseException {
        log.debug("testFindByTags()...");
        assertEquals(3, store.findByTags(t("FILES")).size());        
        Collection<Artifact> as = store.findByTags(t("txt"));
        assertEquals(1, as.size());
        Artifact a = as.iterator().next();
        assertEquals("file1.txt", a.name());
        assertEquals(d("2015-10-10"), a.time());
        assertEquals(14, a.size());
        assertEquals("2416dad444a98324d6dbd41536c850f48c37ac4c", a.hash());
        Collection<String> tags = a.tags();
        assertEquals(3, tags.size());
        assertTrue(tags.contains("txt"));
        assertTrue(tags.contains("1"));
        assertTrue(tags.contains("files"));
        
        assertEquals(1, store.findByTags(t("files", "2")).size());
        
        assertTrue(store.findByTags(t("nosuchtag")).isEmpty());
    }
    
    @Test public void testFindById() {
        log.debug("testFindById()...");
        Optional<Artifact> oa = store.findById(id3);
        assertTrue(oa.isPresent());
        assertEquals("file3", oa.get().name());
    }

    @Test public void testFindByHash() {
        log.debug("testFindByHash()...");
        Collection<Artifact> as = store.findByHash("71bf590175487ccc49172362955d0de7729e9fb4");
        assertEquals(1, as.size());
        Artifact a = as.iterator().next();
        assertEquals("file2.dat", a.name());
    }
    
    @Test public void testFindByQuery() {
        log.debug("testFindByQuery()...");
        Collection<Artifact> as = store.findByQuery("2015-10-11+1w");
        assertEquals(1, as.size());
        assertEquals("file3", as.iterator().next().name());

        as = store.findByQuery("txt files");
        assertEquals(1, as.size());
        assertEquals("file1.txt", as.iterator().next().name());
    
        as = store.findByQuery("");
        assertEquals(4, as.size());
        
        as = store.findByQuery("files 2015-10-10 2015-10-18");
        assertEquals(2, as.size());
        Iterator<Artifact> ia = as.iterator();
        assertEquals("file3", ia.next().name());
        assertEquals("file1.txt", ia.next().name());
        
        as = store.findByQuery("files 2015-10-10 tag:txt 2015-10-18");
        assertEquals(1, as.size());
        assertEquals("file1.txt", as.iterator().next().name());
        
        as = store.findByQuery("nosuchtag");
        assertEquals(0, as.size());
        
        as = store.findByQuery("*.txt file?.dat");
        assertEquals(2, as.size());
        ia = as.iterator();
        assertEquals("file1.txt", ia.next().name());
        assertEquals("file2.dat", ia.next().name());
        
        as = store.findByQuery("name:notags " + id4);
        assertEquals(1, as.size());
        assertEquals("notags", as.iterator().next().name());

        as = store.findByQuery("name:notags " + id3);
        assertEquals(2, as.size());
        ia = as.iterator();
        assertEquals("notags", ia.next().name());
        assertEquals("file3", ia.next().name());
        
        as = store.findByQuery("name:notags txt");
        assertEquals(0, as.size());
        
        as = store.findByQuery("name:notags 2016-01-01-2016-12-31");
        assertEquals(1, as.size());
        assertEquals("notags", as.iterator().next().name());
        
        as = store.findByQuery("71bf590175487ccc49172362955d0de7729e9fb4");
        assertEquals(1, as.size());
        assertEquals("file2.dat", as.iterator().next().name());
        
        as = store.findByQuery("Crazy\"file-\\\"name[].*");
        assertEquals(0, as.size());
    }

    @Test public void testTagAndUntag() {
        log.debug("testTagAndUntag()...");        
        assertEquals(0, store.findByQuery("testTagAndUntag").size());
        
        Optional<Artifact> oa = store.findById(id4);
        assertEquals(0, oa.get().tags().size());
        oa.get().tag("testTagAndUntag", "andAnother");
        assertEquals(2, oa.get().tags().size());
        
        Collection<Artifact> as = store.findByQuery("testTagAndUntag");
        assertEquals(1, as.size());
        Artifact a = as.iterator().next();
        assertEquals(2, a.tags().size());
        assertTrue(a.tags().contains("testtaganduntag"));
        assertTrue(a.tags().contains("andanother"));
        a.untag("andAnother");
        
        as = store.findByQuery("testTagAndUntag");
        assertEquals(1, as.size());
        a = as.iterator().next();
        assertEquals(1, a.tags().size());
        assertTrue(a.tags().contains("testtaganduntag"));
        assertFalse(a.tags().contains("andanother"));
        a.untag("testtaganduntag");
        
        oa = store.findById(id4);
        assertEquals(0, oa.get().tags().size());
        
        testTags(); // make sure we garbage collected tags properly
    }
    
    @Test public void testAll() {
        log.debug("testAll()...");
        assertEquals(4, store.all().size());
    }
    
    @Test public void testReadArtifact() throws IOException {
        log.debug("testReadArtifact()...");
        Optional<Artifact> oa = store.findById(id4);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(oa.get().inputStream(), bout);
        String s = new String(bout.toByteArray(), StandardCharsets.UTF_8);
        assertEquals("I have no tags", s);
    }
    
}
