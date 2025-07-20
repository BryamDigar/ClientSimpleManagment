package com.rti.prueba.mapper;

import com.rti.prueba.bd.orm.ClienteORM;
import com.rti.prueba.controller.dto.ClienteCreateDTO;
import com.rti.prueba.controller.dto.ClienteResponseDTO;
import com.rti.prueba.controller.dto.ClienteUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    
    @Mapping(target = "nombre", expression = "java(dto.getNombre().trim())")
    @Mapping(target = "apellidos", expression = "java(dto.getApellidos().trim())")
    @Mapping(target = "ciudad", expression = "java(dto.getCiudad().trim())")
    @Mapping(target = "correoElectronico", expression = "java(dto.getCorreoElectronico().toLowerCase().trim())")
    @Mapping(target = "telefono", expression = "java(dto.getTelefono().trim())")
    @Mapping(target = "esViable", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClienteORM createDTOToORM(ClienteCreateDTO dto);
    
    @Mapping(target = "nombre", expression = "java(dto.getNombre().trim())")
    @Mapping(target = "apellidos", expression = "java(dto.getApellidos().trim())")
    @Mapping(target = "ciudad", expression = "java(dto.getCiudad().trim())")
    @Mapping(target = "correoElectronico", expression = "java(dto.getCorreoElectronico().toLowerCase().trim())")
    @Mapping(target = "telefono", expression = "java(dto.getTelefono().trim())")
    @Mapping(target = "numeroDocumento", ignore = true)
    @Mapping(target = "esViable", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateDTOToORM(ClienteUpdateDTO dto, @MappingTarget ClienteORM cliente);

    @Mapping(target = "edad", ignore = true)
    ClienteResponseDTO ORMToResponseDTO(ClienteORM entity);

}
