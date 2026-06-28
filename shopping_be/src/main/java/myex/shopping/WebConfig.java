package myex.shopping;

import lombok.RequiredArgsConstructor;
import myex.shopping.interceptor.LoginCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @SuppressWarnings("FieldCanBeLocal")
    private final LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

    //인터셉터 등록
 /*   public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/**") //모든 요청에 적용
                .addPathPatterns("/posts/new")
                .excludePathPatterns(
                        "/login", //로그인 페이지
                        "/logout",//로그아웃
                        "/register", //회원가입 페이지
                        "/", //메인 쇼핑 페이지
                        "/*.css",
                        "/*.js",
                        "/image/**",
                        "/api/**",

                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "swagger-resources/**",
                        "/webjars/**",

                        "/posts",
//                        "/*.ico",
                        "/error"
                );


    }*/
}
