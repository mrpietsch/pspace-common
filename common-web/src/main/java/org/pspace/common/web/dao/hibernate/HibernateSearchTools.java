package org.pspace.common.web.dao.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.TermMatchingContext;

/**
 * Utility class to generate lucene queries for hibernate search and perform full reindexing.
 *
 * @author jgarcia
 */
class HibernateSearchTools {
    protected static final Log log = LogFactory.getLog(HibernateSearchTools.class);


    /**
     * Generates a lucene query to search for a given term in all the indexed fields of a class
     *
     * @param searchTerm the term to search for
     * @param searchedEntity the class searched
     * @param sess the hibernate session
     * @return query
     * @throws ParseException
     */
    public static Query generateQuery(String searchTerm, Class searchedEntity, Session sess, Analyzer analyzer) throws ParseException {
        Query qry = null;

        if (searchTerm.equals("*")) {
            qry = new MatchAllDocsQuery();
        } else {
            // Search in all indexed fields

            IndexReaderAccessor readerAccessor = null;
            IndexReader reader = null;
            try {
                FullTextSession fullTextSession = Search.getFullTextSession(sess);

                // search on all indexed fields: generate field list, removing internal hibernate search field name: _hibernate_class
                // TODO: possible improvement: cache the fields of each entity
                SearchFactory searchFactory = fullTextSession.getSearchFactory();
                readerAccessor = searchFactory.getIndexReaderAccessor();
                reader = readerAccessor.open(searchedEntity);
                FieldInfos fieldInfos = ReaderUtil.getMergedFieldInfos(reader);
                int size = fieldInfos.size();
                ArrayList<String> fieldNames = new ArrayList<String>();
                for ( int i=0; i<size; i++ ) {
                    if (fieldInfos.fieldInfo(i).isIndexed)
                    fieldNames.add(fieldInfos.fieldName(i));
                }
                fieldNames.remove("_hibernate_class");

                QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity(searchedEntity).get();

                TermMatchingContext termMatchingContext = queryBuilder.keyword().wildcard().onField(fieldNames.get(0));
                for (int i = 1; i < fieldNames.size(); ++i) {
                    termMatchingContext = termMatchingContext.andField(fieldNames.get(i));
                }

                // TODO: use proper analyzer per field
                qry = termMatchingContext.matching(searchTerm.toLowerCase() + "*").createQuery();

//                String[] fnames = new String[0];
//                fnames = fieldNames.toArray(fnames);
//
//                // To search on all fields, search the term in all fields
//                String[] queries = new String[fnames.length];
//                for (int i = 0; i < queries.length; ++i) {
//                    queries[i] = searchTerm;
//                }
//
//                qry = MultiFieldQueryParser.parse(Version.LUCENE_36, queries, fnames, analyzer);

            } finally {
                if (readerAccessor != null && reader != null) {
                    readerAccessor.close(reader);
                }
            }
        }
        return qry;
    }

    /**
     * Regenerates the index for a given class
     *
     * @param clazz the class
     * @param sess the hibernate session
     */
    public static void reindex(Class clazz, Session sess) {
        FullTextSession txtSession = Search.getFullTextSession(sess);
        MassIndexer massIndexer = txtSession.createIndexer(clazz);
        try {
            massIndexer.startAndWait();
        } catch (InterruptedException e) {
            log.error("mass reindexing interrupted: " + e.getMessage());
        } finally {
            txtSession.flushToIndexes();
        }
    }

    /**
     * Regenerates all the indexed class indexes
     *
     * @param async true if the reindexing will be done as a background thread
     * @param sess the hibernate session
     */
    public static void reindexAll(boolean async, Session sess) {
        FullTextSession txtSession = Search.getFullTextSession(sess);
        MassIndexer massIndexer = txtSession.createIndexer();
        massIndexer.purgeAllOnStart(true);
        try {
            if (!async) {
                massIndexer.startAndWait();
            } else {
                massIndexer.start();
            }
        } catch (InterruptedException e) {
            log.error("mass reindexing interrupted: " + e.getMessage());
        } finally {
            txtSession.flushToIndexes();
        }
    }
}

