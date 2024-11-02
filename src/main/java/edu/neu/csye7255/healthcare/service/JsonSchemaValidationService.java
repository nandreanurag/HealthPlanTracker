package edu.neu.csye7255.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import edu.neu.csye7255.healthcare.exeception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JsonSchemaValidationService{

    @Autowired
    private JsonSchema jsonSchema;

    public boolean validateJson(String jsonString){

//        Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);
        //if errors have a single miss match, there would be a value in the errors set.
        Set<ValidationMessage> errors = jsonSchema.validate(jsonString, InputFormat.JSON, executionContext -> {
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
        });
        //List<ValidationMessage> list = errors.stream().collect(Collectors.toList());
        if(!errors.isEmpty()){
            //event is valid.
//            log.info("event is valid");
            throw new BadRequestException(errors.toString());

        }
        return true;
    }
}