package com.example.application.data.service;

import com.example.application.data.entity.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.UUID;

public class DataService {

    private final PersonRepository repository;

    @Autowired
    public DataService(PersonRepository repository) {
        this.repository = repository;
    }

    public static List<Person> getPeople() {
        List<Person> personen = new ArrayList<Person>();

        Person p = new Person();
        p.setEmail("DE.Justiztest.ajehrfhefr-23sef-2423.2344");

        p.setFirstName("Landgericht Hamburg_Test_2");
        p.setLastName("K1100");
        p.setIsActive(false);
        personen.add(p);

        Person p2 = new Person();
        p2.setEmail("DE.Justiztest.ehwefrfhefr-23sef-2323.2333");

        p2.setFirstName("Amtsgericht Hamburh_ZENVG_Test2");
        p2.setLastName("K1010V");
        p2.setIsActive(true);

        personen.add(p2);

        Person p3 = new Person();
        p3.setEmail("DE.Justiztest.ehgggrfhefr-23sef-2423.2323");

        p3.setFirstName("Grundbuchamt Hamburg Test_2");
        p3.setLastName("K1101G");
        p3.setIsActive(true);

        personen.add(p3);


        return personen;
    }

    public Optional<Person> get(UUID id) {
        return repository.findById(id);
    }

    public Person update(Person entity) {
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public Page<Person> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}
