package myex.shopping;

import myex.shopping.interceptor.LoginCheckInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebConfigTest {

    @Test
    @DisplayName("정적 리소스 핸들러가 classpath:/static/ 경로를 사용한다")
    void addResourceHandlersUsesStaticDirectory() {
        WebConfig webConfig = new WebConfig(mock(LoginCheckInterceptor.class));

        MockServletContext servletContext = new MockServletContext();
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(servletContext);
        applicationContext.refresh();

        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);
        webConfig.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/**")).isTrue();
        // S3 전환 후 /img/** 로컬 매핑은 더 이상 필요하지 않음
        assertThat(registry.hasMappingForPattern("/img/**")).isFalse();
    }
}
