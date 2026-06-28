package myex.shopping.exception;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;

public final class DatabaseConstraintUtils {

    public static final String DUPLICATE_EMAIL_MESSAGE = "이미 사용 중인 이메일입니다.";

    private DatabaseConstraintUtils() {
    }

    public static boolean isUserEmailUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && isUserEmailConstraintName(constraintViolationException.getConstraintName())) {
                return true;
            }

            if (isUserEmailUniqueViolationMessage(current.getMessage())) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private static boolean isUserEmailConstraintName(String constraintName) {
        if (constraintName == null) {
            return false;
        }

        String normalized = constraintName.toLowerCase(Locale.ROOT);
        return normalized.contains("uk_member_email")
                || normalized.contains("member_email")
                || normalized.contains("member.email");
    }

    private static boolean isUserEmailUniqueViolationMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        boolean refersToMemberEmail = normalized.contains("uk_member_email")
                || normalized.contains("member.email")
                || normalized.contains("member (email")
                || normalized.contains("member(email")
                || normalized.contains("member_email");
        boolean isUniqueViolation = normalized.contains("duplicate")
                || normalized.contains("unique")
                || normalized.contains("23505"); // PostgreSQL unique violation error code

        return refersToMemberEmail && isUniqueViolation;
    }
}
