package myex.shopping.controller.web;

import myex.shopping.exception.WebExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WebExceptionHandlerTest {

    @Test
    @DisplayName("회원 이메일 unique 충돌은 웹에서 409 에러 페이지로 변환된다")
    void handleMemberEmailUniqueViolation() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DuplicateEmailWebController())
                .setControllerAdvice(new WebExceptionHandler())
                .build();

        mockMvc.perform(get("/web/test/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "이미 사용 중인 이메일입니다."));
    }

    @Controller
    static class DuplicateEmailWebController {

        @GetMapping("/web/test/duplicate-email")
        String duplicateEmail() {
            throw new DataIntegrityViolationException(
                    "Duplicate entry 'test@example.com' for key 'uk_member_email'");
        }
    }
}
