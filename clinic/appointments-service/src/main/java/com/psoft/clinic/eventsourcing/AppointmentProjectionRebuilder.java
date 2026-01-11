package com.psoft.clinic.eventsourcing;

import com.psoft.clinic.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentProjectionRebuilder {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentEventRepository eventRepository;
    private final AppointmentProjector projector;

    @Transactional
    public int rebuildAll() {
        // 1) limpar projeção
        appointmentRepository.deleteAll();

        // 2) ler eventos por ordem (aggregateId, version)
        List<AppointmentEvent> events = eventRepository.findAllOrdered();

        // 3) projetar
        int projected = 0;
        for (AppointmentEvent e : events) {
            projector.project(e);
            projected++;
        }

        return projected;
    }
}
