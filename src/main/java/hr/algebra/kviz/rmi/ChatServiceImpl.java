package hr.algebra.kviz.rmi;

import hr.algebra.kviz.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServiceImpl implements ChatService {

    private final Map<String, List<ChatMessage>> rooms = new ConcurrentHashMap<>();

    @Override
    public void sendMessage(String gameId, ChatMessage message) {
        rooms.computeIfAbsent(gameId, id -> new CopyOnWriteArrayList<>()).add(message);
    }

    @Override
    public List<ChatMessage> getMessages(String gameId, int fromIndex) {
        List<ChatMessage> room = rooms.getOrDefault(gameId, List.of());
        if (fromIndex >= room.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(room.subList(Math.max(0, fromIndex), room.size()));
    }
}
