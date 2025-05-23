package com.example.techpet;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Mascota implements Serializable  {
    private String id;
    private String uidPropietario;
    private String dispositivoAsociado;
    private String nombre;
    private String fechaNacimiento; // Mantenemos como String para simplificar
    private String tipo;
    private String raza;
    private String genero;
    private String color;
    private String fotoUrl;
    private long timestamp;
    private Map<String, PetDetailFragment.PetMetric> metricas; // Historial de pesos y alturas
    private double pesoActual;
    private double alturaActual;

    private Map<String, Object> configAlimentacion;

    // Constructor vacío requerido por Firebase
    public Mascota() {
        this.configAlimentacion = new HashMap<>();
        this.configAlimentacion.put("agua", new HashMap<String, Object>());
        this.configAlimentacion.put("comida", new HashMap<String, Object>());
    }

    // Constructor básico
    public Mascota(String nombre, String tipo, String uidPropietario) {
        this();
        this.nombre = nombre;
        this.tipo = tipo;
        this.uidPropietario = uidPropietario;
        this.timestamp = System.currentTimeMillis();
    }

    // --- Getters y Setters ---

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUidPropietario() {
        return uidPropietario;
    }

    public void setUidPropietario(String uidPropietario) {
        this.uidPropietario = uidPropietario;
    }

    public String getDispositivoAsociado() {
        return dispositivoAsociado;
    }

    public void setDispositivoAsociado(String dispositivoAsociado) {
        this.dispositivoAsociado = dispositivoAsociado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getRaza() {
        return raza;
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getConfigAlimentacion() {
        return configAlimentacion;
    }

    public void setConfigAlimentacion(Map<String, Object> configAlimentacion) {
        this.configAlimentacion = configAlimentacion;
    }

    // --- Métodos para Firebase Realtime Database ---

    // Agregar estos métodos a la clase Mascota
    public double getPesoActual() {
        return pesoActual;
    }

    public void setPesoActual(double pesoActual) {
        this.pesoActual = pesoActual;
    }

    public double getAlturaActual() {
        return alturaActual;
    }

    public void setAlturaActual(double alturaActual) {
        this.alturaActual = alturaActual;
    }

    // Actualizar el método toMap()
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("uidPropietario", uidPropietario);
        result.put("dispositivoAsociado", dispositivoAsociado);
        result.put("nombre", nombre);
        result.put("fechaNacimiento", fechaNacimiento);
        result.put("tipo", tipo);
        result.put("raza", raza);
        result.put("genero", genero);
        result.put("color", color);
        result.put("fotoUrl", fotoUrl);
        result.put("timestamp", timestamp);
        result.put("configAlimentacion", configAlimentacion);
        result.put("pesoActual", pesoActual);  // Añadir esto
        result.put("alturaActual", alturaActual);  // Añadir esto
        if (metricas != null) {
            result.put("metricas", metricas);
        }
        return result;
    }

    // --- Métodos útiles ---

    @Exclude
    public boolean tieneDispositivoAsociado() {
        return dispositivoAsociado != null && !dispositivoAsociado.isEmpty();
    }

    @Exclude
    public void setConfigAgua(int cantidadPorRacion, int nivelMinimoPlato, int pesoRecomendado) {
        Map<String, Object> aguaConfig = new HashMap<>();
        aguaConfig.put("cantidad_por_racion", cantidadPorRacion);
        aguaConfig.put("nivel_minimo_plato", nivelMinimoPlato);
        aguaConfig.put("peso_recomendado_plato", pesoRecomendado);
        configAlimentacion.put("agua", aguaConfig);
    }

    @Exclude
    public void setConfigComida(int cantidadPorRacion, int nivelMinimoPlato, int pesoRecomendado, Map<String, String> horarios) {
        Map<String, Object> comidaConfig = new HashMap<>();
        comidaConfig.put("cantidad_por_racion", cantidadPorRacion);
        comidaConfig.put("nivel_minimo_plato", nivelMinimoPlato);
        comidaConfig.put("peso_recomendado_plato", pesoRecomendado);
        comidaConfig.put("horarios", horarios);
        configAlimentacion.put("comida", comidaConfig);
    }
}