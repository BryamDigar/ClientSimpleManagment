package com.rti.prueba.bd.jpa;

import com.rti.prueba.bd.orm.ClienteORM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteJPA extends JpaRepository<ClienteORM, String> {
    
    Optional<ClienteORM> findByCorreoElectronico(String correoElectronico);
    
    List<ClienteORM> findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(String nombre, String apellidos);
}
