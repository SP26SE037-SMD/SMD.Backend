package com.example.smd.dto.excel;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    String name();     // tên cột trong Excel

    int order();       // vị trí cột

    boolean required() default false;

}
