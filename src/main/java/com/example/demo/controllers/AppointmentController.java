package com.example.demo.controllers;

import com.example.demo.entities.Appointment;
import com.example.demo.repositories.AppointmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getAllAppointments(){
        List<Appointment> appointments = new ArrayList<>();

        appointmentRepository.findAll().forEach(appointments::add);

        if (appointments.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(appointments, HttpStatus.OK);
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable("id") long id){
        Optional<Appointment> appointment = appointmentRepository.findById(id);

        if (appointment.isPresent()){
            return new ResponseEntity<>(appointment.get(),HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

@PostMapping("/appointment")
public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment){
    // Verifica si la cita recibida es nula
    if (appointment == null) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    //lógica de validación:
    
    // Validación 1: Verificar si la fecha y hora de inicio están en el futuro
    LocalDateTime now = LocalDateTime.now();
    if (appointment.getStartsAt().isBefore(now)) {
        return new ResponseEntity<>("La fecha y hora de inicio deben ser futuras", HttpStatus.BAD_REQUEST);
    }

    // Validación 2: Verificar si la fecha y hora de fin son posteriores a la fecha y hora de inicio
    if (appointment.getFinishesAt().isBefore(appointment.getStartsAt())) {
        return new ResponseEntity<>("La fecha y hora de fin deben ser posteriores a la fecha y hora de inicio", HttpStatus.BAD_REQUEST);
    }
    // Validación 3: Verificar si el paciente está disponible
    if (!isPatientAvailable(appointment.getPatient(), appointment.getStartsAt(), appointment.getFinishesAt())) {
        return new ResponseEntity<>("El paciente no está disponible en ese momento", HttpStatus.BAD_REQUEST);
    }
    // Validación 4: Verificar si el médico está disponible
    if (!isDoctorAvailable(appointment.getDoctor(), appointment.getStartsAt(), appointment.getFinishesAt())) {
        return new ResponseEntity<>("El médico no está disponible en ese momento", HttpStatus.BAD_REQUEST);
    }
    // Validación 5: Verificar si la sala está disponible
    if (!isRoomAvailable(appointment.getRoom(), appointment.getStartsAt(), appointment.getFinishesAt())) {
        return new ResponseEntity<>("La sala no está disponible en ese momento", HttpStatus.BAD_REQUEST);
    }

    // Guarda la cita en la base de datos
    try {
        Appointment _appointment = appointmentRepository.save(appointment);
        return new ResponseEntity<>(_appointment, HttpStatus.CREATED);
    } catch (Exception e) {
        // Si hay un error al guardar la cita, devuelve un error 500 Internal Server Error
        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

    
// Método para verificar la disponibilidad del paciente en el momento dado
private boolean isPatientAvailable(Patient patient, LocalDateTime startsAt, LocalDateTime finishesAt) {
    // Consultar la base de datos para ver si el paciente tiene otras citas en ese momento
    List<Appointment> patientAppointments = appointmentRepository.findByPatientAndTimeRange(patient, startsAt, finishesAt);
    return patientAppointments.isEmpty();
}

// Método para verificar la disponibilidad del médico en el momento dado
private boolean isDoctorAvailable(Doctor doctor, LocalDateTime startsAt, LocalDateTime finishesAt) {
    // Consultar la base de datos para ver si el médico tiene otras citas en ese momento
    List<Appointment> doctorAppointments = appointmentRepository.findByDoctorAndTimeRange(doctor, startsAt, finishesAt);
    return doctorAppointments.isEmpty();
}

// Método para verificar la disponibilidad de la sala en el momento dado
private boolean isRoomAvailable(Room room, LocalDateTime startsAt, LocalDateTime finishesAt) {
    // Consultar la base de datos para ver si la sala está reservada para otra cita en ese momento
    List<Appointment> roomAppointments = appointmentRepository.findByRoomAndTimeRange(room, startsAt, finishesAt);
    return roomAppointments.isEmpty();
}


    
    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<HttpStatus> deleteAppointment(@PathVariable("id") long id){

        Optional<Appointment> appointment = appointmentRepository.findById(id);

        if (!appointment.isPresent()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        appointmentRepository.deleteById(id);

        return new ResponseEntity<>(HttpStatus.OK);
        
    }

    @DeleteMapping("/appointments")
    public ResponseEntity<HttpStatus> deleteAllAppointments(){
        appointmentRepository.deleteAll();
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
