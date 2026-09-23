CREATE TABLE student_groups (
    student_id   BIGINT    NOT NULL,
    group_id     BIGINT    NOT NULL,
    enrolled_at  TIMESTAMP NOT NULL,
    withdrawn_at TIMESTAMP,
    active       BOOLEAN   NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_student_groups PRIMARY KEY (student_id, group_id),
    CONSTRAINT fk_student_groups_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_student_groups_group FOREIGN KEY (group_id) REFERENCES groups (id)
);
