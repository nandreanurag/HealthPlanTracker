package edu.neu.csye7255.healthcare.exeception;

public class ResourceNotModifiedException extends RuntimeException {
    public ResourceNotModifiedException(String message) {
        super(message);
    }
}