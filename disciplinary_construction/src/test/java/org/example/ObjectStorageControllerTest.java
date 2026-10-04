package org.example;

import org.example.controller.FileUploadController;
import org.example.controller.PrivateFileController;
import org.example.model.UploadedFileModel;
import org.example.repository.StudentRepository;
import org.example.repository.UploadedFileRepository;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStorageControllerTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path legacyDir;

    @Test
    void legacyFallbackKeepsOwnerAndTaskAudienceAuthorization() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        org.example.service.TaskAttachmentAccessService audience = mock(org.example.service.TaskAttachmentAccessService.class);
        UploadedFileModel metadata = new UploadedFileModel(); metadata.setOwnerId("admin");
        when(files.findById("fixture.pdf")).thenReturn(Optional.of(metadata));
        when(storage.getObject("fixture.pdf")).thenThrow(new org.example.service.ObjectStorageNotFoundException("missing", null));
        when(storage.getObject("missing.pdf")).thenThrow(new org.example.service.ObjectStorageNotFoundException("missing", null));
        java.nio.file.Files.writeString(legacyDir.resolve("fixture.pdf"), "legacy fixture");
        PrivateFileController controller = new PrivateFileController(files,
                new CurrentUserAccessService(mock(StudentRepository.class)), audience, storage);
        ReflectionTestUtils.setField(controller, "uploadDir", legacyDir.toString());
        Map<String,Object> recipient = Map.of("userId", "student-a", "userType", "student");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.download("fixture.pdf", recipient));
        when(audience.canDownload("fixture.pdf", metadata, recipient)).thenReturn(true);
        assertEquals("legacy fixture", new String(controller.download("fixture.pdf", recipient).getBody().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.download("fixture.pdf", Map.of("userId", "student-b", "userType", "student")));
        assertEquals(404, controller.download("missing.pdf", Map.of("userId", "admin", "userType", "admin")).getStatusCodeValue());
    }
    @Test
    void uploadKeepsUuidKeyAndContextRelativeUrlContract() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "uploadedFiles", files);
        ReflectionTestUtils.setField(controller, "objectStorage", storage);
        ReflectionTestUtils.setField(controller, "contextPath", "/SCSE@hbut");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "student-a");
        MockMultipartFile file = new MockMultipartFile("file", "evidence.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        ResponseResult<String> response = controller.uploadSingleFile(file, request);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).putObject(key.capture(), any(), eq(file.getSize()), eq("image/png"));
        assertTrue(key.getValue().matches("[0-9a-f-]{36}\\.png"));
        assertEquals("/SCSE@hbut/uploads/" + key.getValue(), response.getData());
        verify(files).insert(any(UploadedFileModel.class));
    }

    @Test
    void authorizedReadStreamsObjectStorageContent() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        UploadedFileModel metadata = new UploadedFileModel();
        metadata.setOwnerId("student-a");
        metadata.setSize(99);
        when(files.findById("fixture.pdf")).thenReturn(Optional.of(metadata));
        when(storage.getObject("fixture.pdf")).thenReturn(new ByteArrayInputStream("fixture".getBytes(StandardCharsets.UTF_8)));
        PrivateFileController controller = new PrivateFileController(files,
                new CurrentUserAccessService(mock(StudentRepository.class)), mock(org.example.service.TaskAttachmentAccessService.class), storage);

        ResponseEntity<Resource> response = controller.download("fixture.pdf", Map.of("userId", "student-a", "userType", "student"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals(-1, response.getHeaders().getContentLength());
        assertEquals("fixture", new String(response.getBody().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        verify(storage).getObject("fixture.pdf");
    }

    @Test
    void invalidFileInBatchWritesNeitherObjectNorOwnership() {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "uploadedFiles", files);
        ReflectionTestUtils.setField(controller, "objectStorage", storage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "student-a");
        MockMultipartFile valid = new MockMultipartFile("file", "evidence.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        MockMultipartFile invalid = new MockMultipartFile("file", "invalid.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

        ResponseResult<?> response = controller.uploadFiles(new MockMultipartFile[]{valid, invalid}, request);

        assertEquals(400, response.getCode());
        verifyNoInteractions(storage, files);
    }

    @Test
    void ownershipPersistenceFailureDeletesStoredObject() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        doThrow(new RuntimeException("database unavailable")).when(files).insert(any(UploadedFileModel.class));
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "uploadedFiles", files);
        ReflectionTestUtils.setField(controller, "objectStorage", storage);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "student-a");
        MockMultipartFile file = new MockMultipartFile("file", "evidence.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        assertThrows(RuntimeException.class, () -> controller.uploadSingleFile(file, request));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).deleteObject(key.capture());
        assertTrue(key.getValue().matches("[0-9a-f-]{36}\\.png"));
    }
}
