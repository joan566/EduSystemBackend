package com.edusistem.core.student.domain.outputports;

import com.edusistem.core.student.domain.entity.Student;
import java.util.List;

/** Borrado definitivo de un estudiante y de todo lo que lo referencia. */
public interface StudentErasurePort {

    /** Borra las filas (y anonimiza la auditoría del profesor); devuelve las rutas de archivos que hay que borrar. */
    List<String> erase(Long teacherId, Student student);
}
