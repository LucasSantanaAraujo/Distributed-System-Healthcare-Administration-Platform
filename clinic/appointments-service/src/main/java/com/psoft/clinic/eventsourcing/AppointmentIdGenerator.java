package com.psoft.clinic.eventsourcing;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentIdGenerator {

    private final JdbcTemplate jdbcTemplate;

    public Long nextId() {
        return jdbcTemplate.queryForObject("select nextval('appointment_id_seq')", Long.class);
    }
}
