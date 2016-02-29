package org.pspace.common.web.dao.jackrabbit;

import javax.jcr.Session;

public interface SessionAwareCallable<T> {
    T call(Session session) throws Exception;
}