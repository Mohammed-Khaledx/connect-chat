
package com.example.connectbackend.service;

import com.example.connectbackend.model.Message;
import com.example.connectbackend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;

    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    public List<Message> getGlobalMessages() {
        return messageRepository.findByIsGlobalOrderByTimestamp(true);
    }

    public List<Message> getPrivateMessages(String senderId, String receiverId) {
        return messageRepository.findBySenderIdAndReceiverIdOrderByTimestampDesc(senderId, receiverId);
    }

//    Message saveMessage(Message message);
//    Page<Message> getGlobalMessages(PageRequest pageRequest);
//    Page<Message> getGlobalMessagesBefore(LocalDateTime before, PageRequest pageRequest);
//    Page<Message> getPrivateMessages(String senderId, String receiverId, PageRequest pageRequest);
//    Page<Message> getPrivateMessagesBefore(String senderId, String receiverId, LocalDateTime before, PageRequest pageRequest);
//    Page<Message> getUserMessages(String userId, PageRequest pageRequest);
}