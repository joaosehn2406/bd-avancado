package com.jps.jps.tracking;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String trackingCode) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(trackingCode, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(trackingCode, emitter));
        emitter.onTimeout(() -> remove(trackingCode, emitter));
        emitter.onError(e -> remove(trackingCode, emitter));
        return emitter;
    }

    public void publish(String trackingCode, Object payload) {
        List<SseEmitter> list = emitters.get(trackingCode);
        if (list == null || list.isEmpty()) return;
        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(payload));
                return false;
            } catch (IOException e) {
                emitter.complete();
                return true;
            }
        });
    }

    private void remove(String trackingCode, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(trackingCode);
        if (list != null) list.remove(emitter);
    }
}
