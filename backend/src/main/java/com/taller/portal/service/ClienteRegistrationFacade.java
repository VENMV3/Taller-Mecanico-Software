package com.taller.portal.service;
import com.taller.portal.dto.ClienteDtos.*; import com.taller.portal.model.Cliente; import com.taller.portal.model.RoleName; import com.taller.portal.repository.ClienteRepository; import org.springframework.security.access.AccessDeniedException; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.multipart.MultipartFile; import javax.imageio.ImageIO; import java.awt.image.BufferedImage; import java.io.*; import java.nio.file.*; import java.time.*; import java.util.*;
/** Fachada única del registro: autoriza, valida, evita duplicados, guarda foto y persiste. */
@Service public class ClienteRegistrationFacade { private final ClienteRepository repo; private final Path uploads=Paths.get("uploads","clientes"); /** @param repo repositorio de clientes. */ public ClienteRegistrationFacade(ClienteRepository repo){this.repo=repo;} /** Registra un cliente de forma idempotente. @param datos datos validados por Bean Validation @param foto imagen de cliente @param key clave de idempotencia @param auth identidad autenticada @return cliente registrado o el resultado previo @throws IOException si no se puede procesar la foto. */ @Transactional public Respuesta registrar(Registro datos,MultipartFile foto,String key,Authentication auth)throws IOException{autorizar(auth);if(key==null||key.isBlank())throw new IllegalArgumentException("Idempotency-Key es obligatorio");var previo=repo.findByIdempotencyKey(key);if(previo.isPresent())return new Respuesta(previo.get().id,"Cliente Registrado");validar(datos,foto);if(repo.existsByEmailOrTelefonoPersonalOrTelefonoTrabajoOrNombreCompletoAndFechaNacimiento(datos.email().trim().toLowerCase(),datos.telefonoPersonal().trim(),datos.telefonoTrabajo().trim(),datos.nombreCompleto().trim(),datos.fechaNacimiento()))throw new IllegalArgumentException("Ya existe un cliente con correo, teléfono o nombre y fecha de nacimiento coincidentes");String ruta=guardarFoto(foto);Cliente c=new Cliente();c.nombreCompleto=datos.nombreCompleto().trim();c.contactoAlternativo=datos.contactoAlternativo().trim();c.edad=datos.edad();c.fechaNacimiento=datos.fechaNacimiento();c.telefonoPersonal=datos.telefonoPersonal().trim();c.telefonoTrabajo=datos.telefonoTrabajo().trim();c.email=datos.email().trim().toLowerCase();c.emailTrabajo=datos.emailTrabajo()==null||datos.emailTrabajo().isBlank()?null:datos.emailTrabajo().trim().toLowerCase();c.fotografiaRuta=ruta;c.calle=datos.direccion().calle().trim();c.colonia=datos.direccion().colonia().trim();c.municipio=datos.direccion().municipio().trim();c.estado=datos.direccion().estado().trim();c.codigoPostal=datos.direccion().codigoPostal();c.idempotencyKey=key;repo.save(c);return new Respuesta(c.id,"Cliente Registrado");} /** Comprueba los dos roles permitidos. @param auth autenticación actual @throws AccessDeniedException si el rol no es permitido. */ private void autorizar(Authentication auth){if(auth==null||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_"+RoleName.ADMINISTRADOR_SISTEMA)||a.getAuthority().equals("ROLE_"+RoleName.RECEPCIONISTA)))throw new AccessDeniedException("No tiene autorización para registrar clientes");} /** Valida coherencia de edad y bytes de imagen. @param d datos del formulario @param foto archivo adjunto @throws IOException si ImageIO no puede leerlo. */ private void validar(Registro d,MultipartFile foto)throws IOException{int edad=Period.between(d.fechaNacimiento(),LocalDate.now()).getYears();if(edad!=d.edad())throw new IllegalArgumentException("La edad no coincide con la fecha de nacimiento");if(foto==null||foto.isEmpty()||foto.getSize()>15L*1024*1024||foto.getContentType()==null||!foto.getContentType().startsWith("image/"))throw new IllegalArgumentException("La fotografía debe ser una imagen válida de máximo 15 MB");try(InputStream in=foto.getInputStream()){BufferedImage image=ImageIO.read(in);if(image==null)throw new IllegalArgumentException("El contenido de la fotografía no es una imagen válida");}} /** Guarda bytes validados sin usar el nombre enviado por el cliente. @param foto imagen validada @return ruta relativa persistida @throws IOException si el disco no permite escritura. */ private String guardarFoto(MultipartFile foto)throws IOException{Files.createDirectories(uploads);String extension=Optional.ofNullable(foto.getContentType()).filter(x->x.equals("image/png")).isPresent()?".png":".jpg";String name=UUID.randomUUID()+extension;Files.copy(foto.getInputStream(),uploads.resolve(name),StandardCopyOption.REPLACE_EXISTING);return uploads.resolve(name).toString();}
    /** Consulta clientes autorizados.
     * @param pagina índice base cero @param tamanio tamaño de 1 a 50 @param busqueda texto de hasta 120 caracteres
     * @param auth sesión actual @return página preparada
     * @throws IllegalArgumentException por parámetros inválidos
     * @throws AccessDeniedException por sesión o rol no permitido. */
    @Transactional(readOnly=true)
    public com.taller.portal.dto.ClienteConsultaDtos.Pagina consultar(int pagina, int tamanio, String busqueda, Authentication auth) {
        autorizarConsulta(auth);
        if (pagina < 0 || pagina > 100000 || tamanio < 1 || tamanio > 50)
            throw new IllegalArgumentException("Página inválida o tamaño fuera de 1 a 50");
        String texto = busqueda == null ? "" : busqueda.strip();
        if (texto.length() > 120 || texto.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Búsqueda inválida: máximo 120 caracteres");
        String patron = "%" + texto.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var resultado = repo.consultar(patron, org.springframework.data.domain.PageRequest.of(
            pagina, tamanio, org.springframework.data.domain.Sort.by("id").descending()));
        return new com.taller.portal.dto.ClienteConsultaDtos.Pagina(resultado.getContent(), pagina, tamanio,
            resultado.getTotalElements(), resultado.getTotalPages());
    }

    /** @param id identificador positivo @param auth sesión
     * @return datos de lectura @throws IllegalArgumentException por ID inválido
     * @throws org.springframework.web.server.ResponseStatusException si no existe; 404.
     * @throws AccessDeniedException si no está autorizado. */
    @Transactional(readOnly=true)
    public com.taller.portal.dto.ClienteConsultaDtos.Detalle detalle(Long id, Authentication auth) {
        autorizarConsulta(auth);
        validarId(id);
        return repo.consultarDetalle(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    /** Lee y recodifica la imagen para evitar entregar contenido activo o rutas públicas.
     * @param id cliente @param auth sesión @return PNG decodificado
     * @throws IOException por error de lectura @throws AccessDeniedException por rol
     * @throws org.springframework.web.server.ResponseStatusException si falta/no es segura. */
    @Transactional(readOnly=true)
    public com.taller.portal.dto.ClienteConsultaDtos.Foto fotografia(Long id, Authentication auth) throws IOException {
        autorizarConsulta(auth);
        validarId(id);
        String ruta = repo.consultarRutaFoto(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Fotografía no disponible"));
        Path root = uploads.toAbsolutePath().normalize();
        Path file = Paths.get(ruta).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file) || !file.startsWith(root) || !file.toRealPath().startsWith(root.toRealPath())
            || Files.size(file) > 15L * 1024 * 1024)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Fotografía no disponible");
        try (var stream = ImageIO.createImageInputStream(file.toFile())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            var reader = readers.next();
            try {
                reader.setInput(stream);
                if ((long)reader.getWidth(0) * reader.getHeight(0) > 20000000)
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
                var bytes = new ByteArrayOutputStream();
                ImageIO.write(reader.read(0), "png", bytes);
                return new com.taller.portal.dto.ClienteConsultaDtos.Foto(bytes.toByteArray(), "image/png");
            } finally { reader.dispose(); }
        }
    }

    /** @param auth sesión @throws AccessDeniedException si no existe o no tiene rol permitido. */
    private void autorizarConsulta(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Acceso denegado");
        autorizar(auth);
    }
    /** @param id identificador @throws IllegalArgumentException si no es positivo. */
    private void validarId(Long id) { if (id == null || id <= 0) throw new IllegalArgumentException("ID inválido"); }
}
