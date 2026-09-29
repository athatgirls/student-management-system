package org.example.service;

import org.example.model.MajorModel;

import java.util.List;

public interface MajorService {
    MajorModel createMajor(MajorModel major);

    MajorModel findByMajorName(String majorName);

    List<String> getAllMajorNames();

    void deleteMajorByName(String majorName);

    boolean deleteMajor(String id);
}
