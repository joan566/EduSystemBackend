package com.edusistem.core.gradebook.domain.inputports;

import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.StudentObservation;

public interface ManageStudentObservationUseCase {

    /** Devuelve la observación guardada, o nula si el texto venía en blanco (se borró). */
    StudentObservation save(GradebookCommands.SaveObservation command);
}
