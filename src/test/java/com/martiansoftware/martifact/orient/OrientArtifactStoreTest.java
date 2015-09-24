package com.martiansoftware.martifact.orient;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mlamb
 */
public class OrientArtifactStoreTest {
    
    private static OrientBackend _backend;
    
    public OrientArtifactStoreTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        try {
            _backend = new OrientBackend(Paths.get("/home/mlamb/teststore"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test public void makeSureIUnderstandTransactionModel() {
        
        _backend.noTx(()-> {
            new OCommandSQL("drop class Test").execute();
            ODocument doc1 = new ODocument("Test");
            doc1.field("testfield", "abc");
            doc1.save();
        });
        
        _backend.tx(() -> {
            OCommandSQL sql = new OCommandSQL("select from Test where testfield = ?");
            List<ODocument> results = (List<ODocument>) sql.execute("abc");
            assertEquals(1, results.size());
            assertEquals("abc", results.get(0).field("testfield"));
        });

        // try a rollback
        _backend.tx(() -> {
           OCommandSQL sql = new OCommandSQL("insert into Test set testfield = ?");
           sql.execute("anothertest");
           _backend.rollback();
        });
        _backend.tx(() -> {
           OCommandSQL sql = new OCommandSQL("select from Test where testfield = ?");
           assertEquals(0, ((List<ODocument>) sql.execute("anothertest")).size());
        });

        // and one without a rollback
        _backend.tx(() -> {
           OCommandSQL sql = new OCommandSQL("insert into Test set testfield = ?");
           sql.execute("anothertest");
        });
        _backend.tx(() -> {
           OCommandSQL sql = new OCommandSQL("select from Test where testfield = ?");
           assertEquals(1, ((List<ODocument>) sql.execute("anothertest")).size());
        });

    }
    
}
