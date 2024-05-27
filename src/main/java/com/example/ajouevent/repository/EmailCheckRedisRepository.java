package com.example.ajouevent.repository;

import com.example.ajouevent.domain.EmailCheck;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

@Repository
public interface EmailCheckRedisRepository extends CrudRepository<EmailCheck, String> {
    EmailCheck findByEmail(String email);
}
