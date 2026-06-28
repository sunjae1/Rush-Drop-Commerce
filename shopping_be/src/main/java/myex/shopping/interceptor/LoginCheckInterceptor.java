package myex.shopping.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.User;
import myex.shopping.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        String uri = request.getRequestURI();

        // /posts 관련 처리
        if (uri.matches("/posts/\\d+")) {
            log.info("posts/{id} 로그인 없이 통과 로직");
            // 게시물 리스트, 단일 게시물: 로그인 없이 접근
            // /posts/{id} 검사 없이 통과 (로그인 X)
            return true;
        }

        //로그인된 사용자 확인
        if (session == null || session.getAttribute("loginUser") == null) {
            //request.getSession(false) : 기존 세션 있으면 return, 없으면 null
            //request.getSession(true) : 기존 세션 있으면 return, 없으면 새 세션 생성하여 반환.
            HttpSession validSession = request.getSession(true);
            validSession.setAttribute("needLoginMessage", "로그인이 필요한 작업입니다.");
            response.sendRedirect("/login");
            return false;
        }

        return true; //통과

    }
}
