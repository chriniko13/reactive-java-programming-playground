package com.chriniko.reactive.playground;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.*;
import org.json.JSONObject;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// Networking with RxJava and Retrofit
public class Chapter5 {


    public static void main(String[] args) throws Exception {

        //example1();

        System.out.println();

        example2();

        System.out.println();

    }

    private static void example1() throws Exception {

        Undertow undertow = getUndertow();
        undertow.start();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:1234/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();


        EmployeesService employeesService = retrofit.create(EmployeesService.class);

        employeesService
                .listEmployees()
                .flatMap(employees -> {
                    System.out.println(Thread.currentThread().getName() + " --- flatMap");
                    return Observable.fromIterable(employees);
                })
                .filter(emp -> {
                    System.out.println(Thread.currentThread().getName() + " --- filter");
                    return Integer.parseInt(emp.getId()) >= 13000;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(
                        employee -> {
                            System.out.println(Thread.currentThread().getName() + " --- " + employee);
                        },
                        error -> System.err.println("error occurred: " + error.getMessage()),
                        () -> System.out.println("finished")
                );


        TimeUnit.SECONDS.sleep(30);
        undertow.stop();
    }

    private static void example2() throws Exception {

        Undertow undertow = getUndertow();
        undertow.start();


        CacheManager<Employee> employeeCacheManager = new CacheManager<Employee>() {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://localhost:1234/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(JacksonConverterFactory.create())
                    .build();

            EmployeesService employeesService = retrofit.create(EmployeesService.class);

            Path path = Paths.get("/home/chriniko/Desktop/emp_tmp");

            @Override
            boolean isValid(Employee data) {
                //Hint: make it to expire (based on a timestamp)
                return true;
            }

            @Override
            Maybe<Employee> doNetworkCall() {
                return employeesService.findById("1").toMaybe();
            }

            @Override
            void persistOnDisk(Employee response) {
                String info = response.getId()
                        + "|" + response.getName()
                        + "|" + response.getSalary()
                        + "|" + response.getAge()
                        + "|" + response.getProfileImage();

                try {
                    if (Files.exists(path)) {
                        Files.delete(path);
                        Files.createFile(path);
                    }

                    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path);) {
                        bufferedWriter.write(info);
                    }
                } catch (IOException e) {
                    System.err.println("persistOnDisk error occurred: " + e);
                }

            }

            @Override
            Employee readFromDisk() {
                if (Files.exists(path)) {
                    try {
                        String info = String.join("", Files.readAllLines(path));
                        String[] splittedInfo = info.split("\\|");
                        return new Employee(splittedInfo[0], splittedInfo[1], splittedInfo[2], splittedInfo[3], splittedInfo[4]);
                    } catch (IOException e) {
                        System.err.println("readFromDisk error occurred: " + e);
                        return null;
                    }
                }
                return null;
            }

            @Override
            void deleteFromDisk() {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    System.err.println("deleteFromDisk error occurred: " + e);
                }
            }
        };
        employeeCacheManager.deleteFromDisk();


        //1st case no data to cache, no data to disk, hit network
        employeeCacheManager.deleteFromDisk();
        System.out.println("first hit ---> no memo ---> no disk ---> will hit network");

        employeeCacheManager
                .getData()
                .subscribe(
                        employee -> System.out.println(employee),
                        error -> {
                            System.err.println("error: " + error);
                            error.printStackTrace(System.err);
                        }
                );

        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


        //2nd case data to cache, data to disk, no network | get data from cache
        System.out.println("second hit ---> memo ---> disk ---> will not hit network");

        employeeCacheManager
                .getData()
                .subscribe(
                        employee -> System.out.println(employee),
                        error -> {
                            System.err.println("error: " + error);
                            error.printStackTrace(System.err);
                        }
                );

        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


        //3rd case no data to cache, data to disk, not network | get data from disk
        employeeCacheManager.clearCache();
        System.out.println("third hit ---> no memo ---> disk ---> will not hit network");

        employeeCacheManager
                .getData()
                .subscribe(
                        employee -> System.out.println(employee),
                        error -> {
                            System.err.println("error: " + error);
                            error.printStackTrace(System.err);
                        }
                );

        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


        //4th case no data to cache, no data to disk, hit network
        employeeCacheManager.deleteAll();
        System.out.println("fourth hit ---> no memo ---> no disk ---> will hit network");

        employeeCacheManager
                .getData()
                .subscribe(
                        employee -> System.out.println(employee),
                        error -> {
                            System.err.println("error: " + error);
                            error.printStackTrace(System.err);
                        }
                );

        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        TimeUnit.SECONDS.sleep(30);
        undertow.stop();

    }


    interface EmployeesService {

        @GET("/employees")
        Observable<List<Employee>> listEmployees();

        @GET("/employees/{id}")
        Single<Employee> findById(@retrofit2.http.Path("id") String id);

    }

    @ToString
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class Employee {
        /*
        {
            "id": "12291",
            "employee_name": "acd-42069-0.144737139721536dsddssf",
            "employee_salary": "420",
            "employee_age": "69",
            "profile_image": ""
        }
         */

        private String id;

        @JsonProperty("employee_name")
        private String name;

        @JsonProperty("employee_salary")
        private String salary;

        @JsonProperty("employee_age")
        private String age;

        @JsonProperty("profile_image")
        private String profileImage;

    }


    static abstract class CacheManager<T> {

        private T cachedValue;

        // -------------------------------------------------------------------------------------------------------------

        public Maybe<T> getData() {
            return fromMemory()
                    .switchIfEmpty(fromDisk())
                    .switchIfEmpty(fromNetwork());
        }

        private Maybe<T> fromMemory() {
            return Maybe.create(emitter -> {
                if (cachedValue != null) {
                    emitter.onSuccess(cachedValue);
                }
                emitter.onComplete();
            });
        }

        protected Maybe<T> fromDisk() {
            return Maybe
                    .<T>create(emitter -> {
                        T t = readFromDisk();
                        if (t != null) {
                            emitter.onSuccess(t);
                            cacheInMemory(t);
                        }
                        emitter.onComplete();
                    })
                    .compose(logSource("disk"));
        }

        protected Maybe<T> fromNetwork() {
            return doNetworkCall()
                    .doOnSuccess(response -> {
                        cacheInMemory(response);
                        cacheOnDisk(response);
                    })
                    .compose(logSource("network"));
        }

        // -------------------------------------------------------------------------------------------------------------

        private void cacheInMemory(T t) {
            System.out.println("saving data in memory: " + t);
            cachedValue = t;
        }

        protected void cacheOnDisk(T response) {
            System.out.println("saving data on disk: " + response);
            persistOnDisk(response);
        }

        public void clearCache() {
            cachedValue = null;
        }

        public void deleteAll() {
            cachedValue = null;
            deleteFromDisk();
        }

        protected Maybe<T> fallbackToDiskCache() {
            System.out.println("fallback to disk cache");
            return fromDisk();
        }

        // -------------------------------------------------------------------------------------------------------------

        abstract boolean isValid(T data);

        abstract Maybe<T> doNetworkCall();

        abstract void persistOnDisk(T response);

        abstract T readFromDisk();

        abstract void deleteFromDisk();

        // -------------------------------------------------------------------------------------------------------------

        private MaybeTransformer<T, T> logSource(final String source) {
            return dataObservable -> dataObservable.doOnSuccess(data -> {
                if (data == null) {
                    System.out.println(source + " does not have any data.");
                } else if (!isValid(data)) {
                    System.out.println(source + " has stale data.");
                } else {
                    System.out.println(source + " has the data you are looking for!");
                }
            });
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static Undertow getUndertow() {
        return Undertow.builder()
                .addHttpListener(1234, "localhost")
                .setHandler(
                        Handlers.routing()
                                .get("/employees", new HttpHandler() {
                                    @Override
                                    public void handleRequest(HttpServerExchange exchange) throws Exception {

                                        // dispatch to non-io threads
                                        if (exchange.isInIoThread()) {
                                            exchange.dispatch(this);
                                            return;
                                        }

                                        // in worker thread
                                        StringBuilder sb = new StringBuilder();

                                        try (
                                                BufferedReader br =
                                                        new BufferedReader(
                                                                new InputStreamReader(
                                                                        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("employees.json"))))
                                        ) {
                                            String line;
                                            while ((line = br.readLine()) != null) {
                                                sb.append(line);
                                            }
                                        }
                                        exchange.getResponseSender().send(sb.toString());
                                    }
                                })
                                .get("/employees/{id}", new HttpHandler() {
                                    @Override
                                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                                        // dispatch to non-io threads
                                        if (exchange.isInIoThread()) {
                                            exchange.dispatch(this);
                                            return;
                                        }

                                        String empId = exchange.getQueryParameters().get("id").pollFirst();

                                        // in worker thread
                                        StringBuilder sb = new StringBuilder();

                                        try (
                                                BufferedReader br =
                                                        new BufferedReader(
                                                                new InputStreamReader(
                                                                        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("employee_by_id.json"))))
                                        ) {
                                            String line;
                                            while ((line = br.readLine()) != null) {
                                                sb.append(line);
                                            }
                                        }

                                        JSONObject jsonObject = new JSONObject(sb.toString());
                                        jsonObject.remove("id");
                                        jsonObject.put("id", empId);

                                        exchange.getResponseSender().send(jsonObject.toString());
                                    }
                                })
                )
                .build();
    }


}
