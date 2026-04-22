package com.askaragoz.supportagent.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts between Java float[] and pgvector's string format "[v1,v2,v3,...]".
 *
 * WHY THIS EXISTS:
 * PostgreSQL's pgvector extension stores vectors in a custom "vector" column type.
 * Hibernate does not natively understand this type, so we teach it how to:
 *   Write: serialize float[] → "[1.0,2.0,...]" before INSERT/UPDATE.
 *   Read:  deserialize "[1.0,2.0,...]" → float[] after SELECT.
 *
 * AttributeConverter<X, Y>:
 *   X = the Java type (float[])
 *   Y = the database column type that JDBC understands (String)
 *
 * autoApply = false — only applied where @Convert(converter = ...) is explicitly declared.
 */
@Converter(autoApply = false)
public class FloatArrayToVectorConverter implements AttributeConverter<float[], String> {

    /**
     * float[] → "[v1,v2,v3,...]"
     * Called by Hibernate before INSERT and UPDATE.
     */
    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            sb.append(attribute[i]);
            if (i < attribute.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * "[v1,v2,v3,...]" → float[]
     * Called by Hibernate after SELECT.
     */
    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String cleaned = dbData.replaceAll("[\\[\\]]", "");
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
