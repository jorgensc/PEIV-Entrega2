package jcondo.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// sindico vai pro painel, morador vai pro app
public class LoginSucessoHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public LoginSucessoHandler() {
        setDefaultTargetUrl("/painel");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException, ServletException {
        boolean sindico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SINDICO"));
        if (sindico) {
            super.onAuthenticationSuccess(req, res, auth);
        } else {
            res.sendRedirect(req.getContextPath() + "/app/");
        }
    }
}
