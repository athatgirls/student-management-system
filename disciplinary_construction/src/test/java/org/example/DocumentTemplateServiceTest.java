package org.example;

import org.example.model.DocumentTemplate;
import org.example.repository.DocumentTemplateRepository;
import org.example.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.util.Optional;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentTemplateServiceTest {
    @Test void acceptsRealExcelAndPreservesDownloadName() throws Exception {
        DocumentTemplateRepository repository = mock(DocumentTemplateRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        when(repository.insert(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet("模板"); workbook.write(output); bytes = output.toByteArray();
        }
        DocumentTemplate template = new DocumentTemplateService(repository, storage).upload("申请模板", "填写说明",
                new MockMultipartFile("file", "申请.xlsx", "application/octet-stream", bytes));
        assertEquals("申请.xlsx", template.getOriginalName()); assertTrue(template.getObjectKey().endsWith(".xlsx"));
        verify(storage).putObject(eq(template.getObjectKey()), any(), eq((long) bytes.length), anyString());
    }
    @Test void rejectsSpoofedFileBeforeWritingObject() throws Exception {
        ObjectStorageService storage = mock(ObjectStorageService.class);
        DocumentTemplateService service = new DocumentTemplateService(mock(DocumentTemplateRepository.class), storage);
        assertThrows(ResponseStatusException.class, () -> service.upload("假文件", "", new MockMultipartFile("file", "fake.xlsx", "application/octet-stream", "not excel".getBytes())));
        verifyNoInteractions(storage);
    }
    @Test void compensatesStorageWhenMetadataFails() throws Exception {
        DocumentTemplateRepository repository = mock(DocumentTemplateRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        when(repository.insert(any(DocumentTemplate.class))).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> new DocumentTemplateService(repository, storage).upload("PDF", "", new MockMultipartFile("file", "template.pdf", "application/pdf", "%PDF-1.4\ncontent".getBytes())));
        verify(storage).deleteObject(anyString());
    }
    @Test void missingTemplateDoesNotReadStorage() {
        DocumentTemplateRepository repository = mock(DocumentTemplateRepository.class);
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> new DocumentTemplateService(repository, mock(ObjectStorageService.class)).find("missing"));
    }

    @Test void deletionHidesMetadataBeforeStorageAndCanBeRetriedAfterDatabaseFailure() throws Exception {
        DocumentTemplateRepository repository = mock(DocumentTemplateRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        DocumentTemplate template = new DocumentTemplate(); template.setId("a"); template.setObjectKey("template.pdf");
        when(repository.findById("a")).thenReturn(Optional.of(template));
        when(repository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(java.util.List.of(template));
        doThrow(new IllegalStateException("database unavailable")).doNothing().when(repository).deleteById("a");
        DocumentTemplateService service = new DocumentTemplateService(repository, storage);
        assertThrows(IllegalStateException.class, () -> service.delete("a"));
        org.mockito.InOrder order = inOrder(repository, storage);
        order.verify(repository).findById("a"); order.verify(repository).save(template);
        order.verify(storage).deleteObject("template.pdf"); order.verify(repository).deleteById("a");
        assertTrue(template.isDeleting()); assertTrue(service.list().isEmpty()); assertEquals(1, service.adminList().size());
        assertThrows(ResponseStatusException.class, () -> service.find("a"));
        assertDoesNotThrow(() -> service.delete("a"));
        verify(storage, times(2)).deleteObject("template.pdf");
    }

    @Test void metadataFailureBeforeTombstoneDoesNotDeleteStoredFile() throws Exception {
        DocumentTemplateRepository repository = mock(DocumentTemplateRepository.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        DocumentTemplate template = new DocumentTemplate(); template.setId("a");
        when(repository.findById("a")).thenReturn(Optional.of(template));
        when(repository.save(template)).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> new DocumentTemplateService(repository, storage).delete("a"));
        verifyNoInteractions(storage);
    }
}
