package com.psoft.clinic.eventsourcing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppointmentEventRepository extends JpaRepository<AppointmentEvent, Long> {

    List<AppointmentEvent> findByAggregateIdOrderByVersionAsc(Long aggregateId);

    Optional<AppointmentEvent> findTopByAggregateIdOrderByVersionDesc(Long aggregateId);

    @Query("select e from AppointmentEvent e order by e.aggregateId asc, e.version asc")
    List<AppointmentEvent> findAllOrdered();
}
