package com.example.connectbackend.repository;

import com.example.connectbackend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // Basic CRUD operations are automatically implemented by Spring Data MongoDB
    User findByEmail(String email);

    @Query(value = "{ }", fields = "{ 'username' : 1, '_id' : 0}")
    List<String> findAllUsernames();
}
