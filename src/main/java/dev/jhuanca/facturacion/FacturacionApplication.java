package dev.jhuanca.facturacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "dev.jhuanca.facturacion",
    "io.github.project.openubl.spring.xsender.runtime"
})
public class FacturacionApplication {

	public static void main(String[] args) {
		SpringApplication.run(FacturacionApplication.class, args);
                
		
	}

}
