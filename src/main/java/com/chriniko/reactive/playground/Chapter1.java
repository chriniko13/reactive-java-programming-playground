package com.chriniko.reactive.playground;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// ReactiveX and RxJava
public class Chapter1 {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Faker FAKER = new Faker();

    public static void main(String[] args) throws Exception {

        //example1();

        System.out.println();

        example2();

    }

    private static void example1() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        Observable<Integer> oddNumbers = Observable
                .fromIterable(input)
                .filter(n -> {
                    System.out.println(Thread.currentThread().getName() + " --- will filter number: " + n);
                    return n % 2 == 0;
                })
                .observeOn(Schedulers.io())
                .map(oddNumber -> {

                    URI uri = Chapter1.class.getClassLoader().getResource("sample_data.json").toURI();
                    Path path = Paths.get(uri);
                    String sampleDataAsString = Files.lines(path).collect(Collectors.joining());

                    List<JsonNode> sampleData = MAPPER.readValue(sampleDataAsString, new TypeReference<List<JsonNode>>() {
                    });

                    JsonNode sampleDatum = sampleData.get(oddNumber);

                    System.out.println(Thread.currentThread().getName() + " --- sampleDatum: " + sampleDatum);

                    return oddNumber;
                });

        Disposable disposable = oddNumbers
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        num -> System.out.println(Thread.currentThread().getName() + " --- next number is: " + num),
                        error -> System.err.println(Thread.currentThread().getName() + " --- error occurred: " + error.getMessage()),
                        () -> {
                            System.out.println(Thread.currentThread().getName() + " --- stream finished");
                            latch.countDown();
                        }
                );

        latch.await();
        System.out.println("is disposed: " + disposable.isDisposed());
    }

    private static void example2() throws Exception {

        ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
        LocalStorage localStorage = new LocalStorage();

        final int count = 4;
        CountDownLatch latch = new CountDownLatch(count);

        Observable
                .intervalRange(1, count, 0, 2, TimeUnit.SECONDS)
                .flatMap(tick -> {
                    System.out.println();
                    return serviceEndpoint.login();
                })
                .flatMap(accessToken -> localStorage.storeCredentials(accessToken))
                .flatMap(token -> serviceEndpoint.getUser(token.getValue()))
                .flatMap(user -> serviceEndpoint.getUserContact(user.getId()))
                .doOnError(throwable -> System.out.println("error occurred: " + throwable.getMessage()))
                .forEach(userContact -> {
                    System.out.println(Thread.currentThread().getName() + " --- user contact: " + userContact);
                    latch.countDown();
                });

        latch.await();

    }


    // --- utils ---

    static class ServiceEndpoint {

        Observable<AccessToken> login() {
            return Observable.create(emitter -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " --- performing login...");
                    randomWait();
                    emitter.onNext(new AccessToken(UUID.randomUUID().toString()));
                    emitter.onComplete();
                } catch (Exception error) {
                    System.out.println(Thread.currentThread().getName() + " --- error occurred during login operation, error: " + error.getMessage());
                    emitter.onError(error);
                }
            });
        }

        Observable<User> getUser(String id) {
            return Observable.fromCallable(() -> {
                randomWait();
                return new User(id, FAKER.name().fullName());
            });
        }

        Observable<UserContact> getUserContact(String id) {
            return Observable.create(emitter -> {
                randomWait();
                emitter.onNext(new UserContact(FAKER.address().city(), FAKER.address().streetAddress()));
                emitter.onComplete();
            });
        }
    }

    static class LocalStorage {

        private final Queue<AccessToken> credentialsStored = new LinkedBlockingQueue<>();

        Observable<AccessToken> storeCredentials(AccessToken token) {
            return Observable.create(emitter -> {
                credentialsStored.add(token);
                emitter.onNext(token);
                emitter.onComplete();
            });
        }

    }

    private static void randomWait() {
        try {
            Thread.sleep((new Random().nextInt(5) + 1) * 100);
        } catch (InterruptedException ignored) {
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class AccessToken {
        private final String value;
        private final Instant created = Instant.now();
    }

    @RequiredArgsConstructor
    @Data
    static class User {
        private final String id;
        private final String name;
    }

    @RequiredArgsConstructor
    @Data
    static class UserContact {
        private final String city;
        private final String address;
    }
}
