package com.example.smd.services.excelService;

import com.example.smd.dto.excel.ExcelColumn;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*    ;
import java.lang.reflect.Field;
import java.util.*;

public class ExcelExporter {

    public static <T> ByteArrayInputStream export(List<T> data, Class<T> clazz)
            throws Exception {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(clazz.getSimpleName());

        Field[] fields = clazz.getDeclaredFields();

        List<Field> excelFields = Arrays.stream(fields)
                .filter(f -> f.isAnnotationPresent(ExcelColumn.class))
                .sorted(Comparator.comparingInt(
                        f -> f.getAnnotation(ExcelColumn.class).order()))
                .toList();

        // ===== HEADER STYLE =====
        CellStyle headerStyle = workbook.createCellStyle();

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        headerStyle.setFillForegroundColor(
                IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());

        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // ===== DATA STYLE =====
        CellStyle dataStyle = workbook.createCellStyle();

        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // ===== HEADER =====
        Row header = sheet.createRow(0);

        for (int i = 0; i < excelFields.size(); i++) {

            ExcelColumn column =
                    excelFields.get(i).getAnnotation(ExcelColumn.class);

            Cell cell = header.createCell(i);
            cell.setCellValue(column.name());
            cell.setCellStyle(headerStyle);
        }

        // ===== DATA =====
        int rowIndex = 1;

        for (T item : data) {

            Row row = sheet.createRow(rowIndex++);

            for (int i = 0; i < excelFields.size(); i++) {

                Field field = excelFields.get(i);
                field.setAccessible(true);

                Object value = field.get(item);

                Cell cell = row.createCell(i);

                if (value != null) {
                    cell.setCellValue(value.toString());
                }

                cell.setCellStyle(dataStyle);
            }
        }

        // ===== AUTO SIZE =====
        for (int i = 0; i < excelFields.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        workbook.write(out);
        workbook.close();

        return new ByteArrayInputStream(out.toByteArray());
    }
}