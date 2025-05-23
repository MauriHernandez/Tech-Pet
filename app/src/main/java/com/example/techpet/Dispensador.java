package com.example.techpet;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

public class Dispensador {
    private String id;
    private String modelo;
    private String marca;
    private String asignadoA;
    private String conexion;
    private Map<String, Object> estado;
    private Map<String, Object> alertas;
    private Map<String, Object> control;
    private Map<String, Object> sensores; // Deja que Firebase lo rellene
    private Map<String, Object> configuracion;

    public Dispensador() {
        // Inicializa los mapas vacíos para asegurar que nunca sean null
        // antes de que Firebase intente setearlos o si no existen en Firebase.
        this.estado = new HashMap<>();
        this.alertas = new HashMap<>();
        this.control = new HashMap<>();
        this.sensores = new HashMap<>(); // Inicializa aquí para evitar NullPointerException si no hay 'sensores' en Firebase
        this.configuracion = new HashMap<>();
    }

    // Constructor opcional
    public Dispensador(String id, String modelo, String marca, String asignadoA,
                       String conexion, Map<String, Object> estado, Map<String, Object> alertas,
                       Map<String, Object> control, Map<String, Object> sensores,
                       Map<String, Object> configuracion) {
        this.id = id;
        this.modelo = modelo;
        this.marca = marca;
        this.asignadoA = asignadoA;
        this.conexion = conexion;
        this.estado = estado != null ? estado : new HashMap<>();
        this.alertas = alertas != null ? alertas : new HashMap<>();
        this.control = control != null ? control : new HashMap<>();
        this.sensores = sensores != null ? sensores : new HashMap<>();
        this.configuracion = configuracion != null ? configuracion : new HashMap<>();
    }

    // --- Getters y Setters ---

    public Map<String, Object> getSensores() {
        return sensores;
    }

    // *** MODIFICACIÓN CLAVE PARA DEPURAR 'ambiente' ***
    // Este setter es llamado por Firebase al mapear los datos
    public void setSensores(Map<String, Object> sensores) {
        this.sensores = sensores != null ? sensores : new HashMap<>();
        // Añadimos un log aquí para depurar si 'ambiente' no llega a este objeto Dispensador
        if (!this.sensores.containsKey("ambiente")) {
            Log.w("DispensadorModel", "La clave 'ambiente' no se encontró en el mapa de sensores al setear el Dispensador.");
        }
    }

    // Métodos para obtener valores de sensores con manejo de tipos y nulls más robusto
    public float getTemperatura() {
        try {
            if (sensores != null && sensores.containsKey("ambiente")) {
                Object ambienteObj = sensores.get("ambiente");
                if (ambienteObj instanceof Map) {
                    Map<String, Object> ambiente = (Map<String, Object>) ambienteObj;
                    if (ambiente.containsKey("temperatura")) {
                        Object tempObj = ambiente.get("temperatura");
                        if (tempObj instanceof Number) {
                            return ((Number) tempObj).floatValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Dispensador", "Error al leer temperatura: " + e.getMessage());
        }
        return -1f; // Valor por defecto si hay error o no se encuentra
    }

    public int getHumedad() {
        try {
            if (sensores != null && sensores.containsKey("ambiente")) {
                Object ambienteObj = sensores.get("ambiente");
                if (ambienteObj instanceof Map) {
                    Map<String, Object> ambiente = (Map<String, Object>) ambienteObj;
                    if (ambiente.containsKey("humedad")) {
                        Object humObj = ambiente.get("humedad");
                        if (humObj instanceof Number) {
                            return ((Number) humObj).intValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Dispensador", "Error al leer humedad: " + e.getMessage());
        }
        return -1; // Valor por defecto si hay error o no se encuentra
    }

    public float getPesoActualAgua() {
        return getPesoActualRecipiente("agua");
    }

    public float getCapacidadAgua() {
        return getCapacidadRecipiente("agua");
    }

    public float getPesoActualComida() {
        return getPesoActualRecipiente("comida");
    }

    public float getCapacidadComida() {
        return getCapacidadRecipiente("comida");
    }

    // Método genérico para obtener peso actual de un recipiente (sin cambios)
    private float getPesoActualRecipiente(String tipoRecipiente) {
        try {
            if (sensores != null && sensores.containsKey("recipientes")) {
                Object recipientesObj = sensores.get("recipientes");
                if (recipientesObj instanceof Map) {
                    Map<String, Object> recipientes = (Map<String, Object>) recipientesObj;
                    if (recipientes.containsKey(tipoRecipiente)) {
                        Object tipoRecipienteDataObj = recipientes.get(tipoRecipiente);
                        if (tipoRecipienteDataObj instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) tipoRecipienteDataObj;
                            if (data.containsKey("peso_actual")) {
                                Object pesoObj = data.get("peso_actual");
                                if (pesoObj instanceof Number) {
                                    return ((Number) pesoObj).floatValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Dispensador", "Error al leer peso actual de " + tipoRecipiente + ": " + e.getMessage());
        }
        return 0f; // Valor por defecto
    }

    // Método genérico para obtener capacidad de un recipiente (sin cambios)
    private float getCapacidadRecipiente(String tipoRecipiente) {
        try {
            if (sensores != null && sensores.containsKey("recipientes")) {
                Object recipientesObj = sensores.get("recipientes");
                if (recipientesObj instanceof Map) {
                    Map<String, Object> recipientes = (Map<String, Object>) recipientesObj;
                    if (recipientes.containsKey(tipoRecipiente)) {
                        Object tipoRecipienteDataObj = recipientes.get(tipoRecipiente);
                        if (tipoRecipienteDataObj instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) tipoRecipienteDataObj;
                            if (data.containsKey("capacidad")) {
                                Object capacidadObj = data.get("capacidad");
                                if (capacidadObj instanceof Number) {
                                    return ((Number) capacidadObj).floatValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Dispensador", "Error al leer capacidad de " + tipoRecipiente + ": " + e.getMessage());
        }
        return 0f; // Valor por defecto
    }

    // --- Resto de Getters, Setters, equals y hashCode (sin cambios) ---

    public String getAsignadoA() {
        return asignadoA;
    }

    public void setAsignadoA(String asignadoA) {
        this.asignadoA = asignadoA;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getConexion() {
        if (conexion != null && !conexion.isEmpty()) {
            return conexion;
        } else if (estado != null && estado.containsKey("conexion")) {
            Object connectedObj = estado.get("conexion");
            if (connectedObj instanceof String) {
                return (String) connectedObj;
            }
        }
        return "desconectado";
    }

    public void setConexion(String conexion) {
        this.conexion = conexion;
    }

    public Map<String, Object> getEstado() {
        if (estado == null) {
            estado = new HashMap<>();
        }
        return estado;
    }

    public void setEstado(Map<String, Object> estado) {
        this.estado = estado;
    }

    public Map<String, Object> getAlertas() {
        if (alertas == null) {
            alertas = new HashMap<>();
        }
        return alertas;
    }

    public void setAlertas(Map<String, Object> alertas) {
        this.alertas = alertas;
    }

    public Map<String, Object> getControl() {
        if (control == null) {
            control = new HashMap<>();
        }
        return control;
    }

    public void setControl(Map<String, Object> control) {
        this.control = control;
    }

    public Map<String, Object> getConfiguracion() {
        if (configuracion == null) {
            configuracion = new HashMap<>();
        }
        return configuracion;
    }

    public void setConfiguracion(Map<String, Object> configuracion) {
        this.configuracion = configuracion;
    }

    public boolean isConectado() {
        return "conectado".equalsIgnoreCase(getConexion());
    }

    public Map<String, Object> getServoConfig(String tipoServo) {
        if (configuracion != null && configuracion.containsKey("servos")) {
            Object servosObj = configuracion.get("servos");
            if (servosObj instanceof Map) {
                Map<String, Object> servos = (Map<String, Object>) servosObj;
                Object servoTypeObj = servos.get(tipoServo);
                if (servoTypeObj instanceof Map) {
                    return (Map<String, Object>) servoTypeObj;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dispensador that = (Dispensador) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(modelo, that.modelo) &&
                Objects.equals(marca, that.marca) &&
                Objects.equals(asignadoA, that.asignadoA) &&
                Objects.equals(getConexion(), that.getConexion()) &&
                Objects.equals(estado, that.estado) &&
                Objects.equals(alertas, that.alertas) &&
                Objects.equals(control, that.control) &&
                Objects.equals(sensores, that.sensores) &&
                Objects.equals(configuracion, that.configuracion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelo, marca, asignadoA, getConexion(),
                estado, alertas, control, sensores, configuracion);
    }
}