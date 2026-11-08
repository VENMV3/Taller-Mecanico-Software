package com.taller.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Evita sintaxis que H2 admite pero MySQL 8 rechaza en migraciones Flyway. */
class MigracionesCompatiblesMySqlTest {
 private static final Pattern INCOMPATIBLE=Pattern.compile("(?is)(ADD\\s+COLUMN|ADD\\s+INDEX|CREATE\\s+INDEX|ADD\\s+CONSTRAINT)\\s+IF\\s+NOT\\s+EXISTS");
 /** Cada migración debe ser aceptable tanto por MySQL 8 como por H2. */
 @Test void migracionesNoUsanIfNotExistsNoCompatibleConMySql8() throws IOException {
  try(var archivos=Files.list(Path.of("src/main/resources/db/migration"))){
   for(Path archivo:archivos.filter(p->p.toString().endsWith(".sql")).toList()) assertFalse(INCOMPATIBLE.matcher(Files.readString(archivo)).find(),()->"Sintaxis no compatible con MySQL 8: "+archivo.getFileName());
  }
 }
 /** La guarda habría detectado la variante fallida de V7 sin reescribirla. */
 @Test void detectaElPatronQueTeniaV7() {
  assertFalse(!INCOMPATIBLE.matcher("ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS control_version BIGINT").find());
 }
}
