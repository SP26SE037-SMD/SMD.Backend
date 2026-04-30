package com.example.smd.mapper;

import com.example.smd.dto.request.DepartmentRequest;
import com.example.smd.dto.request.DocumentRequest;
import com.example.smd.dto.response.DepartmentResponse;
import com.example.smd.dto.response.DocumentResponse;
import com.example.smd.entities.Department;
import com.example.smd.entities.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    @Mapping(target = "documentId", ignore = true)
    Document toEntity(DocumentRequest request);

    @Mapping(target = "documentId", ignore = true)
    void updateDocument(@MappingTarget Document document, DocumentRequest request);

    DocumentResponse toDocumentResponse(Document document);
    List<DocumentResponse> toResponseList(List<Document> documents);
}
