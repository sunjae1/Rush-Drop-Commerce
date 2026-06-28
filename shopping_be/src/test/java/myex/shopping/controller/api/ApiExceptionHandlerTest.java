package myex.shopping.controller.api;

import myex.shopping.exception.ApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    @Test
    @DisplayName("회원 이메일 unique 충돌은 API에서 409로 변환된다")
    void handleMemberEmailUniqueViolation() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DuplicateEmailApiController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/api/test/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("다른 무결성 오류는 중복 이메일로 오인하지 않는다")
    void handleOtherIntegrityViolation() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OtherIntegrityApiController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(get("/api/test/other-integrity"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));
    }

    @RestController
    static class DuplicateEmailApiController {

        @GetMapping("/api/test/duplicate-email")
        String duplicateEmail() {
            throw new DataIntegrityViolationException(
                    "could not execute statement [Unique index or primary key violation: "
                            + "\"PUBLIC.CONSTRAINT_INDEX_F ON PUBLIC.MEMBER(EMAIL NULLS FIRST) "
                            + "VALUES ('test@example.com')\"; SQL [insert into member (email) values (?)]]");
        }
    }

    @RestController
    static class OtherIntegrityApiController {

        @GetMapping("/api/test/other-integrity")
        String otherIntegrity() {
            throw new DataIntegrityViolationException(
                    "could not execute statement [Unique index or primary key violation: "
                            + "\"PUBLIC.UK_CART_USER ON PUBLIC.CART(USER_ID NULLS FIRST) VALUES (1)\"; "
                            + "SQL [insert into cart (user_id) values (?)]]");
        }
    }
}
