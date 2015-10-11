package com.martiansoftware.martifacts.orient;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.tx.OTransaction;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
class OrientSupport {
    
    private final Path _dbPath;
    private final OPartitionedDatabasePool _pool;
    private static final Logger log = LoggerFactory.getLogger(OrientSupport.class);
    
    /**
     * Creates a new OrientHelper with a database at the specified path, optionally
     * creating it if necessary.
     * 
     * @param p the location of the database to open/create
     * @param autoCreate if true, automatically create the database if necessary
     */
    public OrientSupport(Path p, boolean autoCreate) {
        _dbPath = p;
        
        OGlobalConfiguration.ENVIRONMENT_DUMP_CFG_AT_STARTUP.setValue(true);        
        String dburl = String.format("plocal:%s", _dbPath.toFile().getAbsolutePath());
        if (autoCreate) {
            log.info("Opening database (or creating if necessary).  This might take a few seconds...");
        } else {
            log.info("Opening database...");
        }
        log.info(dburl);
        _pool = new OPartitionedDatabasePool( dburl, "admin", "admin").setAutoCreate(autoCreate);        
    }

    /**
     * Creates a new transaction with optimistic locking and sets it in the
     * current thread context
     * @return a new transaction with optimistic locking
     */
    private ODatabaseDocumentTx newTx() {
        return _pool.acquire().begin(OTransaction.TXTYPE.OPTIMISTIC);
    }

    /**
     * Gets the database from the thread context
     * @return the current database (if any)
     */
    public static ODatabaseDocument db() {
        return (ODatabaseDocument) ODatabaseRecordThreadLocal.INSTANCE.get();
    }

    /**
     * Ensures the database is set for the current thread and runs the specified
     * Runnable without a transaction (needed for things like schema changes, etc.)
     * 
     * @param r the stuff to do outside of a transaction
     */
    public void noTx(Runnable r) {
        try (ODatabaseDocumentTx tx = _pool.acquire()) {
            r.run();
        }
    }

    /**
     * Closes the database, duh.
     */
    public void close() { _pool.close(); }
    
    /**
     * Ensures the database is set for the current thread and runs the specified
     * Runnable inside an optimistic transaction
     * 
     * @param r the stuff to do in a transaction
     */
    void tx(Runnable r) {
        try (ODatabaseDocumentTx tx = newTx()) {
            r.run();
        }        
    }
    
    /**
     * Ensures the database is set for the current thread and returns a result
     * from the specified supplier, run within an optimistic transaction
     * 
     * @param supplier the supplier to query within a transaction
     */
    <R> R tx(Supplier<R> supplier) {
        try (ODatabaseDocumentTx tx = newTx()) {
            return supplier.get();
        }
    }
    

    
//    private <R> R tx(Function<ODatabaseDocumentTx, R> function) {
//        try (ODatabaseDocumentTx tx = newTx()) {
//            return function.apply(tx);
//        }
//    }
    
    /**
     * Rolls back the current transaction, if any.
     */
    void rollback() {
        db().rollback();
    }
    
    // TODO: should be oidentifiable?
    public List<ODocument> sql(OCommandSQL osql, Object... args) {
        if (log.isTraceEnabled()) {
            log.trace("SQL: {} [{}]", osql.getText(), 
                        Arrays.asList(args)
                                .stream()
                                .map(Objects::toString)
                                .collect(Collectors.joining(", "))
            );
        }
        Object o = osql.execute(args);
        if (o == null) {
            log.info(String.format("Turns out you get a NULL result for sql query '%s'\n", osql.getText()));
            return Collections.EMPTY_LIST;
        }
        if (o instanceof List) return (List<ODocument>) o;
        log.info(String.format("Turns out you get a %s with value %s for sql query '%s'\n", o.getClass(), o, osql.getText()));
        return Collections.EMPTY_LIST;
    }

    public List<ODocument> sql(String s, Object... args) {
        return sql(new OCommandSQL(s), args);
    }
    
    public List<ODocument> sql(String s) {
        return sql(s, (Object) null);
    }
}
