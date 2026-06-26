package com.Csports.Csports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Csports.Csports.model.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {
        Optional<Sport> findByName(String name);

}
