package edu.neu.csye7255.healthcare.config;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class JsonSchemaConfiguration {

    private static final String SCHEMA_VALIDATION_FILE = "plan-schema.json";

    @Bean
    public JsonSchema jsonSchema() {
        InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_VALIDATION_FILE);

        if (schemaStream == null) {
            throw new IllegalArgumentException("Schema file not found: " + SCHEMA_VALIDATION_FILE);
        }
        return JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(schemaStream);
    }
}
