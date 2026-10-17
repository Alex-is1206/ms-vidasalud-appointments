package cl.duoc.vidasalud.ms_vidasalud_appointments;

import cl.duoc.vidasalud.ms_vidasalud_appointments.config.NombresRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NombresRabbit.class)
public class MsVidasaludAppointmentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsVidasaludAppointmentsApplication.class, args);
    }
}