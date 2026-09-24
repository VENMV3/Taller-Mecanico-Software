package com.taller.portal.dto;
import jakarta.validation.constraints.*;
public final class AuthDtos {
  public record Login(@NotBlank @Email String email,@NotBlank String password){}
  public record Register(@NotBlank @Size(max=80) String nombre,@NotBlank @Email String email,@NotBlank @Pattern(regexp="^[0-9+ -]{8,25}$") String celular,@NotBlank @Size(min=8,max=72) String password){}
  public record Recovery(@NotBlank String identificador,@NotBlank @Size(min=8,max=72) String nuevaPassword){}
  public record AuthResponse(String token,String nombre,String email,String rol){}
}
