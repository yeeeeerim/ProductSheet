package shop.mtcoding.metamall.core.filter;


import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import shop.mtcoding.metamall.core.exception.Exception400;
import shop.mtcoding.metamall.core.jwt.JwtProvider;
import shop.mtcoding.metamall.core.session.SessionUser;
import shop.mtcoding.metamall.core.util.MyFilterResponseUtil;
import shop.mtcoding.metamall.dto.ResponseDTO;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class JwtVerifyFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("디버그 : JwtVerifyFilter 동작함");
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String prefixJwt = req.getHeader(JwtProvider.HEADER);
        if(prefixJwt == null){
            MyFilterResponseUtil.badRequest(resp,new Exception400("authorization","토큰이 전달되지 않았습니다. "));
            return;
        }
        String jwt = prefixJwt.replace(JwtProvider.TOKEN_PREFIX, "");
        try {
            DecodedJWT decodedJWT = JwtProvider.verify(jwt);
            Long id = decodedJWT.getClaim("id").asLong();
            String role = decodedJWT.getClaim("role").asString();

            // 세션을 사용하는 이유는 권한처리를 하기 위해서이다.
            HttpSession session =  req.getSession();
            SessionUser sessionUser = SessionUser.builder().id(id).role(role).build();
            session.setAttribute("loginUser", sessionUser);
            chain.doFilter(req, resp);
        }catch (SignatureVerificationException sve){
            error(resp, sve);
        }catch (TokenExpiredException tee){
            error(resp, tee);
        }
    }

    private void error(HttpServletResponse resp, Exception e) throws IOException {
        resp.setStatus(401);
        resp.setContentType("application/json; charset=utf-8");
        ResponseDTO<?> responseDto = new ResponseDTO<>().fail(HttpStatus.UNAUTHORIZED, "인증 안됨", e.getMessage());
        ObjectMapper om = new ObjectMapper();
        String responseBody = om.writeValueAsString(responseDto);
        resp.getWriter().println(responseBody);
    }

}
