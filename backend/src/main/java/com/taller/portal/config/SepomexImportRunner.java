package com.taller.portal.config;

import com.taller.portal.service.SepomexImportService;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/** Ejecuta la importación desde el script y termina el proceso; no se activa en la API normal. */
@Component
@ConditionalOnProperty(name = "app.sepomex.import-file")
public class SepomexImportRunner implements ApplicationRunner {
    private final SepomexImportService importador; private final ConfigurableApplicationContext contexto;
    /** @param importador servicio transaccional @param contexto aplicación que se cerrará tras completar. */
    public SepomexImportRunner(SepomexImportService importador, ConfigurableApplicationContext contexto) { this.importador = importador; this.contexto = contexto; }
    /** @param argumentos argumentos Spring; importa el archivo y finaliza con código cero o propaga el error. @throws Exception si falla la importación. */
    @Override public void run(ApplicationArguments argumentos) throws Exception {
        String archivo = contexto.getEnvironment().getProperty("app.sepomex.import-file");
        var resultado = importador.importar(Path.of(archivo));
        System.out.printf("SEPOMEX importado: %d filas leídas, %d nuevas, %d total.%n", resultado.filasArchivo(), resultado.filasInsertadas(), resultado.totalCatalogo());
        System.exit(SpringApplication.exit(contexto));
    }
}
