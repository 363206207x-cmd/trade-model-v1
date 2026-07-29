package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.MessagePushReadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessagePushReadController {
    private final MessagePushReadService messagePushReadService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public MessagePushReadController(MessagePushReadService messagePushReadService,
                                     AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.messagePushReadService = messagePushReadService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MessageListDTO>> list(
            @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        MessageListDTO result = messagePushReadService.listForUser(userId, limit);
        return response(result.state(), result);
    }

    @GetMapping("/{messageId}/push-detail")
    public ResponseEntity<ApiResponse<PushDetailDTO>> pushDetail(
            @PathVariable String messageId) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        PushDetailDTO result = messagePushReadService.findPushDetailForUser(userId, messageId);
        return response(result.state(), result);
    }

    private static <T> ResponseEntity<ApiResponse<T>> response(MessageReadState state, T data) {
        HttpStatus httpStatus = switch (state) {
            case ERROR -> HttpStatus.SERVICE_UNAVAILABLE;
            case MISSING -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.OK;
        };
        ApiResponse<T> body = ApiResponse.success(data);
        body.setCode(httpStatus.value());
        body.setMsg(switch (state) {
            case ERROR -> "message data unavailable";
            case MISSING -> "message not found";
            case EMPTY -> "message list empty";
            case PARTIAL -> "message data partial";
            case READY -> "success";
        });
        return ResponseEntity.status(httpStatus).body(body);
    }
}
