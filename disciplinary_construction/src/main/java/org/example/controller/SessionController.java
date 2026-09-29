package org.example.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
public class SessionController {
    @PostMapping("/msi/session/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from("mis_file_session", "").httpOnly(true)
                .sameSite("Strict").path(request.getContextPath() + "/uploads").maxAge(0).build().toString());
        response.setStatus(204);
    }
}
