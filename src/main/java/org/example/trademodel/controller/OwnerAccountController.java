package org.example.trademodel.controller;

import java.util.List;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.MultiUserAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner")
public class OwnerAccountController {
    private final AuthenticatedUserIdResolver userResolver;
    private final MultiUserAccountService accountService;

    public OwnerAccountController(AuthenticatedUserIdResolver userResolver,
                                  MultiUserAccountService accountService) {
        this.userResolver = userResolver;
        this.accountService = accountService;
    }

    @GetMapping("/accounts")
    public ApiResponse<List<MultiUserAccountService.AccountView>> accounts() {
        userResolver.requireOwner();
        return ApiResponse.success(accountService.listAccounts());
    }

    @PostMapping("/accounts/{userId}/disable")
    public ApiResponse<Void> disable(@PathVariable Long userId) {
        userResolver.requireOwner();
        accountService.disableUser(userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/accounts/{userId}/enable")
    public ApiResponse<Void> enable(@PathVariable Long userId) {
        userResolver.requireOwner();
        accountService.enableUser(userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/accounts/{userId}/force-logout")
    public ApiResponse<Void> forceLogout(@PathVariable Long userId) {
        userResolver.requireOwner();
        accountService.forceLogout(userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/password-setup-link")
    public ApiResponse<PasswordSetupLink> passwordSetupLink() {
        PersonalUserDO owner = userResolver.requireOwner();
        return ApiResponse.success(new PasswordSetupLink(
                accountService.issueOwnerPasswordSetupLink(owner.getId()), 15));
    }

    public record PasswordSetupLink(String path, int expiresInMinutes) {
    }
}
