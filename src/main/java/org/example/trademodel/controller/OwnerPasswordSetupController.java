package org.example.trademodel.controller;

import org.example.trademodel.service.MultiUserAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OwnerPasswordSetupController {
    private final MultiUserAccountService accountService;

    public OwnerPasswordSetupController(MultiUserAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/owner/password-setup")
    public String form(@RequestParam String token, Model model) {
        model.addAttribute("setupToken", token);
        return "owner-password-setup";
    }

    @PostMapping("/owner/password-setup")
    public String complete(@RequestParam String token,
                           @RequestParam String password,
                           @RequestParam String passwordConfirmation,
                           Model model) {
        try {
            accountService.completeOwnerPasswordSetup(token, password, passwordConfirmation);
            return "redirect:/login?passwordUpdated=true";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("setupToken", token);
            model.addAttribute("setupError", "链接无效或已过期，或密码不符合 8 至 128 位要求。");
            return "owner-password-setup";
        }
    }
}
