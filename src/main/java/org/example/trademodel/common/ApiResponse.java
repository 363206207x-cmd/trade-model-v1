package org.example.trademodel.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.trademodel.requestcontext.RequestIdSupport;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;
    private String requestId;
    private OffsetDateTime serverTime;

    public static <T> ApiResponse<T> success(T data) {
        return success("success", data);
    }

    /** HTTP 200 with a caller-visible message (e.g. deprecation / non-authoritative notice). */
    public static <T> ApiResponse<T> success(String msg, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 200;
        resp.msg = msg;
        resp.data = data;
        stamp(resp);
        return resp;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 500;
        resp.msg = msg;
        stamp(resp);
        return resp;
    }

    /** Read-only not found (e.g. unknown analysisId). Pair with HTTP 404 on the response entity. */
    public static <T> ApiResponse<T> notFound(String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = 404;
        resp.msg = msg;
        resp.data = null;
        stamp(resp);
        return resp;
    }

    /** Client error (HTTP 400). Pair with ResponseEntity.badRequest when appropriate. */
    public static <T> ApiResponse<T> badRequest(String msg) {
        return error(400, msg);
    }

    public static <T> ApiResponse<T> unauthorized(String msg) {
        return error(401, msg);
    }

    public static <T> ApiResponse<T> forbidden(String msg) {
        return error(403, msg);
    }

    public static <T> ApiResponse<T> conflict(String msg) {
        return error(409, msg);
    }

    private static <T> ApiResponse<T> error(int code, String msg) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = code;
        resp.msg = msg;
        resp.data = null;
        stamp(resp);
        return resp;
    }

    private static void stamp(ApiResponse<?> response) {
        response.requestId = RequestIdSupport.currentOrNew();
        response.serverTime = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    @JsonProperty("request_id")
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    @JsonProperty("server_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public OffsetDateTime getServerTime() { return serverTime; }
    public void setServerTime(OffsetDateTime serverTime) { this.serverTime = serverTime; }
}
