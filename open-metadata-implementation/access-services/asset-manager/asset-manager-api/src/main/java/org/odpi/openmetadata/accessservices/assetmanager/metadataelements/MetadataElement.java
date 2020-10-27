/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.accessservices.assetmanager.metadataelements;

import org.odpi.openmetadata.accessservices.assetmanager.properties.MetadataCorrelationProperties;

/**
 * MetadataElement is the common interface for all metadata elements.  It adds the header information that is stored with the properties.
 * This includes detains of its unique identifier, type and origin.
 */
public interface MetadataElement
{
    /**
     * Return the element header associated with the properties.
     *
     * @return element header object
     */
    ElementHeader getElementHeader();


    /**
     * Set up the element header associated with the properties.
     *
     * @param elementHeader element header object
     */
    void setElementHeader(ElementHeader elementHeader);


    /**
     * Return the details of the external identifier and other correlation properties about the metadata source.
     *
     * @return properties object
     */
    MetadataCorrelationProperties getCorrelationProperties();


    /**
     * Set up the details of the external identifier and other correlation properties about the metadata source.
     *
     * @param correlationProperties properties object
     */
    void setCorrelationProperties(MetadataCorrelationProperties correlationProperties);
}
