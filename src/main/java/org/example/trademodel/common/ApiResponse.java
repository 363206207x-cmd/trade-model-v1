package org.example.trademodel.common;
import java.time.LocalDateTime;

public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;
    private String requestId;
    private LocalDateTime serverTime;

    public static <T> ApiResponse<T> success(T data) {
        return success("success", data);
    }

    /** HTTP 200 with a caller-visible message (e.g. deprecation / non-authoritative notice). */
    public static <T> ApiResponse<T> success(String msg, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 200;
        resp.msg = msg;
        resp.data = data;
        resp.requestId = "req-" + System.currentTimeMillis();
        resp.serverTime = LocalDateTime.now();
        return resp;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 500;
        resp.msg = msg;
        resp.requestId = "req-" + System.currentTimeMillis();
        resp.serverTime = LocalDateTime.now();
        return resp;
    }

    /** Read-only not found (e.g. unknown analysisId). Pair with HTTP 404 on the response entity. */
    public static <T> ApiResponse<T> notFound(String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 404;
        resp.msg = msg;
        resp.data = null;
        resp.requestId = "req-" + System.currentTimeMillis();
        resp.serverTime = LocalDateTime.now();
        return resp;
    }

    /** Client error (HTTP 400). Pair with ResponseEntity.badRequest when appropriate. */
    public static <T> ApiResponse<T> badRequest(String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 400;
        resp.msg = msg;
        resp.data = null;
        resp.requestId = "req-" + System.currentTimeMillis();
        resp.serverTime = LocalDateTime.now();
        return resp;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getServerTime() { return serverTime; }
    public void setServerTime(LocalDateTime serverTime) { this.serverTime = serverTime; }
}
