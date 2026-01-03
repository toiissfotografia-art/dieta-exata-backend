package com.dietaexata.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dietaexata.model.Refeicao;
import com.dietaexata.repository.RefeicaoRepository;

@RestController
@RequestMapping("/api/refeicoes")
public class RefeicaoController {

    @Autowired
    private RefeicaoRepository repository;

    @GetMapping
    public List<Refeicao> listarTodas() {
        return repository.findAll();
    }

    @PostMapping
    public Refeicao salvar(@RequestBody Refeicao refeicao) {
        return repository.save(refeicao);
    }
}