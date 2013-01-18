/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.client.util;

import static org.apache.chemistry.opencmis.commons.impl.Converter.convert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.FolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.ItemTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PolicyTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.SecondaryTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Converter;
import org.apache.chemistry.opencmis.commons.impl.JSONConverter;
import org.apache.chemistry.opencmis.commons.impl.JaxBHelper;
import org.apache.chemistry.opencmis.commons.impl.jaxb.CmisTypeDefinitionType;
import org.apache.chemistry.opencmis.commons.impl.json.parser.JSONParser;

public class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Serializes the type definition to XML, using the format defined in the
     * CMIS specification.
     * 
     * The XML is UTF-8 encoded and the stream is not closed.
     */
    public static void writeToXML(TypeDefinition type, OutputStream stream) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("Type must be set!");
        }
        if (stream == null) {
            throw new IllegalArgumentException("Output stream must be set!");
        }

        try {
            JaxBHelper.marshal(JaxBHelper.CMIS_EXTRA_OBJECT_FACTORY.createTypeDefinition(convert(type)), stream, false);
        } catch (JAXBException e) {
            throw new IOException("Marshaling failed!", e);
        }
    }

    /**
     * Serializes the type definition to JSON, using the format defined in the
     * CMIS specification.
     * 
     * The JSON is UTF-8 encoded and the stream is not closed.
     */
    public static void writeToJSON(TypeDefinition type, OutputStream stream) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("Type must be set!");
        }
        if (stream == null) {
            throw new IllegalArgumentException("Output stream must be set!");
        }

        Writer writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
        JSONConverter.convert(type).writeJSONString(writer);
        writer.flush();
    }

    /**
     * Reads a type definition from a XML stream.
     * 
     * The stream must be UTF-8 encoded.
     */
    public static TypeDefinition readFromXML(InputStream stream) throws Exception {
        if (stream == null) {
            throw new IllegalArgumentException("Input stream must be set!");
        }

        Unmarshaller u = JaxBHelper.createUnmarshaller();

        @SuppressWarnings("unchecked")
        JAXBElement<CmisTypeDefinitionType> jaxb = (JAXBElement<CmisTypeDefinitionType>) u.unmarshal(stream);

        return Converter.convert(jaxb.getValue());
    }

    /**
     * Reads a type definition from a JSON stream.
     * 
     * The stream must be UTF-8 encoded.
     */
    @SuppressWarnings("unchecked")
    public static TypeDefinition readFromJSON(InputStream stream) throws Exception {
        if (stream == null) {
            throw new IllegalArgumentException("Input stream must be set!");
        }

        JSONParser parser = new JSONParser();
        Object json = parser.parse(new InputStreamReader(stream, "UTF-8"));

        if (!(json instanceof Map)) {
            throw new CmisRuntimeException("Invalid stream! Not a type definition!");
        }

        return JSONConverter.convertTypeDefinition((Map<String, Object>) json);
    }

    private static boolean checkQueryName(String queryName) {
        return queryName != null && queryName.length() > 0 && queryName.indexOf(' ') < 0 && queryName.indexOf(',') < 0
                && queryName.indexOf('"') < 0 && queryName.indexOf('\'') < 0 && queryName.indexOf('\\') < 0
                && queryName.indexOf('.') < 0 && queryName.indexOf('(') < 0 && queryName.indexOf(')') < 0;
    }

    /**
     * Validates a type definition.
     * 
     * @return the list of validation errors
     */
    public static List<ValidationError> validateTypeDefinition(TypeDefinition type) {
        if (type == null) {
            throw new IllegalArgumentException("Type is null!");
        }

        List<ValidationError> errors = new ArrayList<TypeUtils.ValidationError>();

        if (type.getId() == null || type.getId().length() == 0) {
            errors.add(new ValidationError("id", "Type id must be set."));
        }

        if (type.getLocalName() == null || type.getLocalName().length() == 0) {
            errors.add(new ValidationError("localName", "Local name must be set."));
        }

        if (type.getQueryName() != null) {
            if (type.getQueryName().length() == 0) {
                errors.add(new ValidationError("queryName", "Query name must not be empty."));
            } else if (!checkQueryName(type.getQueryName())) {
                errors.add(new ValidationError("queryName", "Query name contains invalid characters."));
            }
        }

        if (type.isCreatable() == null) {
            errors.add(new ValidationError("creatable", "Creatable flag must be set."));
        }

        if (type.isFileable() == null) {
            errors.add(new ValidationError("fileable", "Fileable flag must be set."));
        }

        if (type.isQueryable() == null) {
            errors.add(new ValidationError("queryable", "Queryable flag must be set."));
        } else if (type.isQueryable().booleanValue()) {
            if (type.getQueryName() == null || type.getQueryName().length() == 0) {
                errors.add(new ValidationError("queryable",
                        "Queryable flag is set to TRUE, but the query name is not set."));
            }
        }

        if (type.isControllablePolicy() == null) {
            errors.add(new ValidationError("controllablePolicy", "ControllablePolicy flag must be set."));
        }

        if (type.isControllableAcl() == null) {
            errors.add(new ValidationError("controllableACL", "ControllableACL flag must be set."));
        }

        if (type.isFulltextIndexed() == null) {
            errors.add(new ValidationError("fulltextIndexed", "FulltextIndexed flag must be set."));
        }

        if (type.isIncludedInSupertypeQuery() == null) {
            errors.add(new ValidationError("includedInSupertypeQuery", "IncludedInSupertypeQuery flag must be set."));
        }

        if (type.getBaseTypeId() == null) {
            errors.add(new ValidationError("baseId", "Base type id must be set."));
        } else if (!type.getBaseTypeId().value().equals(type.getParentTypeId())) {
            if (type.getParentTypeId() == null || type.getParentTypeId().length() == 0) {
                errors.add(new ValidationError("parentTypeId", "Parent type id must be set."));
            }
        }

        if (type instanceof DocumentTypeDefinition) {
            if (type.getBaseTypeId() != BaseTypeId.CMIS_DOCUMENT) {
                errors.add(new ValidationError("baseId", "Base type id does not match the type."));
            }

            DocumentTypeDefinition docType = (DocumentTypeDefinition) type;

            if (docType.isVersionable() == null) {
                errors.add(new ValidationError("versionable", "Versionable flag must be set."));
            }

            if (docType.getContentStreamAllowed() == null) {
                errors.add(new ValidationError("contentStreamAllowed", "ContentStreamAllowed flag must be set."));
            }

        } else if (type instanceof FolderTypeDefinition) {
            if (type.getBaseTypeId() != BaseTypeId.CMIS_FOLDER) {
                errors.add(new ValidationError("baseId", "Base type id does not match the type."));
            }

        } else if (type instanceof RelationshipTypeDefinition) {
            if (type.getBaseTypeId() != BaseTypeId.CMIS_RELATIONSHIP) {
                errors.add(new ValidationError("baseId", "Base type id does not match the type."));
            }

        } else if (type instanceof PolicyTypeDefinition) {
            if (type.getBaseTypeId() != BaseTypeId.CMIS_POLICY) {
                errors.add(new ValidationError("baseId", "Base type id does not match the type."));
            }

        } else if (type instanceof ItemTypeDefinition) {
            if (type.getBaseTypeId() != BaseTypeId.CMIS_ITEM) {
                errors.add(new ValidationError("baseId", "Base type id does not match the type."));
            }

        } else if (type instanceof SecondaryTypeDefinition) {
            if (type.getBaseTypeId() != BaseTypeId.CMIS_SECONDARY) {
                errors.add(new ValidationError("baseId", "Base type id does not match the type."));
            }

        } else {
            errors.add(new ValidationError("baseId", "Unknown base interface."));
        }

        return errors;
    }

    public static class ValidationError {
        private final String attribute;
        private final String error;

        public ValidationError(String attribute, String error) {
            this.attribute = attribute;
            this.error = error;
        }

        public String getAttribute() {
            return attribute;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            return attribute + ": " + error;
        }
    }
}
