package com.example.techpet;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit; // Necesario para manejo de fechas/semanas

public class ReportFragment extends Fragment {
    private DatabaseReference mDatabase;
    private ValueEventListener mDispensersListener;
    private List<String> deviceIds = new ArrayList<>();
    // Usamos la clase DispenserData corregida (definida abajo)
    private Map<String, DispenserData> dispensersData = new HashMap<>();

    private TextView tvDeviceName, tvEstadoConexion, tvUltimaActualizacion;
    private RadioGroup periodGroup;
    // Asumiendo que tienes referencias a tus clases de gráficos personalizadas
    private ConsumptionChart barChart; // Para consumo de comida
    private ConsumptionChart lineChart; // Para temperatura del agua (con limitaciones)

    private final String TAG = "ReportFragment"; // Para Logs

    // Constantes para los tipos de historial (ajusta si tus tipos son diferentes)
    private static final String TIPO_COMIDA_DISPENSADA = "dispensacion_comida";
    private static final String TIPO_COMIDA_PROGRAMADA = "dispensacion_programada";
    // private static final String TIPO_AGUA_DISPENSADA = "dispensacion_agua"; // Si tuvieras historial de agua

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        initViews(view);
        setupPeriodSelector();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // Establecer la selección inicial del RadioGroup (opcional, pero bueno para el inicio)
        periodGroup.check(R.id.btnDaily);
        return view;
    }

    private void initViews(View view) {
        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        tvEstadoConexion = view.findViewById(R.id.tvEstadoConexion);
        tvUltimaActualizacion = view.findViewById(R.id.tvUltimaActualizacion);
        periodGroup = view.findViewById(R.id.periodGroup);
        // Asumiendo IDs correctos para tus gráficos en fragment_report.xml
        barChart = view.findViewById(R.id.barChart); // Asigna tu gráfico de barras


        tvDeviceName.setText("Consolidado de todos los dispensadores");
        // Puedes personalizar los gráficos aquí si es necesario (colores, etc.)
        // barChart.setBarColor(...)
        // lineChart.setLineColor(...)
    }

    private void setupPeriodSelector() {
        periodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "Periodo seleccionado: " + checkedId);
            updateChartData(); // Llama a la función centralizada
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Configurando listeners");
        setupFirebaseListeners();
        // La carga inicial de datos se hará cuando el listener obtenga los datos por primera vez
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop - Removiendo listeners");
        // Remover el listener principal para evitar fugas de memoria
        if (mDispensersListener != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                DatabaseReference userDispensersRef = mDatabase.child("usuarios").child(user.getUid()).child("dispensadores");
                userDispensersRef.removeEventListener(mDispensersListener);
                Log.d(TAG, "Listener de dispensadores removido.");
            }
        }
        // No es necesario remover los listeners de addListenerForSingleValueEvent,
        // ya que se disparan solo una vez.
    }

    private void setupFirebaseListeners() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "Usuario no autenticado.");
            // Quizás mostrar un mensaje al usuario o redirigir al login
            clearDataAndUI(); // Limpiar datos si no hay usuario
            return;
        }

        DatabaseReference userDispensersRef = mDatabase.child("usuarios").child(user.getUid()).child("dispensadores");

        // Si ya existe un listener, removerlo antes de añadir uno nuevo
        if (mDispensersListener != null) {
            userDispensersRef.removeEventListener(mDispensersListener);
            Log.d(TAG,"Removiendo listener antiguo antes de añadir uno nuevo.");
        }


        mDispensersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Listener principal: onDataChange recibido.");
                deviceIds.clear();
                dispensersData.clear(); // Limpiar datos anteriores

                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    Log.w(TAG, "No se encontraron dispensadores para el usuario: " + user.getUid());
                    clearDataAndUI(); // Limpiar la UI si no hay dispensadores
                    return; // Salir si no hay dispensadores
                }

                long totalDevices = dataSnapshot.getChildrenCount();
                Log.d(TAG, "Número de dispensadores encontrados: " + totalDevices);
                // Usar AtomicInteger para manejar el conteo asíncrono de forma segura
                AtomicInteger remainingDevices = new AtomicInteger((int) totalDevices);

                for (DataSnapshot dispenserIdSnapshot : dataSnapshot.getChildren()) {
                    String deviceId = dispenserIdSnapshot.getKey();
                    if (deviceId == null) {
                        Log.w(TAG, "Se encontró un dispensador con ID nulo.");
                        // Decrementar si contamos un ID nulo para no bloquear la espera
                        if (remainingDevices.decrementAndGet() == 0) {
                            Log.d(TAG, "Última llamada (con ID nulo) - Actualizando UI...");
                            processAndDisplayData();
                        }
                        continue; // Saltar este dispensador
                    }
                    deviceIds.add(deviceId);
                    Log.d(TAG, "Procesando dispensador ID: " + deviceId);

                    // Obtener los datos completos de este dispensador específico
                    DatabaseReference specificDispenserRef = userDispensersRef.child(deviceId);
                    specificDispenserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot specificDataSnapshot) {
                            if (specificDataSnapshot.exists()) {
                                // Intentar deserializar usando la clase DispenserData CORREGIDA
                                try {
                                    DispenserData data = specificDataSnapshot.getValue(DispenserData.class);
                                    if (data != null) {
                                        data.deviceId = deviceId; // Asignar el ID manualmente
                                        dispensersData.put(deviceId, data);
                                        Log.d(TAG, "Datos cargados para: " + deviceId);
                                    } else {
                                        Log.w(TAG, "Datos deserializados son nulos para: " + deviceId);
                                    }
                                } catch (DatabaseException e) {
                                    Log.e(TAG, "Error al deserializar datos para: " + deviceId + ". ¿La estructura de la clase Java coincide con Firebase?", e);
                                    // Puedes añadir lógica aquí para manejar el error,
                                    // como poner un objeto DispenserData con estado de error.
                                }
                            } else {
                                Log.w(TAG, "Snapshot no existe para el dispensador específico: " + deviceId);
                            }

                            // Verificar si este fue el último listener en terminar
                            if (remainingDevices.decrementAndGet() == 0) {
                                Log.d(TAG, "Todos los listeners individuales completados. Actualizando UI...");
                                processAndDisplayData();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Error al cargar datos del dispensador: " + deviceId, databaseError.toException());
                            // Aún así, decrementamos y verificamos si es el último
                            if (remainingDevices.decrementAndGet() == 0) {
                                Log.d(TAG, "Todos los listeners individuales completados (con error en uno). Actualizando UI...");
                                processAndDisplayData();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error en el listener principal de Firebase", databaseError.toException());
                // Quizás mostrar un error general al usuario
                clearDataAndUI();
            }
        };

        // Añadir el listener principal
        userDispensersRef.addValueEventListener(mDispensersListener);
        Log.d(TAG, "Listener principal de dispensadores añadido.");
    }

    // Método centralizado para procesar datos y actualizar UI después de cargar todo
    private void processAndDisplayData() {
        Log.d(TAG, "processAndDisplayData - Actualizando estado y gráficos.");
        updateConsolidatedStatus();
        updateChartData(); // Esto llamará al método de carga apropiado (daily, weekly, monthly)
    }

    // Método para limpiar datos y UI
    private void clearDataAndUI() {
        Log.d(TAG, "clearDataAndUI - Limpiando datos y UI.");
        deviceIds.clear();
        dispensersData.clear();
        if (isAdded() && getContext() != null) { // Verificar que el fragmento está añadido
            tvEstadoConexion.setText("Estado: --/-- conectados");
            tvEstadoConexion.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey)); // Un color neutral
            tvUltimaActualizacion.setText("Última actualización: -- | Consumo total: --");
            showEmptyDataMessage(); // Limpiar gráficos
        }
    }


    private void updateConsolidatedStatus() {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG,"updateConsolidatedStatus - Fragmento no añadido, saltando actualización.");
            return; // Salir si el fragmento no está adjunto
        }

        int connected = 0;
        long mostRecentUpdate = 0; // Timestamp de la última actualización encontrada
        float totalFoodConsumption = 0;
        // float totalWaterConsumption = 0; // No podemos calcularlo del historial_acciones actual

        for (DispenserData data : dispensersData.values()) {
            if (data == null) continue; // Saltar si hubo error al cargar datos

            // Contar conectados usando el campo 'estado'
            if ("conectado".equalsIgnoreCase(data.estado)) {
                connected++;
            }

            // Encontrar el timestamp más reciente. Usaremos configuracion.ultima_actualizacion
            // como fuente principal, pero podríamos considerar sensores también.
            if (data.configuracion != null && data.configuracion.ultima_actualizacion != null) {
                if (data.configuracion.ultima_actualizacion > mostRecentUpdate) {
                    mostRecentUpdate = data.configuracion.ultima_actualizacion;
                }
            }
            // Opcional: considerar también la última medición de sensores si es relevante
            if (data.sensores != null) {
                long lastSensorTime = 0;
                if (data.sensores.ambiente != null && data.sensores.ambiente.ultima_medicion != null) {
                    lastSensorTime = Math.max(lastSensorTime, data.sensores.ambiente.ultima_medicion);
                }
                if (data.sensores.platos != null) {
                    if (data.sensores.platos.agua != null && data.sensores.platos.agua.ultima_medicion != null) {
                        lastSensorTime = Math.max(lastSensorTime, data.sensores.platos.agua.ultima_medicion);
                    }
                    if (data.sensores.platos.comida != null && data.sensores.platos.comida.ultima_medicion != null) {
                        lastSensorTime = Math.max(lastSensorTime, data.sensores.platos.comida.ultima_medicion);
                    }
                }

            }



            if (data.historial_acciones != null) {
                for (Map.Entry<String, HistorialAccion> entry : data.historial_acciones.entrySet()) {
                    HistorialAccion accion = entry.getValue();
                    // Asegurarse de que es un evento de comida y sumar la ración
                    if (accion != null && accion.tipo != null &&
                            (TIPO_COMIDA_DISPENSADA.equalsIgnoreCase(accion.tipo) || TIPO_COMIDA_PROGRAMADA.equalsIgnoreCase(accion.tipo))) {
                        totalFoodConsumption += accion.racion;
                    }

                }
            }
        }

        // Actualizar TextView de Estado
        String statusText = "Estado: " + connected + "/" + deviceIds.size() + " conectados";
        tvEstadoConexion.setText(statusText);

        // Actualizar color del estado
        int color;
        if (deviceIds.isEmpty()) {
            color = ContextCompat.getColor(requireContext(), R.color.grey); // Gris si no hay dispositivos
        } else if (connected == deviceIds.size()) {
            color = ContextCompat.getColor(requireContext(), R.color.green); // Verde si todos conectados
        } else if (connected > 0) {
            color = ContextCompat.getColor(requireContext(), R.color.green); // Naranja si algunos conectados (o usa verde)
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.red); // Rojo si ninguno conectado
        }
        tvEstadoConexion.setTextColor(color);

        // Actualizar TextView de Última Actualización y Consumo Total
        String lastUpdateStr = (mostRecentUpdate > 0) ? formatTimestamp(mostRecentUpdate) : "--";
        // Mostrar consumo de comida. Ajusta las unidades (g, kg) si es necesario.
        String consumptionStr = String.format(Locale.getDefault(), "%.1f g", totalFoodConsumption);
        // Si pudieras calcular agua: String.format("%.1f ml", totalWaterConsumption)

        tvUltimaActualizacion.setText("Última actualización: " + lastUpdateStr +
                " | Consumo total: Comida: " + consumptionStr);
        // Añadir agua si fuera posible: + ", Agua: " + waterConsumptionStr);
    }

    // Llama al método de carga de datos correcto basado en el RadioButton seleccionado
    private void updateChartData() {
        if (!isAdded()) return; // No hacer nada si el fragmento no está adjunto

        int selectedId = periodGroup.getCheckedRadioButtonId();
        Log.d(TAG,"updateChartData - Botón seleccionado: " + selectedId);

        if (selectedId == R.id.btnDaily) {
            loadDailyData();
        } else if (selectedId == R.id.btnWeekly) {
            loadWeeklyData();
        } else if (selectedId == R.id.btnMonthly) {
            loadMonthlyData();
        } else {
            Log.w(TAG,"Ningún botón de periodo seleccionado? Mostrando datos diarios por defecto.");
            loadDailyData(); // Cargar diario si no hay selección (o limpiar gráficos)
        }
    }

    // Carga y muestra datos DIARIOS

    // Carga y muestra datos DIARIOS
    private void loadDailyData() {
        Log.d(TAG, "Cargando datos diarios...");
        if (!isAdded() || dispensersData.isEmpty()) {
            Log.d(TAG, "Sin datos de dispensadores o fragmento no añadido, limpiando gráficos diarios.");
            showEmptyDataMessage();
            return;
        }

        // Mapa para acumular consumo de comida por día de la semana (Lun, Mar, ...)
        Map<String, Float> foodByDay = new LinkedHashMap<>();
        // Mapa para almacenar la ÚLTIMA temperatura registrada por día de la semana
        Map<String, Float> tempByDay = new LinkedHashMap<>();
        // Mapa para contar cuántas lecturas de temperatura tenemos por día (para posible promedio)
        // Map<String, Integer> tempCountByDay = new LinkedHashMap<>(); // Descomentar si implementas promedio

        // Usar Locale español para los nombres de los días
        Locale spanishLocale = new Locale("es", "ES");
        String[] daysOfWeek = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", spanishLocale); // Formato corto del día (Lun, Mar...)

        // Inicializar mapas para todos los días de la semana
        for (String day : daysOfWeek) {
            foodByDay.put(day, 0f);
            tempByDay.put(day, null); // Iniciar temp como null para saber si se encontró alguna
            // tempCountByDay.put(day, 0); // Descomentar si implementas promedio
        }

        // Procesar datos de cada dispensador
        for (DispenserData data : dispensersData.values()) {
            if (data == null) {
                Log.w(TAG,"Procesando datos diarios: Se encontró un DispenserData nulo.");
                continue; // Saltar este dispensador si sus datos son nulos
            }

            // *** Procesar historial de comida para el gráfico de barras diario ***
            if (data.historial_acciones != null) {
                for (Map.Entry<String, HistorialAccion> entry : data.historial_acciones.entrySet()) {
                    String key = entry.getKey();
                    HistorialAccion accion = entry.getValue();

                    if (accion == null || accion.tipo == null || key == null) continue;

                    // Verificar si es un evento de comida
                    if (TIPO_COMIDA_DISPENSADA.equalsIgnoreCase(accion.tipo) || TIPO_COMIDA_PROGRAMADA.equalsIgnoreCase(accion.tipo)) {
                        try {
                            // Extraer timestamp de la clave "timestamp_NUMERO"
                            long timestamp = Long.parseLong(key.substring(key.indexOf('_') + 1));

                            // Considerar solo eventos de los últimos 7 días para el gráfico diario
                            // (Ajusta el '7' si necesitas otro rango)
                            if (isWithinLastNDays(timestamp, 7)) {
                                String dayKey = getDayOfWeek(timestamp, dayFormat);
                                if (foodByDay.containsKey(dayKey)) {
                                    foodByDay.put(dayKey, foodByDay.get(dayKey) + accion.racion);
                                } else {
                                    Log.w(TAG,"Clave de día ("+dayKey+") no encontrada en foodByDay al procesar historial.");
                                }
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            Log.w(TAG, "Error parseando timestamp de clave historial diario: " + key, e);
                        }
                    }
                }
            } else {
                Log.d(TAG, "Device " + data.deviceId + ": No se encontró historial_acciones.");
            }


            // *** Procesar ÚLTIMA temperatura del agua del PLATO para gráfico de líneas diario ***
            if (data.sensores != null && data.sensores.platos != null && data.sensores.platos.agua != null) {
                Float tempValue = data.sensores.platos.agua.temperatura;
                Long tempTimestamp = data.sensores.platos.agua.ultima_medicion;

                // --- Log Detallado ---
                Log.d(TAG, " --- DEBUG TEMP: Device " + data.deviceId + " ---");
                Log.d(TAG, "DEBUG TEMP: tempValue = " + tempValue);
                Log.d(TAG, "DEBUG TEMP: tempTimestamp = " + tempTimestamp);
                // --- Fin Log Detallado ---

                if (tempValue != null && tempTimestamp != null && tempTimestamp > 0) {
                    // ***** MODIFICACIÓN *****
                    // Ahora SIEMPRE procesamos la temperatura, sin importar qué tan antigua sea.
                    // Simplemente la asignamos al día en que se tomó la medición.
                    boolean processTemperature = true; // Siempre procesar si los datos existen

                    if (processTemperature) {
                        String dayKey = getDayOfWeek(tempTimestamp, dayFormat);
                        Log.d(TAG, "DEBUG TEMP: Calculado dayKey = " + dayKey + " para timestamp " + tempTimestamp);

                        if (tempByDay.containsKey(dayKey)) {
                            // Almacena la última temperatura encontrada para ese día
                            // Si hubiera múltiples dispositivos, esto sobrescribiría con la última leída en el bucle.
                            // Podrías querer calcular un promedio si varios dispositivos reportan el mismo día.
                            tempByDay.put(dayKey, tempValue);
                            Log.d(TAG, "DEBUG TEMP: Temp " + tempValue + " almacenada para día " + dayKey);
                        } else {
                            // Este log indica un problema si dayKey no es "Lun", "Mar", etc. o si el mapa no se inicializó bien.
                            Log.w(TAG,"DEBUG TEMP: Clave de día ("+dayKey+") NO encontrada en tempByDay. Verifica dayFormat y daysOfWeek.");
                        }
                    }
                } else {
                    Log.d(TAG, "DEBUG TEMP: Device " + data.deviceId + ": Valor de Temp ("+tempValue+") o Timestamp ("+tempTimestamp+") es null o inválido.");
                }
            } else {
                // Log si falta alguna parte de la ruta para llegar a la temperatura
                String missingPath = "sensores";
                if (data.sensores != null) missingPath += ".platos";
                if (data.sensores != null && data.sensores.platos != null) missingPath += ".agua";
                if (data.sensores != null && data.sensores.platos != null && data.sensores.platos.agua != null) missingPath += ".temperatura";
                Log.d(TAG, "DEBUG TEMP: Device " + data.deviceId + ": Ruta de sensor de temperatura incompleta o nula: " + missingPath);
            }
        } // Fin del bucle for (DispenserData data : dispensersData.values())


        // Preparar datos para los gráficos
        List<String> labels = new ArrayList<>(Arrays.asList(daysOfWeek));
        List<Float> foodDataValues = new ArrayList<>();
        List<Float> tempDataValues = new ArrayList<>();

        for (String dayKey : daysOfWeek) {
            foodDataValues.add(foodByDay.get(dayKey));
            // Añadir temperatura o 0f si no hay datos para ese día (o si el valor era null)
            tempDataValues.add(tempByDay.get(dayKey) != null ? tempByDay.get(dayKey) : 0f);
        }

        // --- Log Final para Temperatura ---
        Log.i(TAG, ">>> Datos Finales para LineChart (Temperatura): " + tempDataValues);
        // --- Fin Log Final ---


        // Actualizar gráfico de BARRAS (Comida)
        if (foodDataValues.size() == labels.size() && barChart != null) {
            Log.d(TAG, "Actualizando BarChart Diario. Labels: " + labels + ", Data: " + foodDataValues);
            barChart.setData(foodDataValues, labels);
        } else {
            Log.e(TAG, "Error de tamaño ("+foodDataValues.size()+" vs "+labels.size()+") o BarChart nulo para datos diarios.");
            if (barChart != null) barChart.setData(Collections.emptyList(), Collections.emptyList());
        }

        // Actualizar gráfico de LÍNEAS (Temperatura) - *** ¡¡¡USANDO LA VISTA INCORRECTA!!! ***
        if (tempDataValues.size() == labels.size() && lineChart != null) {
            // Aunque llamemos a setData, ConsumptionChart dibujará BARRAS, no una línea.
            Log.d(TAG, "Actualizando LineChart Diario (con ConsumptionChart). Labels: " + labels + ", Data: " + tempDataValues);
            lineChart.setData(tempDataValues, labels);
            // !!! ADVERTENCIA: Necesitas cambiar 'lineChart' a una vista que dibuje LÍNEAS !!!
        } else {
            Log.e(TAG, "Error de tamaño ("+tempDataValues.size()+" vs "+labels.size()+") o LineChart (ConsumptionChart) nulo para datos diarios.");
            if (lineChart != null) lineChart.setData(Collections.emptyList(), Collections.emptyList());
        }
    }
    // Carga y muestra datos SEMANALES
    private void loadWeeklyData() {
        Log.d(TAG, "Cargando datos semanales...");
        if (!isAdded() || dispensersData.isEmpty()) {
            Log.d(TAG, "Sin datos de dispensadores o fragmento no añadido, limpiando gráficos semanales.");
            showEmptyDataMessage();
            return;
        }

        // Mapa para acumular consumo por semana del año
        Map<Integer, Float> foodByWeek = new LinkedHashMap<>();
        // Mapa para temperaturas semanales (opcional, requiere definir qué mostrar: promedio, max, min)
        // Map<Integer, List<Float>> tempsByWeek = new LinkedHashMap<>();

        // Definir las etiquetas y semanas a mostrar (ej: últimas 4 semanas)
        List<String> labels = new ArrayList<>();
        List<Integer> weeksToShow = new ArrayList<>(); // Guardar los números de semana

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY); // Opcional: definir inicio de semana

        // Obtener las últimas 4 semanas (incluyendo la actual)
        for (int i = 3; i >= 0; i--) {
            Calendar weekCal = (Calendar) cal.clone();
            weekCal.add(Calendar.WEEK_OF_YEAR, -i);
            int year = weekCal.get(Calendar.YEAR);
            int weekOfYear = weekCal.get(Calendar.WEEK_OF_YEAR);
            // Crear una clave única para semana/año para evitar problemas en cambio de año
            int weekKey = year * 100 + weekOfYear;

            // Crear etiqueta legible (ej: "Sem 19/25" o rango de fechas)
            // Simple:
            String label = "Sem " + weekOfYear;
            // Más complejo: Calcular inicio/fin de semana
            // weekCal.set(Calendar.DAY_OF_WEEK, weekCal.getFirstDayOfWeek());
            // Date startDate = weekCal.getTime();
            // weekCal.add(Calendar.DAY_OF_WEEK, 6);
            // Date endDate = weekCal.getTime();
            // SimpleDateFormat weekDateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            // String label = weekDateFormat.format(startDate) + "-" + weekDateFormat.format(endDate);

            labels.add(label);
            weeksToShow.add(weekKey);
            foodByWeek.put(weekKey, 0f);
            // tempsByWeek.put(weekKey, new ArrayList<>());
        }

        // Procesar datos históricos
        for (DispenserData data : dispensersData.values()) {
            if (data == null || data.historial_acciones == null) continue;

            for (Map.Entry<String, HistorialAccion> entry : data.historial_acciones.entrySet()) {
                String key = entry.getKey();
                HistorialAccion accion = entry.getValue();
                if (accion == null || accion.tipo == null || key == null) continue;

                // Sumar comida
                if (TIPO_COMIDA_DISPENSADA.equalsIgnoreCase(accion.tipo) || TIPO_COMIDA_PROGRAMADA.equalsIgnoreCase(accion.tipo)) {
                    try {
                        long timestamp = Long.parseLong(key.substring(key.indexOf('_') + 1));
                        Calendar eventCal = Calendar.getInstance();
                        eventCal.setTimeInMillis(timestamp);
                        eventCal.setFirstDayOfWeek(Calendar.MONDAY); // Asegurar consistencia
                        int eventYear = eventCal.get(Calendar.YEAR);
                        int eventWeekOfYear = eventCal.get(Calendar.WEEK_OF_YEAR);
                        int eventWeekKey = eventYear * 100 + eventWeekOfYear;

                        if (foodByWeek.containsKey(eventWeekKey)) {
                            foodByWeek.put(eventWeekKey, foodByWeek.get(eventWeekKey) + accion.racion);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error procesando historial semanal: " + key, e);
                    }
                }
                // Procesar temperatura si se implementa...
            }
            // Procesar última temperatura si se quiere agregar al gráfico semanal (requiere lógica de agregación)
             /*
             if (data.sensores != null && ...) {
                  long tempTimestamp = ...;
                  float tempValue = ...;
                  Calendar tempCal = Calendar.getInstance();
                  tempCal.setTimeInMillis(tempTimestamp);
                  // ... calcular weekKey ...
                  if (tempsByWeek.containsKey(weekKey)) {
                      tempsByWeek.get(weekKey).add(tempValue);
                  }
             }
             */
        }

        // Preparar datos finales para el gráfico de barras
        List<Float> foodDataValues = new ArrayList<>();
        for (int weekKey : weeksToShow) {
            foodDataValues.add(foodByWeek.getOrDefault(weekKey, 0f));
        }

        // Preparar datos de temperatura si se implementa
        // List<Float> tempDataValues = calculateWeeklyTemps(tempsByWeek, weeksToShow); // Necesitarías esta función

        // Actualizar gráfico de barras
        if (foodDataValues.size() == labels.size() && barChart != null) {
            Log.d(TAG, "Actualizando BarChart Semanal. Labels: " + labels + ", Data: " + foodDataValues);
            barChart.setData(foodDataValues, labels);
        } else {
            Log.e(TAG, "Error de tamaño o BarChart nulo para datos semanales.");
            if (barChart != null) barChart.setData(Collections.emptyList(), Collections.emptyList());
        }

        // Actualizar gráfico de líneas (con datos vacíos por ahora para temp)
        if (lineChart != null) {
            Log.d(TAG, "Limpiando LineChart Semanal (temp no implementada).");
            lineChart.setData(Collections.emptyList(), Collections.emptyList());
        }
    }

    // Carga y muestra datos MENSUALES
    private void loadMonthlyData() {
        Log.d(TAG, "Cargando datos mensuales...");
        if (!isAdded() || dispensersData.isEmpty()) {
            Log.d(TAG, "Sin datos de dispensadores o fragmento no añadido, limpiando gráficos mensuales.");
            showEmptyDataMessage();
            return;
        }

        // Mapa para acumular consumo por mes (clave YYYYMM)
        Map<Integer, Float> foodByMonth = new LinkedHashMap<>();
        // Mapa para temperaturas mensuales (opcional)
        // Map<Integer, List<Float>> tempsByMonth = new LinkedHashMap<>();

        // Definir las etiquetas y meses a mostrar (ej: últimos 6 meses)
        List<String> labels = new ArrayList<>();
        List<Integer> monthsToShow = new ArrayList<>(); // Guardar las claves YYYYMM

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yy", new Locale("es", "ES")); // Formato "May 25"

        // Obtener los últimos 6 meses (incluyendo el actual)
        for (int i = 5; i >= 0; i--) {
            Calendar monthCal = (Calendar) cal.clone();
            monthCal.add(Calendar.MONTH, -i);
            int year = monthCal.get(Calendar.YEAR);
            int month = monthCal.get(Calendar.MONTH); // 0-11
            // Crear clave única YYYYMM
            int monthKey = year * 100 + (month + 1); // +1 para mes 1-12

            labels.add(monthFormat.format(monthCal.getTime()));
            monthsToShow.add(monthKey);
            foodByMonth.put(monthKey, 0f);
            // tempsByMonth.put(monthKey, new ArrayList<>());
        }

        // Procesar datos históricos
        for (DispenserData data : dispensersData.values()) {
            if (data == null || data.historial_acciones == null) continue;

            for (Map.Entry<String, HistorialAccion> entry : data.historial_acciones.entrySet()) {
                String key = entry.getKey();
                HistorialAccion accion = entry.getValue();
                if (accion == null || accion.tipo == null || key == null) continue;

                // Sumar comida
                if (TIPO_COMIDA_DISPENSADA.equalsIgnoreCase(accion.tipo) || TIPO_COMIDA_PROGRAMADA.equalsIgnoreCase(accion.tipo)) {
                    try {
                        long timestamp = Long.parseLong(key.substring(key.indexOf('_') + 1));
                        Calendar eventCal = Calendar.getInstance();
                        eventCal.setTimeInMillis(timestamp);
                        int eventYear = eventCal.get(Calendar.YEAR);
                        int eventMonth = eventCal.get(Calendar.MONTH); // 0-11
                        int eventMonthKey = eventYear * 100 + (eventMonth + 1); // Clave YYYYMM

                        if (foodByMonth.containsKey(eventMonthKey)) {
                            foodByMonth.put(eventMonthKey, foodByMonth.get(eventMonthKey) + accion.racion);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error procesando historial mensual: " + key, e);
                    }
                }
                // Procesar temperatura si se implementa...
            }
            // Procesar última temperatura si se quiere agregar al gráfico mensual...
        }

        // Preparar datos finales para el gráfico de barras
        List<Float> foodDataValues = new ArrayList<>();
        for (int monthKey : monthsToShow) {
            foodDataValues.add(foodByMonth.getOrDefault(monthKey, 0f));
        }

        // Preparar datos de temperatura si se implementa...

        // Actualizar gráfico de barras
        if (foodDataValues.size() == labels.size() && barChart != null) {
            Log.d(TAG, "Actualizando BarChart Mensual. Labels: " + labels + ", Data: " + foodDataValues);
            barChart.setData(foodDataValues, labels);
        } else {
            Log.e(TAG, "Error de tamaño o BarChart nulo para datos mensuales.");
            if (barChart != null) barChart.setData(Collections.emptyList(), Collections.emptyList());
        }

        // Actualizar gráfico de líneas (con datos vacíos por ahora para temp)
        if (lineChart != null) {
            Log.d(TAG, "Limpiando LineChart Mensual (temp no implementada).");
            lineChart.setData(Collections.emptyList(), Collections.emptyList());
        }
    }


    // Limpia ambos gráficos
    private void showEmptyDataMessage() {
        if (!isAdded()) return;
        Log.d(TAG,"showEmptyDataMessage - Limpiando gráficos.");
        if (barChart != null) {
            barChart.setData(Collections.emptyList(), Collections.emptyList());
        }
        if (lineChart != null) {
            lineChart.setData(Collections.emptyList(), Collections.emptyList());
        }
    }

    // Formatea timestamp a "dd/MM HH:mm"
    private String formatTimestamp(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "--";
        try {
            // Asegúrate que el timestamp está en milisegundos
            // Si viene en segundos, multiplica por 1000L
            // long timestampMillis = (timestamp < 10000000000L) ? timestamp * 1000L : timestamp; // Ejemplo heurístico
            long timestampMillis = timestamp; // Asumiendo que ya viene en milisegundos
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestampMillis));
        } catch (Exception e) {
            Log.e(TAG,"Error formateando timestamp: " + timestamp, e);
            return "--";
        }
    }

    // Obtiene el nombre corto del día ("Lun", "Mar", etc.) para un timestamp
    private String getDayOfWeek(long timestamp, SimpleDateFormat dayFormat) {
        if (timestamp <= 0) return "";
        try {
            // long timestampMillis = (timestamp < 10000000000L) ? timestamp * 1000L : timestamp; // Si viene en segundos
            long timestampMillis = timestamp; // Asumiendo milisegundos
            return dayFormat.format(new Date(timestampMillis));
        } catch (Exception e) {
            Log.e(TAG,"Error obteniendo día de la semana para timestamp: " + timestamp, e);
            return "";
        }

    }

    // Función auxiliar para verificar si un timestamp está dentro de los últimos N días
    private boolean isWithinLastNDays(long timestamp, int days) {
        if (timestamp <= 0) return false;
        long currentMillis = System.currentTimeMillis();
        long daysInMillis = TimeUnit.DAYS.toMillis(days);
        // Comprobar si el timestamp está entre (ahora - N días) y ahora
        return timestamp > (currentMillis - daysInMillis) && timestamp <= currentMillis;
    }


    // -------------------------------------------------------------------------
    // CLASES DE DATOS ANIDADAS (Deben coincidir con tu estructura Firebase)
    // Usando nombres de variables EXACTOS a las claves JSON (sin @PropertyName)
    // -------------------------------------------------------------------------

    public static class DispenserData {
        public String deviceId; // Asignado manualmente
        public String estado;
        public ConfiguracionData configuracion;
        public SensoresData sensores;
        public Map<String, HistorialAccion> historial_acciones;
        public ComandosData comandos;

        public DispenserData() {} // Constructor vacío requerido
    }

    public static class ConfiguracionData {
        public String modo;
        public Long ultima_actualizacion;
        public ConfiguracionData() {}
    }

    public static class SensoresData {
        public AmbienteData ambiente;
        public PlatosData platos;
        public RecipientesData recipientes;
        public SensoresData() {}
    }

    public static class AmbienteData {
        public Float humedad; // Usar Float/Double/Long objects para permitir nulls
        public Float temperatura;
        public Long ultima_medicion;
        public AmbienteData() {}
    }

    public static class PlatosData {
        public PlatoAguaData agua;
        public PlatoComidaData comida;
        public PlatosData() {}
    }

    public static class PlatoAguaData {
        public Float capacidad;
        public Float peso_actual;
        public Float temperatura; // <-- Temperatura del agua del plato
        public Long ultima_medicion;
        public PlatoAguaData() {}
    }

    public static class PlatoComidaData {
        public Float capacidad;
        public Float humedad; // <-- Humedad de la comida del plato
        public Float peso_actual;
        public Long ultima_medicion;
        public PlatoComidaData() {}
    }

    public static class RecipientesData {
        public RecipienteData agua;
        public RecipienteData comida;
        public RecipientesData() {}
    }

    public static class RecipienteData {
        public Float capacidad;
        public Float peso_actual;
        public Float temperatura; // Ojo: Solo presente en "agua" en tu JSON
        public Float humedad;     // Ojo: Solo presente en "comida" en tu JSON
        public Long ultima_medicion;
        public RecipienteData() {}
    }

    public static class HistorialAccion {
        public Boolean completado; // Usar Boolean object
        public String hora;
        public Float racion; // Usar Float object
        public Float sobras;
        public String tipo;
        public HistorialAccion() {}
    }

    public static class ComandosData {
        public UltimoComandoData ultimo_comando;
        public ComandosData() {}
    }

    public static class UltimoComandoData {
        public String accion_comida;
        public String accion_agua;
        public UltimoComandoData() {}
    }
}