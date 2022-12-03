package com.example.application.data.service;

import com.example.application.data.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

    public interface PersonRepository extends JpaRepository<Person, UUID> {
}
