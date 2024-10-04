package edu.neu.csye7255.healthcare.exeception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class PlanAlreadyExists extends BadRequestException {
    public PlanAlreadyExists(String message) {
        super(message);
    }
}
