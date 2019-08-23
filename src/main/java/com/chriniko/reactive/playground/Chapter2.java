package com.chriniko.reactive.playground;


import com.chriniko.reactive.playground.domain.SampleRecord;
import com.chriniko.reactive.playground.provider.SampleRecordObservable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// Observables and Observers
public class Chapter2 {


    public static void main(String[] args) throws Exception {

        //example1();

        System.out.println();

        //example2();

        System.out.println();

        //example3();

        System.out.println();

        //example4();

        System.out.println();

        //example5();

        System.out.println();

        example6();
    }

    private static void example1() {

        // Note: cold observable.
        Observable<Integer> observable = Observable.range(1, 10);
        Disposable disposable = subscribeToObservablePrinter(observable);
        System.out.println("is disposed: " + disposable.isDisposed());

    }

    private static void example2() throws Exception {

        Observable.just("click-tracking-event")
                .subscribe(System.out::println);

        System.out.println();

        CountDownLatch latch1 = new CountDownLatch(3);
        Observable.interval(0, 2, TimeUnit.SECONDS)
                .map(tick -> tick * 10)
                .subscribe(x -> {
                    System.out.println(x);
                    latch1.countDown();
                });
        latch1.await();

        System.out.println();

        CountDownLatch latch2 = new CountDownLatch(1);
        Observable.timer(2, TimeUnit.SECONDS)
                .subscribe(x -> {
                    System.out.println(x);
                    latch2.countDown();
                });
        latch2.await();

        System.out.println();

        PersonRepository personRepository = new PersonRepository(false);
        personRepository.find("1234")
                .subscribe(record -> System.out.println(record));

        System.out.println();

        personRepository = new PersonRepository(true);
        personRepository.find("1234")
                .subscribe(
                        record -> System.out.println(record),
                        error -> System.err.println(error)
                );

        System.out.println();
        Observable<Object> never = Observable.never();
        never.subscribe(System.out::println);


        Thread.sleep(5000);
    }

    private static void example3() {


        Dummy dummy = new Dummy();

        // Note: comment/uncomment below to test defer behaviour (lazy evaluation)
        //Observable<String> stringObservable = Observable.just(dummy.getText());
        Observable<String> stringObservable = Observable.defer(() -> Observable.just(dummy.getText()));

        // Note: comment/uncomment below to test defer behaviour (lazy evaluation)
        //Observable<Integer> integerObservable = Observable.just(dummy.getNumber());
        Observable<Integer> integerObservable = Observable.defer(() -> Observable.just(dummy.getNumber()));

        dummy.setText("abc");
        dummy.setNumber(123);

        stringObservable.subscribe(i -> System.out.println("string value is: " + i));
        integerObservable.subscribe(i -> System.out.println("int value is: " + i));


    }

    private static void example4() {

        SampleRecordObservable sampleRecordObservable = new SampleRecordObservable();

        sampleRecordObservable
                .buffer(10)
                .flatMap(sampleRecords -> Observable.fromIterable(sampleRecords))
                .filter(r -> !r.getFriends().isEmpty())
                .map(r -> r.getName().toUpperCase())
                .flatMap(n -> howManyParts(n))
                //.concatMap(n -> howManyParts(n))
                .subscribe(n -> System.out.println("name is: " + n));


        System.out.println();

        Single<List<SampleRecord>> collect = sampleRecordObservable
                .collect(
                        () -> new LinkedList<SampleRecord>(),
                        (sampleRecords, item) -> sampleRecords.add(item)
                );

        List<SampleRecord> sampleRecords = collect.blockingGet();
        System.out.println("sampleRecords.size() == " + sampleRecords.size());


        System.out.println();


        Observable
                .zip(
                        Observable.range(1, 4),
                        Observable.fromArray("a", "b", "c", "d"),
                        (integer, character) -> Tuple.of(integer, character)
                )
                .subscribe(s -> System.out.println(s));


        System.out.println();


        Observable
                .concat(Observable.just("a"), Observable.just("b"))
                .subscribe(s -> System.out.println(s));


        System.out.println();


        Observable.fromIterable(
                Arrays.asList(
                        new Name("name1"),
                        new Name("name1"),
                        new Name("name2")))
                .distinct()
                .subscribe(s -> System.out.println(s));

    }

    private static void example5() {

        incremental(5)
                .firstElement()
                .subscribe(elem -> System.out.println(elem));

        System.out.println();

        incremental(5)
                .lastElement()
                .subscribe(elem -> System.out.println(elem));

        System.out.println();

        incremental(5)
                .take(2)
                .subscribe(elem -> System.out.println(elem));

        System.out.println();

        incremental(5)
                .startWith(0)
                .subscribe(elem -> System.out.println(elem));

        System.out.println();

        incremental(5)
                .scan(0, Integer::sum)
                .subscribe(elem -> System.out.println(elem));

        System.out.println();

        incremental(5)
                .elementAt(4)
                .subscribe(elem -> System.out.println(elem));

    }

    private static void example6() {

        int fibNumber = 26;

        final int fib[] = {0, 1};

        Observable<Integer> fibObservable = Observable
                .range(1, fibNumber)
                .map(idx -> {
                    if (idx <= 1) {
                        return fib[idx];
                    } else {
                        int result = fib[0] + fib[1];
                        fib[0] = fib[1];
                        fib[1] = result;
                        return result;
                    }

                });

        fibObservable.subscribe(n -> System.out.println(n));
    }


    // --- utils ---

    private static Observable<Integer> incremental(int howMany) {
        return Observable.range(1, howMany);
    }

    private static Observable<Integer> random(int howMany) {
        SecureRandom secureRandom = new SecureRandom();

        return Observable
                .range(1, howMany)
                .map(idx -> secureRandom.nextInt(50) + 1);
    }

    static class PersonRepository {

        private final boolean failIfRecordNotExists;

        public Map<String, Person> people = new ConcurrentHashMap<>();

        PersonRepository(boolean failIfRecordNotExists) {
            this.failIfRecordNotExists = failIfRecordNotExists;
        }

        public Observable<Person> find(String id) {
            Person person = people.get(id);
            return person != null
                    ? Observable.just(person)
                    : failIfRecordNotExists
                    ? Observable.error(new RecordNotFoundException("record does not exist for id: " + id))
                    : Observable.empty();
        }
    }

    static class RecordNotFoundException extends RuntimeException {
        public RecordNotFoundException(String message) {
            super(message);
        }
    }

    private static Observable<Tuple2<String, Integer>> howManyParts(String name) {

        return Observable.create(emitter -> {
            String[] splitted = name.split(" ");
            emitter.onNext(Tuple.of(name, splitted.length));
            emitter.onComplete();
        });

    }

    @RequiredArgsConstructor
    @Data
    static class Name {
        private final String value;
    }

    @RequiredArgsConstructor
    @Data
    static class Person {
        private final String id;
        private final String name;
    }

    @Data
    static class Dummy {
        private String text;
        private int number;

        public String getText() {
            return Optional.ofNullable(text).orElse("NO_VALUE");
        }
    }

    private static <T> Disposable subscribeToObservablePrinter(Observable<T> observable) {
        return subscribeToObservable(
                observable,
                t -> System.out.println("next item is: " + t),
                throwable -> {
                },
                aVoid -> {
                }
        );
    }

    private static <T> Disposable subscribeToObservable(Observable<T> observable,
                                                        Consumer<T> itemConsumption,
                                                        Consumer<Throwable> errorConsumption,
                                                        Consumer<Void> completionConsumption) {
        return observable.subscribe(
                t -> itemConsumption.accept(t),
                e -> errorConsumption.accept(e),
                () -> completionConsumption.accept(null)
        );
    }

}
