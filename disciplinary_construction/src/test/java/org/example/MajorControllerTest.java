package org.example;

import org.example.controller.MajorController;
import org.example.model.MajorModel;
import org.example.service.MajorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MajorControllerTest {

    @Mock
    private MajorService majorService;

    @InjectMocks
    private MajorController majorController;

    @Test
    void createRejectsMajorAlreadyUsedByStudent() {
        when(majorService.getAllMajorNames()).thenReturn(Arrays.asList("软件工程", "网络空间安全"));

        ResponseEntity<Map<String, Object>> response = majorController.createMajor(body(" 软件工程 "));

        assertEquals(400, response.getBody().get("code"));
        assertEquals("该专业已存在", response.getBody().get("msg"));
        verify(majorService, never()).createMajor(any(MajorModel.class));
    }

    @Test
    void createPersistsTrimmedMajorName() {
        when(majorService.getAllMajorNames()).thenReturn(Collections.emptyList());
        when(majorService.createMajor(any(MajorModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = majorController.createMajor(body(" 电子信息 "));

        assertEquals(200, response.getBody().get("code"));
        assertEquals("电子信息", ((MajorModel) response.getBody().get("data")).getMajorName());
    }

    @Test
    void deleteDelegatesTrimmedMajorNameToService() {
        ResponseEntity<Map<String, Object>> response = majorController.deleteMajorByName(" 电子信息 ");

        assertEquals(200, response.getBody().get("code"));
        assertEquals("专业已删除", response.getBody().get("msg"));
        verify(majorService).deleteMajorByName("电子信息");
    }

    @Test
    void deleteMapsBusinessExceptionToBadRequest() {
        doThrow(new IllegalArgumentException("该专业不存在或非手动添加，无法删除"))
                .when(majorService).deleteMajorByName("人工智能");

        ResponseEntity<Map<String, Object>> response = majorController.deleteMajorByName("人工智能");

        assertEquals(400, response.getBody().get("code"));
        assertEquals("该专业不存在或非手动添加，无法删除", response.getBody().get("msg"));
    }

    @Test
    void deleteMapsUnexpectedRuntimeExceptionToServerError() {
        doThrow(new RuntimeException("Mongo unavailable"))
                .when(majorService).deleteMajorByName("人工智能");

        ResponseEntity<Map<String, Object>> response = majorController.deleteMajorByName("人工智能");

        assertEquals(500, response.getBody().get("code"));
        assertEquals("Delete major failed: Mongo unavailable", response.getBody().get("msg"));
    }

    private Map<String, Object> body(String majorName) {
        Map<String, Object> body = new HashMap<>();
        body.put("majorName", majorName);
        return body;
    }
}
