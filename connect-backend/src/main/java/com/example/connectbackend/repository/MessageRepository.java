package com.example.connectbackend.repository;

import com.example.connectbackend.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByIsGlobalOrderByTimestampDesc(boolean isGlobal);
    List<Message> findBySenderIdAndReceiverIdOrderByTimestampDesc(String senderId, String receiverId);
}
