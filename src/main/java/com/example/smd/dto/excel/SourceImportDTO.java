package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SourceImportDTO {
    @ExcelColumn(name = "Source Code", order = 0)
    String sourceCode;

    @ExcelColumn(name = "Source Name", order = 1)
    String sourceName;

    @ExcelColumn(name = "Source Type", order = 2)
    String sourceType;

    @ExcelColumn(name = "Author", order = 3)
    String author;

    @ExcelColumn(name = "Publisher", order = 4)
    String publisher;

    @ExcelColumn(name = "Publication Year", order = 5)
    String publicationYear;

    @ExcelColumn(name = "ISBN", order = 6)
    String isbn;

    @ExcelColumn(name = "URL", order = 7)
    String url;

    @ExcelColumn(name = "Subject Code", order = 8 )
    String subjectCode;
}
