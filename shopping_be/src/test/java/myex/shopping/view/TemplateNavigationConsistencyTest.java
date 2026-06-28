package myex.shopping.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateNavigationConsistencyTest {

    @Test
    @DisplayName("nav가 있는 SSR 템플릿은 공개 카테고리 페이지 링크를 제공한다")
    void navTemplatesShouldExposePublicCategoryLink() throws IOException {
        Path templatesRoot = Path.of("src/main/resources/templates");

        List<Path> navTemplates;
        try (var paths = Files.walk(templatesRoot)) {
            navTemplates = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html"))
                    .filter(this::containsNav)
                    .toList();
        }

        assertThat(navTemplates).isNotEmpty();

        for (Path template : navTemplates) {
            String content = Files.readString(template);
            assertThat(content)
                    .as(template.toString())
                    .doesNotContain("카테고리 관리 페이지");
            assertThat(content.contains("@{/categories}") || content.contains("fragments/nav :: nav"))
                    .as(template.toString())
                    .isTrue();
        }
    }

    private boolean containsNav(Path path) {
        try {
            return Files.readString(path).contains("<nav");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
