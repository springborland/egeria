/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.assetmanager.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * EnumSchemaTypeProperties carries the specialized parameters for creating or updating enum schema types.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)

public class ExternalSchemaTypeProperties extends SimpleSchemaTypeProperties
{
    private static final long     serialVersionUID = 1L;

    private SchemaTypeProperties externalSchemaType = null;

    /**
     * Default constructor
     */
    public ExternalSchemaTypeProperties()
    {
        super();
    }


    /**
     * Copy/clone Constructor
     *
     * @param template template object to copy.
     */
    public ExternalSchemaTypeProperties(ExternalSchemaTypeProperties template)
    {
        super(template);
    }


    /**
     * Return the unique identifier of the schema type that is reusable amongst assets.
     *
     * @return string guid
     */
    public SchemaTypeProperties getExternalSchemaType()
    {
        return externalSchemaType;
    }


    /**
     * Set up the unique identifier of the schema type that is reusable amongst assets.
     *
     * @param externalSchemaType string guid
     */
    public void setExternalSchemaType(SchemaTypeProperties externalSchemaType)
    {
        this.externalSchemaType = externalSchemaType;
    }


    /**
     * Standard toString method.
     *
     * @return print out of variables in a JSON-style
     */
    @Override
    public String toString()
    {
        return "ExternalSchemaTypeProperties{" +
                       "externalSchemaType=" + externalSchemaType +
                       ", assetType='" + getAssetType() + '\'' +
                       ", defaultValue='" + getDefaultValue() + '\'' +
                       ", versionNumber='" + getVersionNumber() + '\'' +
                       ", author='" + getAuthor() + '\'' +
                       ", usage='" + getUsage() + '\'' +
                       ", encodingStandard='" + getEncodingStandard() + '\'' +
                       ", namespace='" + getNamespace() + '\'' +
                       ", formula='" + getFormula() + '\'' +
                       ", queries=" + getQueries() +
                       ", isDeprecated=" + getIsDeprecated() +
                       ", technicalName='" + getTechnicalName() + '\'' +
                       ", technicalDescription='" + getTechnicalDescription() + '\'' +
                       ", displayName='" + getDisplayName() + '\'' +
                       ", summary='" + getSummary() + '\'' +
                       ", description='" + getDescription() + '\'' +
                       ", abbreviation='" + getAbbreviation() + '\'' +
                       ", usage='" + getUsage() + '\'' +
                       ", qualifiedName='" + getQualifiedName() + '\'' +
                       ", additionalProperties=" + getAdditionalProperties() +
                       ", vendorProperties=" + getVendorProperties() +
                       ", typeName='" + getTypeName() + '\'' +
                       ", extendedProperties=" + getExtendedProperties()+
                       '}';
    }


    /**
     * Compare the values of the supplied object with those stored in the current object.
     *
     * @param objectToCompare supplied object
     * @return boolean result of comparison
     */
    @Override
    public boolean equals(Object objectToCompare)
    {
        if (this == objectToCompare)
        {
            return true;
        }
        if (objectToCompare == null || getClass() != objectToCompare.getClass())
        {
            return false;
        }
        if (!super.equals(objectToCompare))
        {
            return false;
        }
        ExternalSchemaTypeProperties that = (ExternalSchemaTypeProperties) objectToCompare;
        return Objects.equals(externalSchemaType, that.externalSchemaType);
    }


    /**
     * Return hash code for this object
     *
     * @return int hash code
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), externalSchemaType);
    }
}