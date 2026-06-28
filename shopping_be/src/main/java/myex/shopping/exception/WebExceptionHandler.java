package myex.shopping.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice(basePackages = "myex.shopping.controller.web")
public class WebExceptionHandler {

    // 타입 미스매치 처리
    @ExceptionHandler(TypeMismatchException.class)
    public String handleTypeMismatch(TypeMismatchException ex, Model model) {
        Map<String, String> error = new HashMap<>();
        String field = (ex.getPropertyName() == null) ? null : ex.getPropertyName();
        String invalidValue = (ex.getValue() == null) ? null : ex.getValue().toString();
        String requiredType = (ex.getRequiredType() == null) ? null : ex.getRequiredType().getSimpleName();
        error.put(field, String.format("'%s'은(는) %s 타입으로 변환할 수 없습니다.", invalidValue, requiredType));
        model.addAttribute("errorMessage", error.get(field));
        return "error/404";
    }

    // 리소스를 찾을 수 없는 경우 (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        return "error/404";
    }

    // 접근 권한이 없는 경우 (403)
    @ExceptionHandler({AccessDeniedException.class, AccessForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Exception e, Model model) {
        model.addAttribute("errorMessage", "접근 권한이 없습니다.");
        return "error/403";
    }

    // 애플리케이션 단에서 중복 리소스 검증(409)
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDuplicateResource(DuplicateResourceException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    // DB 단에서 중복 리소스 및 무결성 Exception Handler (409 또는 500)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException e, Model model, HttpServletResponse response) {
        if (DatabaseConstraintUtils.isUserEmailUniqueViolation(e)) {
            response.setStatus(HttpStatus.CONFLICT.value());
            model.addAttribute("errorMessage", DatabaseConstraintUtils.DUPLICATE_EMAIL_MESSAGE);
            return "error";
        }

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("errorMessage", "데이터 무결성 오류가 발생했습니다.");
        return "error";
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientStock(InsufficientStockException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        return "error";
    }

    // 일반 서버 오류 (500)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception e, Model model) {
        model.addAttribute("errorMessage", "시스템 오류가 발생했습니다.");
        return "error";
    }
}
