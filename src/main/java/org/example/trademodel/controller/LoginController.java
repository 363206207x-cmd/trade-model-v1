package org.example.trademodel.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.example.trademodel.service.MultiUserAccountService;

@Controller
public class LoginController {
    private final MultiUserAccountService accountService;

    public LoginController(MultiUserAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String expired,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String passwordUpdated,
                        Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("sessionExpired", expired != null);
        model.addAttribute("registered", registered != null);
        model.addAttribute("passwordUpdated", passwordUpdated != null);
        model.addAttribute("registrationOpen", accountService.registrationAvailability().open());
        return "login";
    }
}
