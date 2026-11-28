package com.bogotickets.service;

import com.bogotickets.dto.Ubicacion;
import com.bogotickets.repository.UbicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UbicacionService {

    private final UbicacionRepository repositorio;

    public UbicacionService(UbicacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Ubicacion> listar() {
        return repositorio.listar();
    }
}
