package com.chriniko.reactive.playground.provider;

import com.chriniko.reactive.playground.domain.Employee;
import com.github.javafaker.Faker;
import io.reactivex.Observable;
import io.reactivex.Observer;

import java.util.UUID;
import java.util.stream.IntStream;


public class EmployeeGeneratorObservable extends Observable<Employee> {

    private static Faker FAKER = new Faker();

    private final int noOfRecords;

    public EmployeeGeneratorObservable(int noOfRecords) {
        this.noOfRecords = noOfRecords;
    }

    @Override
    protected void subscribeActual(Observer<? super Employee> observer) {
        try {
            IntStream.rangeClosed(1, noOfRecords)
                    .forEach(idx -> {
                        Employee employee = new Employee();

                        employee.setFirstname(FAKER.name().firstName());
                        employee.setInitials(FAKER.name().firstName());
                        employee.setSurname(FAKER.name().lastName());
                        employee.setId(UUID.randomUUID().toString());

                        observer.onNext(employee);
                    });

            observer.onComplete();
        } catch (Exception e) {
            observer.onError(e);
        }
    }
}
