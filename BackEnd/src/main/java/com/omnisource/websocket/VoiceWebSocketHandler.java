package com.omnisource.websocket;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler extends BinaryWebSocketHandler {

    private final ObjectMapper objectMapper;

    @Value("${voice.dashscope.api-key:}")
    private String apiKey;

    @Value("${voice.dashscope.model:fun-asr-realtime}")
    private String model;

    @Value("${voice.dashscope.mandarin-model:fun-asr-realtime-2025-09-15}")
    private String mandarinModel;

    @Value("${voice.dashscope.minnan-model:fun-asr-realtime-2025-11-07}")
    private String minnanModel;

    @Value("${voice.dashscope.sample-rate:16000}")
    private int sampleRate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!StringUtils.hasText(apiKey)) {
            sendJson(session, "error", Map.of("message", "Missing DASHSCOPE_API_KEY or QIANWEN_API_KEY"));
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        Recognition recognition = new Recognition();
        String dialect = resolveDialect(session);
        String selectedModel = resolveModel(dialect);
        RecognitionParam param = RecognitionParam.builder()
                .apiKey(apiKey)
                .model(selectedModel)
                .format("pcm")
                .sampleRate(sampleRate)
                .parameter("language_hints", new String[]{"zh"})
                .build();
        ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
            @Override
            public void onEvent(RecognitionResult result) {
                try {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    String text = result.getSentence() == null ? "" : result.getSentence().getText();
                    payload.put("text", text);
                    payload.put("sentenceEnd", result.isSentenceEnd());
                    payload.put("requestId", result.getRequestId());
                    sendJson(session, "transcript", payload);
                } catch (Exception e) {
                    log.warn("Failed to send recognition result: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                try {
                    sendJson(session, "closed", Map.of("message", "recognition complete"));
                } catch (Exception e) {
                    log.warn("Failed to send recognition completion: {}", e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                try {
                    sendJson(session, "error", Map.of("message", e.getMessage()));
                } catch (Exception ex) {
                    log.warn("Failed to send recognition error: {}", ex.getMessage());
                }
            }
        };

        session.getAttributes().put("recognition", recognition);
        session.getAttributes().put("param", param);
        recognition.call(param, callback);
        sendJson(session, "ready", Map.of(
                "model", selectedModel,
                "dialect", dialect,
                "sampleRate", sampleRate,
                "time", LocalDateTime.now().toString()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);
            Object typeValue = payload.get("type");
            String type = typeValue == null ? "" : typeValue.toString();
            if ("stop".equals(type)) {
                closeRecognition(session);
                sendJson(session, "closed", Map.of("message", "recognition stopped"));
            }
        } catch (Exception e) {
            log.warn("Failed to handle voice text message: {}", e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Recognition recognition = (Recognition) session.getAttributes().get("recognition");
        RecognitionParam param = (RecognitionParam) session.getAttributes().get("param");
        if (recognition == null || param == null) {
            sendJson(session, "error", Map.of("message", "recognition is not initialized"));
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        if (bytes.length == 0) {
            return;
        }

        try {
            recognition.sendAudioFrame(ByteBuffer.wrap(bytes));
        } catch (Exception e) {
            log.error("Fun-ASR realtime recognition failed", e);
            sendJson(session, "error", Map.of("message", e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        closeRecognition(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Voice WebSocket transport error", exception);
        closeRecognition(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void closeRecognition(WebSocketSession session) {
        Object recognition = session.getAttributes().remove("recognition");
        if (recognition instanceof Recognition recognizer) {
            try {
                recognizer.stop();
                if (recognizer.getDuplexApi() != null) {
                    recognizer.getDuplexApi().close(1000, "bye");
                }
            } catch (Exception e) {
                log.warn("Failed to close recognition client: {}", e.getMessage());
            }
        }
    }

    private String resolveDialect(WebSocketSession session) {
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (!StringUtils.hasText(query)) {
            return "mandarin";
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "dialect".equals(pair[0])) {
                String value = pair[1].trim().toLowerCase();
                if (Set.of("minnan", "hokkien", "闽南语").contains(value)) {
                    return "minnan";
                }
                return "mandarin";
            }
        }
        return "mandarin";
    }

    private String resolveModel(String dialect) {
        if ("minnan".equals(dialect) && StringUtils.hasText(minnanModel)) {
            return minnanModel;
        }
        if (StringUtils.hasText(mandarinModel)) {
            return mandarinModel;
        }
        return model;
    }

    private void sendJson(WebSocketSession session, String type, Map<String, ?> data) throws Exception {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", type,
                        "data", data
                ))));
            }
        }
    }
}
