package com.edusistem.core.shared.domain.outputports;

import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.List;

public interface SpreadsheetWriterPort {

    /** Genera un archivo .xlsx con la hoja indicada. */
    byte[] write(TabularData data);

    /** Genera un archivo .xlsx con varias hojas, en el orden dado. */
    byte[] writeWorkbook(List<TabularData> sheets);
}
