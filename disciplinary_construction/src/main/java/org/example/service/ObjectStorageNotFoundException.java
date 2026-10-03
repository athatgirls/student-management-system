package org.example.service;

import java.io.IOException;

public class ObjectStorageNotFoundException extends IOException {
    public ObjectStorageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
