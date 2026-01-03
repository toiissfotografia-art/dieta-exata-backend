package com.dietaexata.repository;

import com.dietaexata.model.Refeicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefeicaoRepository extends JpaRepository<Refeicao, Long> {
    // Ao estender JpaRepository, o findAll() e o save() aparecem magicamente.
}