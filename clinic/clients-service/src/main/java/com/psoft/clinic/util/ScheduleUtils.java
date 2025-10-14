package com.psoft.clinic.util;

import java.time.*;
import java.util.*;

public class ScheduleUtils {
    public static List<LocalDateTime> generateSlotsFromSchedule(String schedule, LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();

        // Exemplo para "Seg-Sex 08h-16h"
        if (schedule != null && schedule.matches(".*\\d{2}h-\\d{2}h.*")) {
            String[] partes = schedule.split(" ");
            String dias = partes[0]; // "Seg-Sex"
            String horas = partes[1]; // "08h-16h"

            String[] h = horas.split("-");
            int startHour = Integer.parseInt(h[0].replace("h", ""));
            int endHour = Integer.parseInt(h[1].replace("h", ""));

            DayOfWeek dow = date.getDayOfWeek();
            if (dias.equalsIgnoreCase("Seg-Sex") &&
                    dow.getValue() >= DayOfWeek.MONDAY.getValue() &&
                    dow.getValue() <= DayOfWeek.FRIDAY.getValue()) {

                for (int hour = startHour; hour < endHour; hour++) {
                    slots.add(date.atTime(hour, 0));
                    slots.add(date.atTime(hour, 30));
                }
            }
            // Podes adicionar mais lógica para outros tipos de schedule aqui!
        }
        return slots;

    }
}
