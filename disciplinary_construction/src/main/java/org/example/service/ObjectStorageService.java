package org.example.service;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorageService {
    void putObject(String key, InputStream content, long size, String contentType) throws IOException;
    InputStream getObject(String key) throws IOException;
    void deleteObject(String key) throws IOException;
}
