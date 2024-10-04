package edu.neu.csye7255.healthcare.exeception;

public class ETagParseException extends BadRequestException {
    public ETagParseException(String s) {
        super(s);
    }
}
