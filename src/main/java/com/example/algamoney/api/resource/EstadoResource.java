package com.example.algamoney.api.resource;

import com.example.algamoney.api.model.Estado;
import com.example.algamoney.api.repository.EstadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/estados")
public class EstadoResource {

	private static final Logger log = LoggerFactory.getLogger(EstadoResource.class);
	
	@Autowired
	private EstadoRepository estadoRepository;
	
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public List<Estado> listar() {
		log.info("Listando todos os estados");
		List<Estado> estados = estadoRepository.findAll();
		log.info("Total de estados encontrados: {}", estados.size());
		return estados;
	}
}
