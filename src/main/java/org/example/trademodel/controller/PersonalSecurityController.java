package org.example.trademodel.controller;

import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.MultiUserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PersonalSecurityController {
    private final AuthenticatedUserIdResolver userResolver;
    private final MultiUserAccountService accountService;

    public PersonalSecurityController(AuthenticatedUserIdResolver userResolver,
                                      MultiUserAccountService accountService) {
        this.userResolver = userResolver;
        this.accountService = accountService;
    }

    @GetMapping("/me/security")
    public String form(Model model) {
        model.addAttribute("username", userResolver.requireCurrentUser().getUsername());
        return "me-security";
    }

    @PostMapping("/me/security")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String passwordConfirmation,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 Model model) {
        var currentUser = userResolver.requireCurrentUser();
        try {
            accountService.changeOwnPassword(currentUser.getId(), currentPassword,
                    newPassword, passwordConfirmation);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("passwordError",
                    "当前密码不正确，或新密码必须正好8个字符，且不能使用默认密码或与用户名相同。");
            return "me-security";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?passwordUpdated=true";
    }
}
