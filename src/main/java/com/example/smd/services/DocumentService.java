package com.example.smd.services;

import com.example.smd.dto.request.DocumentRequest;
import com.example.smd.dto.response.DocumentResponse;
import com.example.smd.entities.Department;
import com.example.smd.entities.Document;
import com.example.smd.entities.Major;
import com.example.smd.entities.Subject;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.DocumentMapper;
import com.example.smd.repositories.DocumentRepository;
import com.example.smd.repositories.MajorRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentService {
    MajorRepository majorRepository;
    DocumentRepository repository;
    DocumentMapper mapper;

    @Transactional
    public List<DocumentResponse> getAll(UUID majorId, String status) {
        List<Document> documents = repository.findAllWithFilters(majorId, status);
        if(majorId == null && status == null){
            documents = repository.findAllByMajorIsNull();
        } else if (majorId == null && status != null){
            documents = repository.findAllByMajorIsNullAndStatus(status);
        }
        return mapper.toResponseList(documents);
    }

    public DocumentResponse getById(UUID id) {
        var document = repository.findById(id).orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        var response = mapper.toDocumentResponse(document);
        if (document.getMajor() != null && document.getMajor().getMajorId() != null) {
            response.setMajorId(document.getMajor().getMajorId());
        }
        return response;
    }

    public DocumentResponse create(DocumentRequest request) {
        var document = mapper.toEntity(request);
        if (request.getMajorId() != null) {
            Major major = majorRepository.findById(request.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
            document.setMajor(major);
        } else {
            // Nếu không có majorId, đảm bảo field major trong entity là null
            document.setMajor(null);
        }
        document.setStatus("ACTIVE");
        var response = repository.save(document);
        return mapper.toDocumentResponse(response);
    }

    public DocumentResponse update(UUID id, DocumentRequest request) {
        Document document = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        if(request.getMajorId() != null) {
            Major major = majorRepository.findById(request.getMajorId())
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
            document.setMajor(major);
        }
        if(request.getDocumentUrl() != null) {
            document.setDocumentUrl(request.getDocumentUrl());
        }
        var response = repository.save(document);
        return mapper.toDocumentResponse(response);
    }

    public void delete(UUID id) {
        Document document = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        document.setStatus("DELETED");
        repository.save(document);
    }
}
