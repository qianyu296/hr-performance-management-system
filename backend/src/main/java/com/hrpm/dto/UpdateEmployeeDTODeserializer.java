package com.hrpm.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class UpdateEmployeeDTODeserializer extends StdDeserializer<UpdateEmployeeDTO> {
    private static final Set<String> ALLOWED_FIELDS = Set.of("name", "gender", "version");

    public UpdateEmployeeDTODeserializer() {
        super(UpdateEmployeeDTO.class);
    }

    @Override
    public UpdateEmployeeDTO deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode payload = parser.getCodec().readTree(parser);
        if (!payload.isObject()) {
            throw JsonMappingException.from(parser, "Employee profile update must be a JSON object");
        }

        Iterator<String> fieldNames = payload.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                throw JsonMappingException.from(parser,
                        "Employee employment fields must be changed through a personnel change");
            }
        }

        return new UpdateEmployeeDTO(textValue(parser, payload, "name"), textValue(parser, payload, "gender"),
                textValue(parser, payload, "version"));
    }

    private String textValue(JsonParser parser, JsonNode payload, String fieldName) throws JsonProcessingException {
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) {
            throw JsonMappingException.from(parser, fieldName + " must be a string");
        }
        return value.textValue();
    }
}
