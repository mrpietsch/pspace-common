package org.pspace.common.web.dao;

import org.pspace.common.web.dao.jackrabbit.SearchResult;

import java.util.List;

/**
 * @author peach
 */
public interface SearchableRepositoryDao extends RepositoryDao {

    List<SearchResult> search(String q) throws Exception;

    String suggestQuery(String q) throws Exception;

}
