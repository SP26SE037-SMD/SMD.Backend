package com.example.smd.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class VectorConverter implements AttributeConverter<List<Double>, String> {

    @Override
    public String convertToDatabaseColumn(List<Double> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        // Biến List thành chuỗi dạng "[0.1, 0.2, 0.3]"
        return attribute.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public List<Double> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        // Xóa ngoặc [] và split chuỗi để đưa về List
        String cleanData = dbData.replace("[", "").replace("]", "");
        return java.util.Arrays.stream(cleanData.split(","))
                .map(Double::valueOf)
                .collect(Collectors.toList());
    }
}
