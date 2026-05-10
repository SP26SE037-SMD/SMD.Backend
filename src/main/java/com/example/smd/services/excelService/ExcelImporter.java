package com.example.smd.services.excelService;

import com.example.smd.dto.excel.ExcelColumn;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.*;

public class ExcelImporter {

    public static <T> List<T> importFromExcel(MultipartFile file, Class<T> clazz) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            return importFromSheet(sheet, clazz);
        }
    }

    public static <T> List<T> importFromSheet(Sheet sheet, Class<T> clazz) throws Exception {
        List<T> result = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(ExcelColumn.class))
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(ExcelColumn.class).order()))
                .toList();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            T obj = clazz.getDeclaredConstructor().newInstance();
            boolean isEmptyRow = true;

            for (int col = 0; col < fields.size(); col++) {
                Field field = fields.get(col);
                field.setAccessible(true);

                String cellValue = getCellValue(row, col, formatter);

                if (!cellValue.isEmpty()) {
                    isEmptyRow = false;
                }

                field.set(obj, cellValue);
            }

            if (!isEmptyRow) {
                result.add(obj);
            }
        }

        return result;
    }

    private static String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null)
            return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return "";

        if (cell.getCellType() == CellType.FORMULA) {
            FormulaEvaluator evaluator = row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
            return formatter.formatCellValue(cell, evaluator).trim();
        }
        return formatter.formatCellValue(cell).trim();
    }
}
