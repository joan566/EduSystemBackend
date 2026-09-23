package com.edusistem.core.imports.infrastructure.adapter;

import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.ParsedWorkbook;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class PoiSpreadsheetReader implements SpreadsheetReaderPort {

    private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

    @Override
    public ParsedSheet read(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new InvalidRequestException("EMPTY_FILE", "The Excel file has no sheets");
            }
            ParsedSheet sheet = parseSheet(workbook.getSheetAt(0));
            if (sheet.headers().isEmpty()) {
                throw new InvalidRequestException("EMPTY_FILE", "The Excel file is empty");
            }
            return sheet;
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidRequestException invalid) {
                throw invalid;
            }
            throw new InvalidRequestException("INVALID_EXCEL", "The file is not a readable Excel (.xlsx) workbook");
        }
    }

    @Override
    public ParsedWorkbook readAll(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new InvalidRequestException("EMPTY_FILE", "The Excel file has no sheets");
            }
            Map<String, ParsedSheet> sheets = new LinkedHashMap<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheets.put(sheet.getSheetName(), parseSheet(sheet));
            }
            return new ParsedWorkbook(sheets);
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidRequestException invalid) {
                throw invalid;
            }
            throw new InvalidRequestException("INVALID_EXCEL", "The file is not a readable Excel (.xlsx) workbook");
        }
    }

    /** Encabezados vacíos u hoja sin filas: se devuelve una hoja vacía en vez de fallar (varias hojas son opcionales). */
    private ParsedSheet parseSheet(Sheet sheet) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            return new ParsedSheet(List.of(), List.of());
        }
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            headers.add(normalizeHeader(text(headerRow.getCell(c))));
        }
        List<SpreadsheetRow> rows = new ArrayList<>();
        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            boolean blank = true;
            for (int c = 0; c < headers.size(); c++) {
                String value = text(row.getCell(c));
                if (!value.isBlank()) {
                    blank = false;
                }
                if (!headers.get(c).isEmpty()) {
                    values.put(headers.get(c), value);
                }
            }
            if (!blank) {
                rows.add(new SpreadsheetRow(r + 1, values));
            }
        }
        return new ParsedSheet(headers, rows);
    }

    private String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return new BigDecimal(String.valueOf(cell.getNumericCellValue())).stripTrailingZeros().toPlainString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static String normalizeHeader(String header) {
        return header.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }
}
