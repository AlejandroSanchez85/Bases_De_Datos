package com.bogotickets.security;

import com.bogotickets.dto.RespuestaError;
import com.bogotickets.dto.SesionUsuario;
import com.bogotickets.repository.CuentaRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

// Quién puede usar cada ruta de la API. La página web y Swagger son públicos; las
// rutas /api/** exigen haber iniciado sesión con el rol de su actor.
@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security busca la cuenta por correo en la vista v_cuentas
    @Bean
    public UserDetailsService cuentas(CuentaRepository repositorio) {
        return correo -> repositorio.buscarPorCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta " + correo));
    }

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, JsonMapper json) throws Exception {
        http.authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/api/auth/**").permitAll()
                        // Cliente: su cuenta, la cartelera y la compra
                        .requestMatchers("/api/cuenta/**").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/compras").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/eventos/*/disponibilidad").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                        // Analista: solo reportes
                        .requestMatchers("/api/reportes/**").hasRole("ANALISTA")
                        // Administrador: todo lo demás (catálogo, clientes, precios, ventas, historial)
                        .requestMatchers("/api/**").hasRole("ADMINISTRADOR")
                        .anyRequest().permitAll())
                .formLogin(login -> login
                        .loginPage("/")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("correo")
                        .passwordParameter("contrasena")
                        .successHandler((peticion, respuesta, auth) ->
                                escribir(json, respuesta, 200, SesionUsuario.de((UsuarioSesion) auth.getPrincipal())))
                        .failureHandler((peticion, respuesta, error) ->
                                escribir(json, respuesta, 401, new RespuestaError(401, "Correo o contraseña incorrectos"))))
                .logout(salida -> salida
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((peticion, respuesta, auth) -> respuesta.setStatus(204)))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((peticion, respuesta, error) ->
                                escribir(json, respuesta, 401, new RespuestaError(401, "Inicie sesión para continuar")))
                        .accessDeniedHandler((peticion, respuesta, error) -> {
                            String mensaje = error instanceof CsrfException
                                    ? "La página tiene un permiso vencido: recárguela e intente de nuevo"
                                    : "Su rol no tiene acceso a esta opción";
                            escribir(json, respuesta, 403, new RespuestaError(403, mensaje));
                        }))
                // La página manda en cada POST, PUT, PATCH y DELETE el token de la cookie
                // XSRF-TOKEN en la cabecera X-XSRF-TOKEN (ver js/api.js)
                .csrf(csrf -> csrf.spa());
        return http.build();
    }

    private static void escribir(JsonMapper json, HttpServletResponse respuesta, int estado, Object cuerpo)
            throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(json.writeValueAsString(cuerpo));
    }
}
