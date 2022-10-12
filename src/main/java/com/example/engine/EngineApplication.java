package com.example.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }

    @Bean
    HttpGraphQlClient httpGraphQlClient() {
        return HttpGraphQlClient.builder().url("https://countries.trevorblades.com/").build();
    }
}

record Customer(Integer id, String name) {
}

record Profile(Integer id, Integer customerId) {
}

record Country(String code, String capital, String name) {
}

@Slf4j
@Controller
@RequiredArgsConstructor
class CountryController {

    private final HttpGraphQlClient httpGraphQlClient;

    @QueryMapping
    Mono<List<Country>> countries() {
        var httpRequestDocument = """
                query {
                  countries {
                    code
                    capital
                    name
                  }
                }
                """;
        return httpGraphQlClient.document(httpRequestDocument)
                .retrieve("countries")
                .toEntityList(Country.class);
    }

    @QueryMapping
    public Mono<Country> countryByCode(@Argument String code) {
        var httpRequestDocument = """
                query {
                    country(code: "%s") {
                      code
                      name
                    }
                  }
                """.formatted(code);
        return httpGraphQlClient.document(httpRequestDocument)
                .variable("code", code)
                .retrieve("country")
                .toEntity(Country.class);
    }
}

@Slf4j
@Controller
@RequiredArgsConstructor
class GreetingsController {

    private final CrmService crmService;

    @QueryMapping
    // @SchemaMapping(typeName = "Query", field = "hello")
    public String hello() {
        return "Hello world!";
    }

    @QueryMapping
    public String helloWithName(@Argument String name) {
        return "Hello " + name + "!";
    }

    @QueryMapping
    public Customer customerById(@Argument Integer id) {
        return crmService.getCustomerById(id);
    }

    @QueryMapping
    public Flux<Customer> customers() {
        return Flux.fromIterable(crmService.getCustomers());
    }

    @BatchMapping
    public Map<Customer, Profile> profile(List<Customer> customers) {
        log.info("get profile for {}", customers);
        return customers.stream()
                .collect(Collectors.toMap(Function.identity(), crmService::getProfile));
    }

    /**
     * This has disadvantage of calling for each customer separately
     *
     * @param customer
     * @return Profile
     */
    // @SchemaMapping(typeName = "Customer")
    public Profile profile(Customer customer) {
        return crmService.getProfile(customer);
    }

    @MutationMapping
    Customer addCustomer(@Argument String name) {
        return crmService.addCustomer(name);
    }
}

@Service
class CrmService {

    private final Map<Integer, Customer> db = new ConcurrentHashMap<>();
    private final AtomicInteger id = new AtomicInteger();

    Customer addCustomer(String name) {
        var currentId = id.incrementAndGet();
        var customer = new Customer(currentId, name);
        db.put(currentId, customer);
        return customer;
    }

    Profile getProfile(Customer customer) {
        return new Profile(customer.id(), customer.id());
    }

    Collection<Customer> getCustomers() {
        return db.values();
    }

    public Customer getCustomerById(Integer id) {
        return db.get(id);
    }
}
