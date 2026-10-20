package org.example;

import org.example.controller.FileUploadController;
import org.example.controller.PrivateFileController;
import org.example.exception.ExceptionHandel;
import org.example.model.UploadedFileModel;
import org.example.repository.StudentRepository;
import org.example.repository.UploadedFileRepository;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.ObjectStorageService;
import org.example.service.TaskAttachmentAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileAttachmentAccessTest {
    private static final long LIMIT = 10L * 1024 * 1024;

    @Test void uploadsForAllInnovationModulesRejectOverTenMegabytesBeforeStorage() throws Exception {
        FileUploadController controller = new FileUploadController();
        ObjectStorageService storage = mock(ObjectStorageService.class);
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ReflectionTestUtils.setField(controller, "objectStorage", storage);
        ReflectionTestUtils.setField(controller, "uploadedFiles", files);
        ReflectionTestUtils.setField(controller, "contextPath", "/SCSE@hbut");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "student-a");
        byte[] jpg = new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0 };
        MockMultipartFile tooLarge = sizedFile("proof.jpg", "image/jpeg", jpg, LIMIT + 1);
        ResponseResult<List<String>> rejected = controller.uploadFiles(new MockMultipartFile[] { tooLarge }, request);
        assertEquals(400, rejected.getCode());
        assertEquals("单个文件不能超过 10MB", rejected.getMessage());
        assertEquals(400, controller.uploadSingleFile(tooLarge, request).getCode());
        verifyNoInteractions(storage, files);

        MockMultipartFile atLimit = sizedFile("proof.jpg", "image/jpeg", jpg, LIMIT);
        ResponseResult<List<String>> accepted = controller.uploadFiles(new MockMultipartFile[] { atLimit }, request);
        assertEquals(200, accepted.getCode());
        assertEquals(1, accepted.getData().size());
        assertTrue(accepted.getData().get(0).startsWith("/SCSE@hbut/uploads/"));
        verify(storage).putObject(anyString(), any(InputStream.class), eq(LIMIT), eq("image/jpeg"));
        verify(files).insert(any(UploadedFileModel.class));
    }

    @Test void privatePreviewAndDownloadRequireOwnershipOrAdminRole() throws Exception {
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        TaskAttachmentAccessService audience = mock(TaskAttachmentAccessService.class);
        CurrentUserAccessService access = new CurrentUserAccessService(mock(StudentRepository.class));
        PrivateFileController controller = new PrivateFileController(files, access, audience, storage);
        String filename = "d113c030-4399-4998-b0ff-fa4257d54cd8.jpg";
        UploadedFileModel metadata = new UploadedFileModel();
        metadata.setFilename(filename);
        metadata.setOwnerId("student-a");
        when(files.findById(filename)).thenReturn(Optional.of(metadata));
        when(storage.getObject(filename)).thenAnswer(call -> new ByteArrayInputStream(new byte[] { 1, 2, 3 }));

        assertThrows(AccessDeniedException.class, () -> controller.download(filename,
                Map.of("userType", "student", "userId", "student-b")));
        assertThrows(AccessDeniedException.class, () -> controller.download(filename, Map.of("userType", "student")));
        verifyNoInteractions(storage);
        assertArrayEquals(new byte[] { 1, 2, 3 }, controller.download(filename,
                Map.of("userType", "student", "userId", "student-a")).getBody().getInputStream().readAllBytes());
        assertEquals("image/jpeg", controller.download(filename,
                Map.of("userType", "admin", "userId", "admin-a")).getHeaders().getContentType().toString());
    }

    @Test void sharedUploadAcceptsSupportedImagePdfAndWordSignatures() throws Exception {
        FileUploadController controller = new FileUploadController();
        ObjectStorageService storage = mock(ObjectStorageService.class);
        UploadedFileRepository files = mock(UploadedFileRepository.class);
        ReflectionTestUtils.setField(controller, "objectStorage", storage);
        ReflectionTestUtils.setField(controller, "uploadedFiles", files);
        ReflectionTestUtils.setField(controller, "contextPath", "/SCSE@hbut");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "student-a");

        Map<String, byte[]> samples = Map.of(
                "proof.png", new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a },
                "proof.pdf", new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2d, 0, 0, 0 },
                "proof.doc", new byte[] { (byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1 },
                "proof.docx", new byte[] { 0x50, 0x4b, 0x03, 0x04, 0, 0, 0, 0 });
        for (Map.Entry<String, byte[]> sample : samples.entrySet()) {
            ResponseResult<List<String>> uploaded = controller.uploadFiles(new MockMultipartFile[] {
                    new MockMultipartFile("file", sample.getKey(), "application/octet-stream", sample.getValue()) }, request);
            assertEquals(200, uploaded.getCode(), sample.getKey());
            assertEquals(1, uploaded.getData().size());
        }
        verify(storage, times(samples.size())).putObject(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(files, times(samples.size())).insert(any(UploadedFileModel.class));
    }

    private MockMultipartFile sizedFile(String name, String contentType, byte[] content, long size) {
        return new MockMultipartFile("file", name, contentType, content) {
            @Override public long getSize() { return size; }
        };
    }

    @Test void multipartLimitFailureReturnsClearPayloadTooLargeMessage() {
        Map<String, Object> result = new ExceptionHandel().handleUploadTooLarge(new MaxUploadSizeExceededException(LIMIT));
        assertEquals(413, result.get("code"));
        assertTrue(((String) result.get("msg")).contains("10MB"));
    }
}
