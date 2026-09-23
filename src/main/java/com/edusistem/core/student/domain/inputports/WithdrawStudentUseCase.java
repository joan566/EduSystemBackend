package com.edusistem.core.student.domain.inputports;

import com.edusistem.core.student.application.use_case.dtos.StudentCommands;

public interface WithdrawStudentUseCase {

    void withdraw(StudentCommands.Withdraw command);
}
