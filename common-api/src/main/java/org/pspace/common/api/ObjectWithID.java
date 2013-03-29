package org.pspace.common.api;


import java.io.Serializable;

/**
 * @author Martin Pietsch
 */
public interface ObjectWithID<PK extends Number> extends Serializable {

    public abstract PK getId();
}
