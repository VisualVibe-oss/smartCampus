package com.example.demo;

import com.example.demo.repository.EquipmentRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired EquipmentRepository equipmentRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired UserRepository userRepository;
    @Autowired ReservationRepository reservationRepository;

    @BeforeEach
    void resetDatabase() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        equipmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completeCrudScenarioWithFlatResponsesAndDefaults() throws Exception {
        long equipmentId = createEquipment();
        long userId = createUser();
        long roomId = createRoom(equipmentId);
        long reservationId = createReservation(roomId, userId);

        mvc.perform(get("/api/equipment/{id}", equipmentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Projecteur"));
        mvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.equipmentIds[0]").value(equipmentId))
                .andExpect(jsonPath("$.equipment").doesNotExist());
        mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.room").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        for (String resource : new String[]{"equipment", "rooms", "users", "reservations"}) {
            mvc.perform(get("/api/" + resource))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        }
        mvc.perform(get("/api/users"))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        putJson("/api/equipment/" + equipmentId, """
                {"name":"Tableau interactif"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Tableau interactif"));
        putJson("/api/rooms/" + roomId, """
                {"name":"A101","building":"Sciences","capacity":40,"description":"TP","active":false,"equipmentIds":[]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(40))
                .andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.equipmentIds").isEmpty());
        putJson("/api/users/" + userId, """
                {"email":"amina@example.test","fullName":"Amina Mise à jour","role":"PROFESSOR","enabled":false}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Amina Mise à jour"))
                .andExpect(jsonPath("$.role").value("PROFESSOR"))
                .andExpect(jsonPath("$.enabled").value(false)).andExpect(jsonPath("$.password").doesNotExist());
        putJson("/api/reservations/" + reservationId, reservationJson(roomId, userId,
                "2026-11-02T11:00:00Z", "2026-11-02T12:00:00Z", ",\"status\":\"CANCELLED\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        for (String path : new String[]{"reservations/" + reservationId, "rooms/" + roomId,
                "equipment/" + equipmentId, "users/" + userId}) {
            mvc.perform(delete("/api/" + path)).andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            mvc.perform(get("/api/" + path)).andExpect(status().isNotFound());
        }
    }

    @Test
    void missingResourcesAndRelationsReturn404() throws Exception {
        mvc.perform(get("/api/rooms/999999")).andExpect(status().isNotFound());
        putJson("/api/equipment/999999", "{\"name\":\"Absent\"}")
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/users/999999")).andExpect(status().isNotFound());
        postJson("/api/rooms", """
                {"name":"A101","building":"Sciences","capacity":30,"equipmentIds":[999999]}
                """).andExpect(status().isNotFound());
        postJson("/api/reservations", reservationJson(999999, 999999,
                "2026-11-02T09:00:00Z", "2026-11-02T10:00:00Z", ""))
                .andExpect(status().isNotFound());
        long roomId = createRoom(createEquipment());
        postJson("/api/reservations", reservationJson(roomId, 999999,
                "2026-11-02T09:00:00Z", "2026-11-02T10:00:00Z", ""))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidInputsAndReservationDatesReturn400() throws Exception {
        postJson("/api/equipment", "{\"name\":\"  \"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details.name").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
        postJson("/api/equipment", "{\"id\":42,\"name\":\"Projecteur\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/rooms", """
                {"name":"A101","building":"Sciences","capacity":30,"equipmentIds":[],"equipment":[{"name":"Client"}]}
                """).andExpect(status().isBadRequest());
        postJson("/api/rooms", """
                {"name":"A101","building":"Sciences","capacity":0,"equipmentIds":[]}
                """).andExpect(status().isBadRequest());
        postJson("/api/users", """
                {"email":"incorrect","password":"secret123","fullName":"Amina","role":"STUDENT"}
                """).andExpect(status().isBadRequest());
        postJson("/api/reservations", "{\"title\":\"TP\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms/abc")).andExpect(status().isBadRequest());

        long roomId = createRoom(createEquipment());
        long userId = createUser();
        postJson("/api/reservations", reservationJson(roomId, userId,
                "2026-11-02T10:00:00Z", "2026-11-02T09:00:00Z", ""))
                .andExpect(status().isBadRequest());
        postJson("/api/reservations", reservationJson(roomId, userId,
                "2026-11-02T10:00:00Z", "2026-11-02T10:00:00Z", ""))
                .andExpect(status().isBadRequest());
        postJson("/api/reservations", reservationJson(roomId, userId,
                "2026-11-02T10:00:00", "2026-11-02T11:00:00", ""))
                .andExpect(status().isBadRequest());
        long reservationId = createReservation(roomId, userId);
        putJson("/api/reservations/" + reservationId, reservationJson(roomId, userId,
                "2026-11-02T12:00:00Z", "2026-11-02T11:00:00Z", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uniquenessAndReferencedDeletesReturn409() throws Exception {
        long equipmentId = createEquipment();
        long userId = createUser();
        postJson("/api/equipment", "{\"name\":\"Projecteur\"}").andExpect(status().isConflict());
        postJson("/api/users", userJson(",\"password\":\"autreSecret123\""))
                .andExpect(status().isConflict());
        long roomId = createRoom(equipmentId);
        long reservationId = createReservation(roomId, userId);

        mvc.perform(delete("/api/equipment/{id}", equipmentId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/rooms/{id}", roomId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isConflict());
        assertThat(reservationRepository.existsById(reservationId)).isTrue();
        assertThat(roomRepository.existsById(roomId)).isTrue();
        assertThat(userRepository.existsById(userId)).isTrue();
        assertThat(equipmentRepository.existsById(equipmentId)).isTrue();
    }

    @Test
    void passwordsAreRequiredOnCreationHashedAndOptionalOnUpdate() throws Exception {
        postJson("/api/users", userJson("")).andExpect(status().isBadRequest());
        postJson("/api/users", userJson(",\"password\":\"court\""))
                .andExpect(status().isBadRequest());
        postJson("/api/users", userJson(",\"password\":\"" + "é".repeat(37) + "\""))
                .andExpect(status().isBadRequest());
        long userId = createUser();
        var encoder = new BCryptPasswordEncoder();
        var user = userRepository.findById(userId).orElseThrow();
        String firstHash = user.getPassword();
        var createdAt = user.getCreatedAt();
        assertThat(firstHash).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", firstHash)).isTrue();

        putJson("/api/users/" + userId, userJson(""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        assertThat(userRepository.findById(userId).orElseThrow().getPassword()).isEqualTo(firstHash);
        putJson("/api/users/" + userId, userJson(",\"password\":\"court\""))
                .andExpect(status().isBadRequest());
        putJson("/api/users/" + userId, userJson(",\"password\":\"nouveauSecret123\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        var updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getPassword()).isNotEqualTo(firstHash);
        assertThat(encoder.matches("nouveauSecret123", updated.getPassword())).isTrue();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    private long createEquipment() throws Exception {
        return createdId(postJson("/api/equipment", "{\"name\":\"Projecteur\"}"), "/api/equipment/");
    }

    private long createUser() throws Exception {
        return createdId(postJson("/api/users", userJson(",\"password\":\"secret123\""))
                .andExpect(jsonPath("$.password").doesNotExist()), "/api/users/");
    }

    private long createRoom(long equipmentId) throws Exception {
        return createdId(postJson("/api/rooms", """
                {"name":"A101","building":"Sciences","capacity":30,"equipmentIds":[%d]}
                """.formatted(equipmentId)), "/api/rooms/");
    }

    private long createReservation(long roomId, long userId) throws Exception {
        return createdId(postJson("/api/reservations", reservationJson(roomId, userId,
                "2026-11-02T09:00:00Z", "2026-11-02T10:00:00Z", "")), "/api/reservations/");
    }

    private long createdId(ResultActions result, String locationPrefix) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.id");
        result.andExpect(header().string("Location", locationPrefix + id.longValue()));
        return id.longValue();
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putJson(String path, String json) throws Exception {
        return mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String userJson(String passwordField) {
        return """
                {"email":"amina@example.test","fullName":"Amina Benali","role":"STUDENT"%s}
                """.formatted(passwordField);
    }

    private String reservationJson(long roomId, long userId, String startsAt, String endsAt, String extra) {
        return """
                {"roomId":%d,"userId":%d,"title":"TP Java","startsAt":"%s","endsAt":"%s"%s}
                """.formatted(roomId, userId, startsAt, endsAt, extra);
    }
}
