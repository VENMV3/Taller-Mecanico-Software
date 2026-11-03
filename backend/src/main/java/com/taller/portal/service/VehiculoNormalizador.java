package com.taller.portal.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Normaliza los identificadores de negocio del vehículo antes de validarlos o persistirlos. */
@Component
public class VehiculoNormalizador {
 private static final Pattern SEPARADORES=Pattern.compile("[\\s\\p{Z}\\u200B\\u200C\\u200D\\u2060\\uFEFF\\-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015]+");

 /** Obtiene la forma canónica del VIN. @param valor VIN recibido. @return VIN sin separadores y en mayúsculas estables. */
 public String normalizarVin(String valor){return limpiar(valor).toUpperCase(Locale.ROOT);}
 /** Obtiene la forma canónica de la placa. @param valor placa recibida. @return placa sin separadores y en la convención histórica de minúsculas. */
 public String normalizarPlaca(String valor){return limpiar(valor).toLowerCase(Locale.ROOT);}
 /** Aplica NFKC y elimina todos los separadores admitidos. @param valor texto de entrada. @return texto canónico sin separadores. */
 private String limpiar(String valor){return valor==null?"":SEPARADORES.matcher(Normalizer.normalize(valor,Normalizer.Form.NFKC).trim()).replaceAll("");}
}
