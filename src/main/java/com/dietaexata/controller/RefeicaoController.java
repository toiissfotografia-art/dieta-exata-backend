package com.dietaexata.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dietaexata.model.Refeicao;
import com.dietaexata.repository.RefeicaoRepository;

@RestController
@RequestMapping("/api/refeicoes")
// Incluído CrossOrigin para permitir que seu site (frontend) acesse o backend no Render
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RefeicaoController {

    @Autowired
    private RefeicaoRepository repository;

    @GetMapping
    public List<Refeicao> listarTodas() {
        return repository.findAll();
    }

    @PostMapping
    public Refeicao salvar(@RequestBody Refeicao refeicao) {
        // Garantimos que a refeição seja salva e enviada imediatamente para o banco
        return repository.save(refeicao);
    }

    // Dentro do RefeicaoController.java, substitua o método listarPorUsuario por este:
    @GetMapping("/usuario/{usuarioEmail}")
     public List<Refeicao> listarPorUsuario(@PathVariable String usuarioEmail) {
     // Agora o repository faz todo o trabalho duro de uma vez só
     return repository.findByUsuarioEmail(usuarioEmail);
  }  

}