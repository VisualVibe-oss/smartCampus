package com.example.demo;

import com.example.demo.repository.EquipmentRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
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

    private static final String FUTURE_START = "2026-12-01T09:00:00Z";
    private static final String FUTURE_END = "2026-12-08T09:00:00Z";

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
        String equipmentId = createEquipment();
        String userId = createUser();
        String roomId = createRoom(equipmentId);
        String reservationId = createReservation(roomId, userId);

        mvc.perform(get("/api/equipment/{id}", equipmentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Projecteur"));
        mvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.code").value("A101"))
                .andExpect(jsonPath("$.equipmentIds[0]").value(equipmentId))
                .andExpect(jsonPath("$.equipment").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
        mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startAt").value("2026-12-01T09:00:00Z"))
                .andExpect(jsonPath("$.endAt").value(FUTURE_END))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.room").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        for (String resource : new String[]{"equipment", "rooms", "users", "reservations"}) {
            mvc.perform(get("/api/" + resource))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        }
        mvc.perform(get("/api/users"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        putJson("/api/equipment/" + equipmentId, """
                {"name":"Tableau interactif"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Tableau interactif"));
        putJson("/api/reservations/" + reservationId, reservationJson(roomId, userId, FUTURE_START,
                ",\"status\":\"CANCELLED\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isString());
        putJson("/api/rooms/" + roomId, """
                {"code":"B202","name":"B202","building":"Sciences","capacity":40,"active":false,"equipmentIds":[]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(40))
                .andExpect(jsonPath("$.code").value("B202")).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.equipmentIds").isEmpty());
        putJson("/api/users/" + userId, """
                {"email":"amina@example.test","fullName":"Amina Mise à jour","role":"STAFF","active":false}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Amina Mise à jour"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.passwordHash").doesNotExist());

        for (String path : new String[]{"reservations/" + reservationId, "rooms/" + roomId,
                "equipment/" + equipmentId, "users/" + userId}) {
            mvc.perform(delete("/api/" + path)).andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            mvc.perform(get("/api/" + path)).andExpect(status().isNotFound());
        }
    }

    @Test
    void reservationLastsExactlySevenDaysAndConflictsAreRejected() throws Exception {
        String equipmentId = createEquipment();
        String userId = createUser();
        String otherUserId = createUserOther();
        String roomId = createRoom(equipmentId);
        createReservation(roomId, userId);

        postJson("/api/reservations", reservationJson(roomId, otherUserId,
                "2026-12-05T09:00:00Z", ""))
                .andExpect(status().isConflict());

        postJson("/api/reservations", reservationJson(roomId, otherUserId,
                "2026-12-08T09:00:00Z", ""))
                .andExpect(status().isCreated());
    }

    @Test
    void reservationRulesRejectPastStartInactiveRoomAndInactiveUser() throws Exception {
        String equipmentId = createEquipment();
        String roomId = createRoom(equipmentId);
        String userId = createUser();

        String pastStart = Instant.now().minus(Duration.ofDays(1)).toString();
        postJson("/api/reservations", reservationJson(roomId, userId, pastStart, ""))
                .andExpect(status().isBadRequest());

        String inactiveRoomId = createRoomInactive(equipmentId);
        postJson("/api/reservations", reservationJson(inactiveRoomId, userId, FUTURE_START, ""))
                .andExpect(status().isBadRequest());

        String inactiveUserId = createUserInactive();
        postJson("/api/reservations", reservationJson(roomId, inactiveUserId, FUTURE_START, ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revokeRequiresRevokerAndRecordsRevocation() throws Exception {
        String equipmentId = createEquipment();
        String userId = createUser();
        String adminId = createAdmin();
        String roomId = createRoom(equipmentId);
        String reservationId = createReservation(roomId, userId);

        putJson("/api/reservations/" + reservationId,
                reservationJson(roomId, userId, FUTURE_START, ",\"status\":\"REVOKED\""))
                .andExpect(status().isBadRequest());

        putJson("/api/reservations/" + reservationId, reservationJson(roomId, userId, FUTURE_START,
                ",\"status\":\"REVOKED\",\"revokedBy\":\"" + adminId + "\",\"revocationReason\":\"Cours prioritaire\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedBy").value(adminId))
                .andExpect(jsonPath("$.revokedAt").isString())
                .andExpect(jsonPath("$.revocationReason").value("Cours prioritaire"));
    }

    @Test
    void roomCodeMustBeUniqueWithinBuilding() throws Exception {
        String equipmentId = createEquipment();
        createRoom(equipmentId);
        postJson("/api/rooms", """
                {"code":"A101","name":"Autre","building":"Sciences","capacity":20,"equipmentIds":[]}
                """).andExpect(status().isConflict());
    }

    @Test
    void missingResourcesAndRelationsReturn404() throws Exception {
        mvc.perform(get("/api/rooms/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        putJson("/api/equipment/00000000-0000-0000-0000-000000000000", "{\"name\":\"Absent\"}")
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/users/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        postJson("/api/rooms", """
                {"code":"A101","name":"A101","building":"Sciences","capacity":30,
                 "equipmentIds":["00000000-0000-0000-0000-000000000000"]}
                """).andExpect(status().isNotFound());
    }

    @Test
    void invalidInputsReturn400() throws Exception {
        postJson("/api/equipment", "{\"name\":\"  \"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details.name").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
        postJson("/api/equipment", "{\"id\":\"abc\",\"name\":\"Projecteur\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/rooms", """
                {"code":"A101","name":"A101","building":"Sciences","capacity":0,"equipmentIds":[]}
                """).andExpect(status().isBadRequest());
        postJson("/api/users", """
                {"email":"incorrect","password":"secret123","fullName":"Amina","role":"STUDENT"}
                """).andExpect(status().isBadRequest());
        postJson("/api/reservations", "{\"roomId\":\"abc\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/rooms/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void uniquenessAndReferencedDeletesReturn409() throws Exception {
        String equipmentId = createEquipment();
        String userId = createUser();
        postJson("/api/equipment", "{\"name\":\"Projecteur\"}").andExpect(status().isConflict());
        postJson("/api/users", userJson(",\"password\":\"autreSecret123\""))
                .andExpect(status().isConflict());
        String roomId = createRoom(equipmentId);
        String reservationId = createReservation(roomId, userId);

        mvc.perform(delete("/api/equipment/{id}", equipmentId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/rooms/{id}", roomId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isConflict());
        assertThat(reservationRepository.existsById(java.util.UUID.fromString(reservationId))).isTrue();
        assertThat(roomRepository.existsById(java.util.UUID.fromString(roomId))).isTrue();
        assertThat(userRepository.existsById(java.util.UUID.fromString(userId))).isTrue();
        assertThat(equipmentRepository.existsById(java.util.UUID.fromString(equipmentId))).isTrue();
    }

    @Test
    void passwordsAreRequiredOnCreationHashedAndOptionalOnUpdate() throws Exception {
        postJson("/api/users", userJson("")).andExpect(status().isBadRequest());
        postJson("/api/users", userJson(",\"password\":\"court\""))
                .andExpect(status().isBadRequest());
        postJson("/api/users", userJson(",\"password\":\"" + "é".repeat(37) + "\""))
                .andExpect(status().isBadRequest());
        String userId = createUser();
        var encoder = new BCryptPasswordEncoder();
        var user = userRepository.findById(java.util.UUID.fromString(userId)).orElseThrow();
        String firstHash = user.getPasswordHash();
        var createdAt = user.getCreatedAt();
        assertThat(firstHash).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", firstHash)).isTrue();

        putJson("/api/users/" + userId, userJson(""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(userRepository.findById(java.util.UUID.fromString(userId)).orElseThrow()
                .getPasswordHash()).isEqualTo(firstHash);
        putJson("/api/users/" + userId, userJson(",\"password\":\"court\""))
                .andExpect(status().isBadRequest());
        putJson("/api/users/" + userId, userJson(",\"password\":\"nouveauSecret123\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        var updated = userRepository.findById(java.util.UUID.fromString(userId)).orElseThrow();
        assertThat(updated.getPasswordHash()).isNotEqualTo(firstHash);
        assertThat(encoder.matches("nouveauSecret123", updated.getPasswordHash())).isTrue();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    private String createEquipment() throws Exception {
        return createdId(postJson("/api/equipment", "{\"name\":\"Projecteur\"}"), "/api/equipment/");
    }

    private String createUser() throws Exception {
        return createdId(postJson("/api/users", userJson(",\"password\":\"secret123\""))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()), "/api/users/");
    }

    private String createUserOther() throws Exception {
        return createdId(postJson("/api/users", """
                {"email":"youssef@example.test","password":"secret123","fullName":"Youssef","role":"STAFF"}
                """), "/api/users/");
    }

    private String createUserInactive() throws Exception {
        return createdId(postJson("/api/users", """
                {"email":"inactif@example.test","password":"secret123","fullName":"Inactif",
                 "role":"STUDENT","active":false}
                """), "/api/users/");
    }

    private String createAdmin() throws Exception {
        return createdId(postJson("/api/users", """
                {"email":"admin@example.test","password":"secret123","fullName":"Admin","role":"ADMIN"}
                """), "/api/users/");
    }

    private String createRoom(String equipmentId) throws Exception {
        return createdId(postJson("/api/rooms", """
                {"code":"A101","name":"A101","building":"Sciences","capacity":30,"equipmentIds":["%s"]}
                """.formatted(equipmentId)), "/api/rooms/");
    }

    private String createRoomInactive(String equipmentId) throws Exception {
        return createdId(postJson("/api/rooms", """
                {"code":"C303","name":"C303","building":"Sciences","capacity":10,
                 "active":false,"equipmentIds":["%s"]}
                """.formatted(equipmentId)), "/api/rooms/");
    }

    private String createReservation(String roomId, String userId) throws Exception {
        return createdId(postJson("/api/reservations", reservationJson(roomId, userId, FUTURE_START, "")),
                "/api/reservations/");
    }

    private String createdId(ResultActions result, String locationPrefix) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        result.andExpect(header().string("Location", locationPrefix + id));
        return id;
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putJson(String path, String json) throws Exception {
        return mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String userJson(String extraFields) {
        return """
                {"email":"amina@example.test","fullName":"Amina Benali","role":"STUDENT"%s}
                """.formatted(extraFields);
    }

    private String reservationJson(String roomId, String userId, String startAt, String extra) {
        return """
                {"roomId":"%s","userId":"%s","startAt":"%s"%s}
                """.formatted(roomId, userId, startAt, extra);
    }
}
