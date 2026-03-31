package net.minecraft.server.jsonrpc.security;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Sharable
public class AuthenticationHandler extends ChannelDuplexHandler {
    private final Logger LOGGER = LogUtils.getLogger();
    private static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf("authenticated");
    private static final AttributeKey<Boolean> ATTR_WEBSOCKET_ALLOWED = AttributeKey.valueOf("websocket_auth_allowed");
    private static final String SUBPROTOCOL_VALUE = "minecraft-v1";
    private static final String SUBPROTOCOL_HEADER_PREFIX = "minecraft-v1,";
    public static final String BEARER_PREFIX = "Bearer ";
    private final SecurityConfig securityConfig;
    private final Set<String> allowedOrigins;

    public AuthenticationHandler(SecurityConfig securityConfig, String allowedOrigins) {
        this.securityConfig = securityConfig;
        this.allowedOrigins = Sets.newHashSet(allowedOrigins.split(","));
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object request) throws Exception {
        String s = this.getClientIp(context);
        if (request instanceof HttpRequest httprequest) {
            AuthenticationHandler.SecurityCheckResult authenticationhandler$securitycheckresult = this.performSecurityChecks(httprequest);
            if (!authenticationhandler$securitycheckresult.isAllowed()) {
                this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", s, authenticationhandler$securitycheckresult.getReason());
                context.channel().attr(AUTHENTICATED_KEY).set(false);
                this.sendUnauthorizedResponse(context, authenticationhandler$securitycheckresult.getReason());
                return;
            }

            context.channel().attr(AUTHENTICATED_KEY).set(true);
            if (authenticationhandler$securitycheckresult.isTokenSentInSecWebsocketProtocol()) {
                context.channel().attr(ATTR_WEBSOCKET_ALLOWED).set(Boolean.TRUE);
            }
        }

        Boolean obool = context.channel().attr(AUTHENTICATED_KEY).get();
        if (Boolean.TRUE.equals(obool)) {
            super.channelRead(context, request);
        } else {
            this.LOGGER.debug("Dropping unauthenticated connection with ip {}", s);
            context.close();
        }
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof HttpResponse httpresponse
            && httpresponse.status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()
            && context.channel().attr(ATTR_WEBSOCKET_ALLOWED).get() != null
            && context.channel().attr(ATTR_WEBSOCKET_ALLOWED).get().equals(Boolean.TRUE)) {
            httpresponse.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, "minecraft-v1");
        }

        super.write(context, message, promise);
    }

    private AuthenticationHandler.SecurityCheckResult performSecurityChecks(HttpRequest request) {
        String s = this.parseTokenInAuthorizationHeader(request);
        if (s != null) {
            return this.isValidApiKey(s)
                ? AuthenticationHandler.SecurityCheckResult.allowed()
                : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
        } else {
            String s1 = this.parseTokenInSecWebsocketProtocolHeader(request);
            if (s1 != null) {
                if (!this.isAllowedOriginHeader(request)) {
                    return AuthenticationHandler.SecurityCheckResult.denied("Origin Not Allowed");
                } else {
                    return this.isValidApiKey(s1)
                        ? AuthenticationHandler.SecurityCheckResult.allowed(true)
                        : AuthenticationHandler.SecurityCheckResult.denied("Invalid API key");
                }
            } else {
                return AuthenticationHandler.SecurityCheckResult.denied("Missing API key");
            }
        }
    }

    private boolean isAllowedOriginHeader(HttpRequest request) {
        String s = request.headers().get(HttpHeaderNames.ORIGIN);
        return s != null && !s.isEmpty() ? this.allowedOrigins.contains(s) : false;
    }

    private @Nullable String parseTokenInAuthorizationHeader(HttpRequest request) {
        String s = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        return s != null && s.startsWith("Bearer ") ? s.substring("Bearer ".length()).trim() : null;
    }

    private @Nullable String parseTokenInSecWebsocketProtocolHeader(HttpRequest request) {
        String s = request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
        return s != null && s.startsWith("minecraft-v1,") ? s.substring("minecraft-v1,".length()).trim() : null;
    }

    public boolean isValidApiKey(String apiKey) {
        if (apiKey.isEmpty()) {
            return false;
        } else {
            byte[] abyte = apiKey.getBytes(StandardCharsets.UTF_8);
            byte[] abyte1 = this.securityConfig.secretKey().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(abyte, abyte1);
        }
    }

    private String getClientIp(ChannelHandlerContext context) {
        InetSocketAddress inetsocketaddress = (InetSocketAddress)context.channel().remoteAddress();
        return inetsocketaddress.getAddress().getHostAddress();
    }

    private void sendUnauthorizedResponse(ChannelHandlerContext context, String message) {
        String s = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        byte[] abyte = s.getBytes(StandardCharsets.UTF_8);
        DefaultFullHttpResponse defaultfullhttpresponse = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer(abyte)
        );
        defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        defaultfullhttpresponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, abyte.length);
        defaultfullhttpresponse.headers().set(HttpHeaderNames.CONNECTION, "close");
        context.writeAndFlush(defaultfullhttpresponse).addListener(p_449756_ -> context.close());
    }

    static class SecurityCheckResult {
        private final boolean allowed;
        private final String reason;
        private final boolean tokenSentInSecWebsocketProtocol;

        private SecurityCheckResult(boolean allowed, String reason, boolean tokenSentInSecWebsocketProtocol) {
            this.allowed = allowed;
            this.reason = reason;
            this.tokenSentInSecWebsocketProtocol = tokenSentInSecWebsocketProtocol;
        }

        public static AuthenticationHandler.SecurityCheckResult allowed() {
            return new AuthenticationHandler.SecurityCheckResult(true, null, false);
        }

        public static AuthenticationHandler.SecurityCheckResult allowed(boolean tokenSentInSecWebsocketProtocol) {
            return new AuthenticationHandler.SecurityCheckResult(true, null, tokenSentInSecWebsocketProtocol);
        }

        public static AuthenticationHandler.SecurityCheckResult denied(String reason) {
            return new AuthenticationHandler.SecurityCheckResult(false, reason, false);
        }

        public boolean isAllowed() {
            return this.allowed;
        }

        public String getReason() {
            return this.reason;
        }

        public boolean isTokenSentInSecWebsocketProtocol() {
            return this.tokenSentInSecWebsocketProtocol;
        }
    }
}
