package io.stardog.stardao.auto.processor;

import javax.lang.model.element.Element;

public class ProcessorException extends RuntimeException {
    public ProcessorException(String message, Element element) {
        super("Error processing " + element + ": " + message);
    }
}
