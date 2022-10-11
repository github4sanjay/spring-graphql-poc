package com.example.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@SpringBootApplication
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer(CrmService crmService) {
        return builder -> {

            builder.type("Customer", wiring -> wiring
                    .dataFetcher("profile", environment -> crmService.getProfile(environment.getSource())));

            builder.type("Query", wiring -> wiring
                    .dataFetcher("customerById", environment -> crmService.getCustomerById(Integer.parseInt(environment.getArgument("id"))))
                    .dataFetcher("customers", environment -> crmService.getCustomers()));
        };
    }

}

record Customer(Integer id, String name) {
}

record Profile(Integer id, Integer customerId) {
}

@Service
class CrmService {

    Profile getProfile(Customer customer) {
        return new Profile(customer.id(), customer.id());
    }

    Collection<Customer> getCustomers() {
        return List.of(new Customer(1, "San"),
                new Customer(2, "Jan"),
                new Customer(3, "Ran")
        );
    }

    public Customer getCustomerById(Integer id) {
        return new Customer(id, "San");
    }
}
