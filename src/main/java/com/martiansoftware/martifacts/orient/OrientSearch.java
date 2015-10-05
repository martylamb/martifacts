package com.martiansoftware.martifacts.orient;

import com.martiansoftware.martifacts.model.Tags;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author mlamb
 */
class OrientSearch {

    private final String sql;
    private final List<Object> params = new java.util.ArrayList<>();
    private boolean noSearchParams = true;
    private boolean noResults = false;
    
    public OrientSearch(String search, OrientBackend backend) {
        search = search.toLowerCase(); // everything is case-insensitive here...
        
        Clause nonTagsClause; // everything but tags is ORed together.  Tags are then filters on those results.
        nonTagsClause = new Clause(" OR ");        
        
        Set<String> tags = new java.util.HashSet<>();  // tags specified as part of query, held separate from other params
        
        List<QueryMatcher> matchers = new java.util.ArrayList<>();
        matchers.add(new QueryMatcher("id:", "^\\p{XDigit}{8}-(?:\\p{XDigit}{4}-){3}\\p{XDigit}{12}$")  // a UUID or manually prefixed with "id:"
                        .onMatch((s) -> nonTagsClause.add("uuid = ?", s)));
        matchers.add(new QueryMatcher("sha1:", "^\\p{XDigit}{40}$")                             // a SHA-1 hash or manually prefixed with "sha1:"
                        .onMatch((s) -> nonTagsClause.add("sha1 = ?", s)));
        matchers.add(new QueryMatcher("name:", ".*[*?].*")                                     // any file glob or manually prefixed with "name:"
                        .onMatch((s) -> addNameToQuery(nonTagsClause, s)));
        matchers.add(new QueryMatcher("tag:", ".+")                                             // anything else or manually prefixed with "tag:"
                        .onMatch(s -> tags.add(s)));
        
        // go through search terms and add 'em to the query per the QueryMatchers defined above
        split(search.toLowerCase()).forEach(
            (s) -> matchers.stream().filter((m) -> m.matchSuccess(s)).findFirst().orElseThrow(IllegalArgumentException::new)
        );

        // first check the tags specified on the command line.  any results returned must match ALL tags specified.
        // easy shortcut: if any nonexistent tags were specified we can shortcut since we know nothing can match.
        Collection<String> ntags = Tags.normalize(tags);
        String tagSql = null; // the sql for the tag portion of the query.  save it for later.
        if (!ntags.isEmpty()) {
            List<ODocument> tagDocs = backend.findTagDocsFor(ntags);
            if (tagDocs.size() < ntags.size()) { // nonexistent tags specified - nothing can possibly match
                noResults = true; noSearchParams = false; sql = ""; return;
            } else{
                // use RIDs for tags since the Artifacts.tags linkset is indexed
                tagSql = tagDocs.stream().map(t -> "tags contains " + t.getIdentity() + " ").collect(Collectors.joining("AND "));
            }
        }

        // if we got this far we have some sql to build
        StringBuilder s = new StringBuilder();
        s.append("select from Artifact where ");
        if (!nonTagsClause.isEmpty()) {
            s.append(nonTagsClause.sql());          // all of the non-tag conditions for our sql as a prepared statement...
            params.addAll(nonTagsClause.params());  // ...plus all of the associated parameters for the prepared statement
            noSearchParams = false;                 // something was specified by the user, so don't return ALL
        }
        if (tagSql != null) {
            if (!noSearchParams) s.append(" AND ");  // only need AND if there was a nonTagsClause
            s.append(String.format("(%s)", tagSql)); // require all of the tags identified above
            noSearchParams = false;                  // something was specified by the user, so don't return ALL
        }
        sql = s.toString();        
    }
    
    public String sql() { return sql; }                                              // text portion of a prepared statement for this search
    public List<Object> sqlParams() { return Collections.unmodifiableList(params); } // arguments for a prepared statement for this search
    public boolean hasNoParams() { return noSearchParams; }                          // if user specified no search params, return all documents!
    public boolean hasNoResults() { return noResults; }                              // if query can't return any results, don't bother with query!
    
    // split a String containing a query into individual search parameters.  TODO: handle quoted strings so users can search for filenames with spaces
    private Stream<String> split(String search) { return Arrays.asList(search.split("\\s+")).stream().filter((s) -> !s.isEmpty()); }
    
    // filenames in queries are a bit different.  straight names work as prepared statements, but regexes don't.
    private void addNameToQuery(Clause clause, String s){
        if (isGlob(s)) clause.add(String.format("name MATCHES \"%s\"", globToRegex(s)));
        else clause.add("name = ?", s);
    }    
    
    private boolean isGlob(String s) { return s.contains("*") || s.contains("?"); }
    
    // converts a filename glob (with special chars * and ?) to a regex
    private String globToRegex(String s) {
        StringBuilder glob = new StringBuilder("(?i)"); // case-insensitive
        for (char c : s.toCharArray()) {
            switch (c) {
                case '*': glob.append(".*"); break;
                case '?': glob.append("."); break;
                case '.': glob.append("[.]"); break;
                case '\\': glob.append("[\\\\]"); break;
                case '"': glob.append("\\\""); break;
                case '\n': glob.append("\\n"); break;
                default:
                    if(Character.isLetterOrDigit(c) || ' ' == c) glob.append(c);
                    else {
                        glob.append("\\u");
                        glob.append(String.format("%04x", (int) c));
                    }
            }
        }
        return glob.toString();
    }

    // helper to parse search query.  for some terms we can automatically figure out what kind of
    // search term they are (e.g., a file glob only applies to a filename, and if something is clearly
    // a sha1 hash we can safely assume that's what the user is looking for).  this helper provides a
    // way to force specific search terms with a prefix as well, in case the user really really wants
    // a tag that looks like e.g. a UUID.
    class QueryMatcher {
        private final String _prefix;                                         // if present and query term matches this, forces a match.
        private final List<Pattern> _patterns = new java.util.LinkedList<>(); // used to auto-match if no term prefix present in query term
        private Consumer<String> _action;                                     // what to do if there's a match
        public QueryMatcher(String prefixToForce, String... regexes) {
            _prefix = prefixToForce;
            for (String regex : regexes) _patterns.add(Pattern.compile(regex));
        }
        private String match(String query) {                                  // if there's a match, what is its text?  (strip prefix if present)
            if (_prefix != null && query.startsWith(_prefix)) return query.substring(_prefix.length());
            for (Pattern p : _patterns) {
                Matcher m = p.matcher(query);
                if (m.matches()) return query;
            }
            return null;                                                      // no match for you!
        }
        public QueryMatcher onMatch(Consumer<String> action) { _action = action; return this; }
        public boolean matchSuccess(String s) {                               // if this matches, call the action and return true.  used with stream.findFirst(), ensures only first match is applied
            String m = match(s);
            if (m != null) _action.accept(m);
            return m != null;
        }
    }
    
    // helper to build a prepared statement with corresponding parameters
    class Clause {
        private final String _joiner;
        private final StringBuilder _sql = new StringBuilder();
        private final List<Object> _params = new java.util.ArrayList<>();
        public Clause(String joiner) { _joiner = joiner; }
        public void add(String sql, Object... params) {
            if (_sql.length() > 0) _sql.append(_joiner);
            _sql.append(sql);            
            _params.addAll(Arrays.asList(params));
        }
        public String sql() { return String.format("(%s)", _sql.toString()); }
        public List<Object> params() { return Collections.unmodifiableList(_params); }
        @Override public String toString() { return sql() + " [" + _params.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "]"; }
        public boolean isEmpty() { return _sql.length() == 0; }
    }
    
}
