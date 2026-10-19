package org.example.service;

import org.example.model.DocumentTemplate;
import org.example.repository.DocumentTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.apache.poi.poifs.filesystem.FileMagic;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import org.springframework.data.domain.Sort;

@Service
public class DocumentTemplateService {
    private final DocumentTemplateRepository repository;
    private final ObjectStorageService storage;

    public DocumentTemplateService(DocumentTemplateRepository repository, ObjectStorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public List<DocumentTemplate> list() {
        return adminList().stream().filter(template -> !template.isDeleting()).collect(java.util.stream.Collectors.toList());
    }

    public List<DocumentTemplate> adminList() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
    }

    public DocumentTemplate find(String id) {
        DocumentTemplate template = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模板不存在"));
        if (template.isDeleting()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "模板已下架，正在删除");
        return template;
    }

    public DocumentTemplate upload(String title, String description, MultipartFile file) throws IOException {
        if (title == null || title.trim().isEmpty() || title.trim().length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板名称必填且不能超过120字");
        if (description != null && description.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板说明不能超过2000字");
        if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择不超过10MB的非空模板文件");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\p{Cntrl}]", "");
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        if (!List.of(".doc", ".docx", ".pdf", ".xls", ".xlsx").contains(extension))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 Word、PDF、Excel 模板");
        boolean valid;
        try (InputStream input = FileMagic.prepareToCheckMagic(file.getInputStream())) {
            FileMagic magic = FileMagic.valueOf(input);
            valid = extension.equals(".pdf") ? magic == FileMagic.PDF
                    : (extension.equals(".doc") || extension.equals(".xls")) ? magic == FileMagic.OLE2
                    : magic == FileMagic.OOXML;
        }
        if (valid && (extension.equals(".docx") || extension.equals(".xlsx"))) {
            valid = false;
            try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                int entries = 0;
                while ((entry = zip.getNextEntry()) != null && entries++ < 1000) {
                    if (entry.getName().equals(extension.equals(".docx") ? "word/document.xml" : "xl/workbook.xml")) {
                        valid = true;
                        break;
                    }
                }
            }
        }
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件内容与扩展名不匹配");
        DocumentTemplate template = new DocumentTemplate();
        template.setId(UUID.randomUUID().toString());
        template.setTitle(title.trim());
        template.setDescription(description == null ? "" : description.trim());
        template.setOriginalName(original);
        template.setObjectKey("template_" + UUID.randomUUID() + extension);
        template.setSize(file.getSize());
        template.setCreateTime(LocalDateTime.now());
        try (InputStream input = file.getInputStream()) {
            storage.putObject(template.getObjectKey(), input, file.getSize(), "application/octet-stream");
        }
        try {
            return repository.insert(template);
        } catch (RuntimeException failure) {
            try { storage.deleteObject(template.getObjectKey()); }
            catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public InputStream download(DocumentTemplate template) throws IOException {
        return storage.getObject(template.getObjectKey());
    }

    public void delete(String id) throws IOException {
        DocumentTemplate template = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模板不存在"));
        // Persist the tombstone before touching storage. A failed cleanup remains hidden and retryable.
        template.setDeleting(true);
        repository.save(template);
        storage.deleteObject(template.getObjectKey());
        repository.deleteById(id);
    }
}
