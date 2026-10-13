package com.taller.portal.controller;

import com.taller.portal.dto.VehiculoDtos.*;
import com.taller.portal.service.VehiculoFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** API REST del registro de vehículos y del catálogo vPIC local autenticado. */
@RestController @RequestMapping("/api/vehiculos") public class VehiculoController {
 private final VehiculoFacade facade;
 /** @param facade caso de uso de vehículos. */ public VehiculoController(VehiculoFacade facade){this.facade=facade;}
 /** @return marcas locales por prefijo. */ @GetMapping("/catalogo/marcas") public List<Marca> marcas(@RequestParam(defaultValue="") String prefijo,Authentication a){return facade.marcas(prefijo,a);}
 /** @return modelos locales filtrados por marca y prefijo. */ @GetMapping("/catalogo/modelos") public List<Modelo> modelos(@RequestParam Long marcaId,@RequestParam(defaultValue="") String prefijo,Authentication a){return facade.modelos(marcaId,prefijo,a);}
 /** @return clientes activos del taller de la identidad autenticada. */ @GetMapping("/clientes") public List<ClienteOpcion> clientes(@RequestParam(defaultValue="") String busqueda,Authentication a){return facade.clientes(busqueda,a);}
 /** @return alta creada en estado ACT. */ @PostMapping @ResponseStatus(HttpStatus.CREATED) public Respuesta registrar(@Valid @RequestBody Registro d,Authentication a){return facade.registrar(d,a);}
}
