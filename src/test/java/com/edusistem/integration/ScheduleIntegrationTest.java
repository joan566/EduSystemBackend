package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Horario semanal por periodo de enseñanza y agenda del profesor ("Hoy" y calendario). */
class ScheduleIntegrationTest extends IntegrationTest {

    /** Un lunes dentro del periodo académico de {@link #newContext} (15-ene a 1-abr). */
    private static final LocalDate MONDAY = LocalDate.of(YEAR, 2, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    private static String schedulesUrl(Context c) {
        return "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/schedules";
    }

    private static Map<String, Object> block(String day, String start, String end, String room) {
        Map<String, Object> body = new HashMap<>(Map.of("dayOfWeek", day, "startTime", start, "endTime", end));
        body.put("room", room);
        return body;
    }

    @Test
    void schedulesAreValidatedOrderedAndOwned() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        String url = schedulesUrl(c);

        JsonNode tuesday = post(t, url, block("TUESDAY", "09:00", "10:00", " Aula 3 "), 201);
        assertThat(tuesday.get("startTime").asText()).isEqualTo("09:00");
        assertThat(tuesday.get("room").asText()).isEqualTo("Aula 3");
        post(t, url, block("MONDAY", "08:00", "09:00", null), 201);
        post(t, url, block("MONDAY", "07:00", "08:00", ""), 201);

        JsonNode list = get(t, url, 200);
        assertThat(list).hasSize(3);
        assertThat(list.get(0).get("startTime").asText()).isEqualTo("07:00");
        assertThat(list.get(0).get("room").isNull()).isTrue();
        assertThat(list.get(1).get("startTime").asText()).isEqualTo("08:00");
        assertThat(list.get(2).get("dayOfWeek").asText()).isEqualTo("TUESDAY");

        assertError(post(t, url, block("MONDAY", "10:00", "10:00", null), 400), 400, "INVALID_SCHEDULE_TIMES");
        assertError(post(t, url, block("MONDAY", "11:00", "10:00", null), 400), 400, "INVALID_SCHEDULE_TIMES");
        assertError(post(t, url, Map.of("dayOfWeek", "MONDAY", "startTime", "10:00"), 400), 400, "INVALID_REQUEST");

        long id = tuesday.get("id").asLong();
        JsonNode updated = put(t, url + "/" + id, block("TUESDAY", "09:30", "10:30", "Lab"), 200);
        assertThat(updated.get("endTime").asText()).isEqualTo("10:30");

        Teacher other = newTeacher();
        assertError(get(other, url, 404), 404, "RESOURCE_NOT_FOUND");
        assertError(post(other, url, block("FRIDAY", "07:00", "08:00", null), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(put(other, url + "/" + id, block("TUESDAY", "09:30", "10:30", null), 404), 404, "RESOURCE_NOT_FOUND");

        // el bloque debe pertenecer al periodo de enseñanza de la URL
        Context second = newContext(t);
        assertError(put(t, schedulesUrl(second) + "/" + id, block("TUESDAY", "09:30", "10:30", null), 404), 404,
                "RESOURCE_NOT_FOUND");

        assertThat(perform("DELETE", t, url + "/" + id, null).getResponse().getStatus()).isEqualTo(204);
        assertThat(get(t, url, 200)).hasSize(2);
    }

    @Test
    void aTeacherCannotHaveTwoOverlappingClassesOnTheSameDay() {
        Teacher t = newTeacher();
        Context math = newContext(t);
        Context science = newContext(t);
        JsonNode first = post(t, schedulesUrl(math), block("MONDAY", "07:00", "08:00", null), 201);

        JsonNode conflict = post(t, schedulesUrl(science), block("MONDAY", "07:30", "08:30", null), 409);
        assertError(conflict, 409, "SCHEDULE_CONFLICT");
        assertError(post(t, schedulesUrl(math), block("MONDAY", "06:00", "09:00", null), 409), 409, "SCHEDULE_CONFLICT");

        // bloques contiguos, otro día u otro profesor no chocan
        post(t, schedulesUrl(science), block("MONDAY", "08:00", "09:00", null), 201);
        post(t, schedulesUrl(science), block("TUESDAY", "07:00", "08:00", null), 201);
        Teacher other = newTeacher();
        post(other, schedulesUrl(newContext(other)), block("MONDAY", "07:00", "08:00", null), 201);

        // editar un bloque no choca consigo mismo, pero sí con los demás
        put(t, schedulesUrl(math) + "/" + first.get("id").asLong(), block("MONDAY", "06:30", "07:45", "B"), 200);
        assertError(put(t, schedulesUrl(math) + "/" + first.get("id").asLong(), block("MONDAY", "07:30", "08:15", null), 409),
                409, "SCHEDULE_CONFLICT");

        // periodos académicos que no coexisten en fechas no chocan entre sí
        long laterPeriod = post(t, "/api/v1/academic-periods", Map.of("name", unique("P2"),
                "startDate", YEAR + "-06-01", "endDate", YEAR + "-09-30"), 201).get("id").asLong();
        long laterTp = post(t, "/api/v1/teaching-periods", Map.of("teachingAssignmentId", math.assignmentId(),
                "academicPeriodId", laterPeriod), 201).get("id").asLong();
        post(t, "/api/v1/teaching-periods/" + laterTp + "/schedules", block("MONDAY", "07:00", "08:00", null), 201);
    }

    @Test
    void todayReturnsTheClassesOfThatDaySortedByStartTime() {
        Teacher t = newTeacher();
        Context math = newContext(t);
        Context science = newContext(t);
        post(t, schedulesUrl(science), block("MONDAY", "10:00", "11:00", "Lab"), 201);
        post(t, schedulesUrl(math), block("MONDAY", "07:00", "08:00", null), 201);
        post(t, schedulesUrl(math), block("TUESDAY", "07:00", "08:00", null), 201);

        JsonNode today = get(t, "/api/v1/schedule/today?date=" + MONDAY, 200);
        assertThat(today.get("date").asText()).isEqualTo(MONDAY.toString());
        assertThat(today.get("timezone").asText()).isEqualTo("America/Bogota");
        assertThat(today.get("serverTime").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        JsonNode classes = today.get("classes");
        assertThat(classes).hasSize(2);
        JsonNode first = classes.get(0);
        assertThat(first.get("teachingPeriodId").asLong()).isEqualTo(math.teachingPeriodId());
        assertThat(first.get("subjectId").asLong()).isEqualTo(math.subjectId());
        assertThat(first.get("groupId").asLong()).isEqualTo(math.groupId());
        assertThat(first.get("gradeName").asText()).isEqualTo(math.gradeName());
        assertThat(first.get("groupName").asText()).isEqualTo(math.groupName());
        assertThat(first.has("subjectName")).isTrue();
        assertThat(first.has("academicPeriodName")).isTrue();
        assertThat(first.get("startTime").asText()).isEqualTo("07:00");
        assertThat(first.get("endTime").asText()).isEqualTo("08:00");
        assertThat(first.get("room").isNull()).isTrue();
        assertThat(classes.get(1).get("room").asText()).isEqualTo("Lab");

        // fuera del periodo académico no hay clases
        LocalDate mondayAfterPeriod = LocalDate.of(YEAR, 4, 2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertThat(get(t, "/api/v1/schedule/today?date=" + mondayAfterPeriod, 200).get("classes")).isEmpty();

        // una asignación desactivada desaparece de la agenda
        call("PATCH", t, "/api/v1/teaching-assignments/" + science.assignmentId() + "/active", Map.of("active", false), 200);
        assertThat(get(t, "/api/v1/schedule/today?date=" + MONDAY, 200).get("classes")).hasSize(1);

        // sin fecha usa el día actual del colegio
        JsonNode now = get(t, "/api/v1/schedule/today", 200);
        assertThat(now.get("date").asText()).isEqualTo(now.get("serverTime").asText().substring(0, 10));
    }

    @Test
    void rangeListsEveryDayBetweenFromAndTo() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        post(t, schedulesUrl(c), block("MONDAY", "07:00", "08:00", null), 201);
        post(t, schedulesUrl(c), block("WEDNESDAY", "09:00", "10:00", null), 201);

        JsonNode week = get(t, "/api/v1/schedule?from=" + MONDAY + "&to=" + MONDAY.plusDays(6), 200);
        assertThat(week.get("from").asText()).isEqualTo(MONDAY.toString());
        JsonNode days = week.get("days");
        assertThat(days).hasSize(7);
        List<Integer> counts = new ArrayList<>();
        days.forEach(d -> counts.add(d.get("classes").size()));
        assertThat(counts).containsExactly(1, 0, 1, 0, 0, 0, 0);
        assertThat(days.get(2).get("dayOfWeek").asText()).isEqualTo("WEDNESDAY");

        assertThat(get(t, "/api/v1/schedule", 200).get("days")).hasSize(7);
        assertError(get(t, "/api/v1/schedule?from=" + MONDAY + "&to=" + MONDAY.minusDays(1), 400), 400, "INVALID_DATE_RANGE");
        assertError(get(t, "/api/v1/schedule?from=" + MONDAY + "&to=" + MONDAY.plusDays(200), 400), 400, "INVALID_DATE_RANGE");
    }

    @Test
    void teachingPeriodsIncludeTheActiveStudentCount() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        importStudents(t, c, 3);
        assertThat(get(t, "/api/v1/teaching-periods/" + c.teachingPeriodId(), 200).get("studentCount").asLong())
                .isEqualTo(3);
        JsonNode page = get(t, "/api/v1/teaching-periods", 200);
        assertThat(page.get("content").get(0).get("studentCount").asLong()).isEqualTo(3);
    }
}
