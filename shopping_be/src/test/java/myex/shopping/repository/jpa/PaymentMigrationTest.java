package myex.shopping.repository.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThatCode;

class PaymentMigrationTest {

    @Test
    @DisplayName("결제 테이블 Flyway 제약조건은 Toss provider 저장을 허용한다")
    void paymentProviderConstraintAllowsToss() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:payment-migration;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    status VARCHAR(30) NULL
                )
                """);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V8__create_payment_table.sql"));
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V9__allow_toss_payment_provider.sql"));
        }

        jdbcTemplate.update("INSERT INTO orders (status) VALUES ('PAYMENT_PENDING')");

        assertThatCode(() -> jdbcTemplate.update("""
                INSERT INTO payment (
                    order_id,
                    provider,
                    status,
                    payment_order_id,
                    amount,
                    requested_at
                )
                VALUES (1, 'TOSS', 'READY', 'toss_test_order', 1000, CURRENT_TIMESTAMP)
                """)).doesNotThrowAnyException();
    }
}
