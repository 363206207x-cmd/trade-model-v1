package org.example.trademodel.controller;

import java.util.Objects;

import org.example.trademodel.service.MultiUserAccountService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistrationController {
    private final MultiUserAccountService accountService;

    public RegistrationController(MultiUserAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/register")
    public String form(Authentication authentication, Model model) {
        if (authenticated(authentication)) return "redirect:/dashboard";
        addAvailability(model);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String passwordConfirmation,
                           Model model) {
        addAvailability(model);
        if (!Objects.equals(password, passwordConfirmation)) {
            model.addAttribute("registrationError", "两次输入的密码不一致。");
            model.addAttribute("submittedUsername", username == null ? "" : username.trim());
            return "register";
        }
        try {
            accountService.register(username, password);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("registrationError", safeMessage(exception));
            model.addAttribute("submittedUsername", username == null ? "" : username.trim());
            addAvailability(model);
            return "register";
        }
    }

    private void addAvailability(Model model) {
        MultiUserAccountService.RegistrationAvailability availability =
                accountService.registrationAvailability();
        model.addAttribute("registrationOpen", availability.open());
        model.addAttribute("enabledAccounts", availability.enabledAccounts());
        model.addAttribute("maximumAccounts", availability.maximumAccounts());
    }

    private static boolean authenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("disabled")) return "注册暂未开放";
        if (message != null && message.contains("capacity")) return "注册人数已满";
        if (message != null && message.contains("already")) return "用户名已存在";
        if (message != null && message.contains("username format")) return "用户名格式不正确";
        if (message != null && message.contains("password policy rejected")) {
            return "密码必须正好8个字符，不能使用默认密码，也不能与用户名相同。";
        }
        return "注册失败，请重新尝试";
    }
}
