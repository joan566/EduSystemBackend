package com.edusistem.core.shared.infrastructure.adapter;

import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class PoiSpreadsheetWriter implements SpreadsheetWriterPort {

    @Override
    public byte[] write(TabularData data) {
        return writeWorkbook(List.of(data));
    }

    @Override
    public byte[] writeWorkbook(List<TabularData> sheets) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            CreationHelper helper = workbook.getCreationHelper();
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            CellStyle dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            for (TabularData data : sheets) {
                writeSheet(workbook.createSheet(safeSheetName(data.sheetName())), data, header, date, dateTime);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the spreadsheet", e);
        }
    }

    private static void writeSheet(Sheet sheet, TabularData data, CellStyle header, CellStyle date, CellStyle dateTime) {
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < data.headers().size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(data.headers().get(c));
            cell.setCellStyle(header);
        }
        int rowIndex = 1;
        for (List<Object> values : data.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int c = 0; c < values.size(); c++) {
                write(row.createCell(c), values.get(c), date, dateTime);
            }
        }
        for (int c = 0; c < data.headers().size(); c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private static void write(Cell cell, Object value, CellStyle date, CellStyle dateTime) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal d) {
            cell.setCellValue(d.doubleValue());
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d);
            cell.setCellStyle(date);
        } else if (value instanceof LocalDateTime d) {
            cell.setCellValue(d);
            cell.setCellStyle(dateTime);
        } else {
            cell.setCellValue(neutralize(String.valueOf(value)));
        }
    }

    /** Evita inyección de fórmulas en Excel: los textos que empiezan por = + - @ se guardan como texto literal. */
    private static String neutralize(String text) {
        return !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0 && !text.matches("-?\\d+(\\.\\d+)?")
                ? "'" + text : text;
    }

    private static String safeSheetName(String name) {
        String cleaned = name == null ? "Sheet1" : name.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
