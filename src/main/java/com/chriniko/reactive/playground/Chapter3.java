package com.chriniko.reactive.playground;

/*
    Note:

 There are some exceptions that are not handled as error events but they are thrown,
 causing JVM to stop:
    • rx.exceptions.OnErrorNotImplementedException
    • rx.exceptions.OnErrorFailedException
    • rx.exceptions.OnCompletedFailedException
    • java.lang.StackOverflowError
    • java.lang.VirtualMachineError
    • java.lang.ThreadDeath
    • java.lang.LinkageError
 */

import com.chriniko.reactive.playground.domain.Employee;
import com.chriniko.reactive.playground.provider.EmployeeGeneratorObservable;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.reactivex.Observable;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;


// Subscription Lifecycle
public class Chapter3 {


    public static void main(String[] args) throws Exception {

        //example1();

        System.out.println();

        //example2();

        System.out.println();

        //example3();

        System.out.println();

        example4();

        System.out.println();

        //example5();

        System.out.println();

        //example6();

        System.out.println();

        //example7();

        System.out.println();

        //example7b();
    }

    private static void example1() throws Exception {

        sampleObservable()

                /*.onErrorResumeNext(throwable -> {
                    System.out.println(Thread.currentThread().getName() + " --- onErrorResumeNext, error message: " + throwable.getMessage());
                    return Observable.empty();
                })*/

                /*.onErrorReturn(error -> {
                    System.out.println(Thread.currentThread().getName() + " --- onErrorReturn, error message: " + error.getMessage());
                    return -9L;
                })*/

                .onExceptionResumeNext(Observable.rangeLong(200, 6))

                .subscribe(
                        elem -> {
                            System.out.println(Thread.currentThread().getName() + " --- next elem is: " + elem);
                        },
                        error -> {
                            System.err.println(Thread.currentThread().getName() + " --- error occurred: " + error.getMessage());
                        },
                        () -> {
                            System.out.println(Thread.currentThread().getName() + " --- processing finished");
                        }
                );


        TimeUnit.SECONDS.sleep(15);
    }

    private static void example2() throws Exception {

        ServerRuntime serverRuntime = ServerRuntime.builder().addConfig("cayenne-project.xml").build();
        ObjectContext objectContext = serverRuntime.newContext();

        List<Employee> storedEmployees = ObjectSelect.query(Employee.class).select(objectContext);
        objectContext.deleteObjects(storedEmployees);
        objectContext.commitChanges();

        new EmployeeGeneratorObservable(20)
                .buffer(5)
                .subscribeOn(Schedulers.io())
                .subscribe(employees -> {
                    for (Employee employee : employees) {
                        System.out.println("will store employee: " + employee);
                        objectContext.registerNewObject(employee);
                    }

                    objectContext.commitChanges();
                });

        TimeUnit.SECONDS.sleep(30);
    }

    private static void example3() throws Exception {

        Observable<Integer> intObservable = Observable.create(emitter -> {

            IntStream.rangeClosed(1, 10)
                    .forEach(idx -> {

                        if (new Random().nextInt(2) == 1) {
                            throw new IllegalStateException();
                        }

                        emitter.onNext(idx);

                    });

            emitter.onComplete();
        });


        intObservable

                /*.retry(2, error -> {
                    System.out.println("inside retry, error: " + error);
                    return true;
                })*/

                /*.retryWhen(error -> {
                    System.out.println("inside retry, error: " + error);
                    return Observable.timer(5, TimeUnit.SECONDS);
                })*/

                .retryWhen(error -> {

                    return error
                            .zipWith(
                                    Observable.range(1, 3),
                                    (throwable, retryCount) -> {
                                        System.out.println("throwable is: " + throwable + ", retryCount is: " + retryCount);
                                        return retryCount;
                                    })
                            .flatMap(idx -> Observable.timer(3, TimeUnit.SECONDS));

                })

                .subscribe(
                        elem -> System.out.println("elem is: " + elem),
                        error -> System.err.println("error is: " + error)
                );

        TimeUnit.SECONDS.sleep(30);
    }

    private static void example4() throws Exception {

        Undertow undertow = getUndertow();
        undertow.start();

        final List<String> ids = getIds();
        System.out.println("ids.size() == " + ids.size());

        Observable
                .<JSONObject>create(emitter -> {
                    for (String id : ids) {

                        String url = "http://localhost:1234/api/v1/employee/" + id;

                        JsonNode node = Unirest
                                .get(url)
                                .asJson()
                                .getBody();

                        emitter.onNext(node.getObject());
                    }

                    emitter.onComplete();
                })

                .compose(mySchedulers())

                //.subscribeOn(Schedulers.io())
                //.observeOn(Schedulers.computation())


                .map(json -> {
                    System.out.println(Thread.currentThread().getName() + " --- inside map");
                    return json.getString("employee_name");
                })
                .observeOn(Schedulers.io())
                .subscribe(name -> System.out.println(Thread.currentThread().getName() + " --- employee name is: " + name));

        TimeUnit.SECONDS.sleep(30);
        undertow.stop();
    }

    private static void example5() throws Exception {

        Scheduler.Worker worker = Schedulers.newThread().createWorker();


        final LongAdder longAdder = new LongAdder();

        Disposable disposable = worker.schedulePeriodically(
                () -> {
                    longAdder.increment();
                    System.out.println("will execute task with number: " + longAdder.sum());

                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException ignored) {
                    }

                },
                1, 3, TimeUnit.SECONDS);


        TimeUnit.SECONDS.sleep(30);
        disposable.dispose();

    }

    // Note: handling backpressure during emission: throttling technique
    private static void example6() throws Exception {


        Flowable<Long> fastObservable = Flowable
                .interval(0, 1, TimeUnit.NANOSECONDS);

        //.sample(1, TimeUnit.MILLISECONDS); // Note: comment/uncomment to see what happens.

        //.throttleFirst(1, TimeUnit.SECONDS); // Note: comment/uncomment to see what happens.

        //.debounce(1, TimeUnit.SECONDS); // Note: comment/uncomment to see what happens.

        //.onBackpressureBuffer(); // Note: comment/uncomment to see what happens.

        FlowableSubscriber<Long> slowObserver = new FlowableSubscriber<Long>() {

            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Long elem) {
                System.out.println("received elem is: " + elem + " will do some heavy calculations...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                s.request(1);
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("error: " + e);
            }

            @Override
            public void onComplete() {
                System.out.println("completed");
            }
        };

        fastObservable.subscribeOn(Schedulers.computation()).subscribe(slowObserver);

        TimeUnit.SECONDS.sleep(30);
    }

    // Note: handling backpressure during emission: buffering technique
    private static void example7() throws Exception {


        Flowable<List<Long>> fastObservable = Flowable
                .interval(0, 1, TimeUnit.NANOSECONDS)
                .buffer(1000); // Note: comment/uncomment to see what happens.


        FlowableSubscriber<List<Long>> slowObserver = new FlowableSubscriber<List<Long>>() {

            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(List<Long> elems) {
                for (Long elem : elems) {
                    System.out.println("received elem is: " + elem + " will do some heavy calculations...");
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("error: " + e);
            }

            @Override
            public void onComplete() {
                System.out.println("completed");
            }
        };


        fastObservable
                .subscribeOn(Schedulers.computation())
                .subscribe(slowObserver);


        TimeUnit.SECONDS.sleep(30);
    }

    private static void example7b() throws Exception {

        Flowable
                .interval(0, 1, TimeUnit.NANOSECONDS)
                .window(2, TimeUnit.SECONDS)
                .map(
                        longFlowable -> longFlowable
                                .subscribeOn(Schedulers.computation())
                                .subscribe(aLong -> System.out.println("aLong: " + aLong))
                )
                .subscribeOn(Schedulers.computation())
                .subscribe();


        TimeUnit.SECONDS.sleep(30);
    }

    // --- utils ---
    static class ProcessingException extends RuntimeException {
        ProcessingException(String message) {
            super(message);
        }
    }

    private static Observable<Long> sampleObservable() {
        int count = 5;
        final Random random = new Random();

        return Observable
                .intervalRange(1, count, 1, 2, TimeUnit.SECONDS)
                .map(idx -> {
                    int randomNum = random.nextInt(2);
                    if (randomNum == 1) {
                        throw new ProcessingException("could not process elem");
                    }
                    return idx;
                })
                .subscribeOn(Schedulers.computation());
    }

    private static List<String> getIds() throws UnirestException {

        HttpResponse<JsonNode> httpResponse = Unirest.get("http://localhost:1234/api/v1/employees")
                .asJson();

        System.out.println("status >>> " + httpResponse.getStatusText());

        JsonNode body = httpResponse.getBody();

        final List<String> ids = new LinkedList<>();

        JSONArray employees = body.getArray();
        Iterator<Object> iterator = employees.iterator();

        while (iterator.hasNext()) {

            Object next = iterator.next();
            JSONObject.testValidity(next);

            JSONObject jsonObject = (JSONObject) next;

            String id = jsonObject.getString("id");
            ids.add(id);
        }
        return ids;
    }

    private static <T> ObservableTransformer<T, T> mySchedulers() {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation());
            }
        };

    }

    private static class HelloHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {

            // dispatch to non-io threads
            if (exchange.isInIoThread()) {
                exchange.dispatch(this);
                return;
            }

            // in worker thread
            exchange.getResponseSender().send("Hello there!");
        }
    }

    private static Undertow getUndertow() {
        return Undertow.builder()
                .addHttpListener(1234, "localhost")
                .setHandler(
                        Handlers.routing()
                                .get("/hello", new HelloHandler())
                                .get("/api/v1/employees", new HttpHandler() {
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
                                .get("/api/v1/employee/{id}", new HttpHandler() {
                                    @Override
                                    public void handleRequest(HttpServerExchange exchange) throws Exception {

                                        String employeeId = exchange.getQueryParameters().get("id").pollFirst();

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
                                                                        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("employee_by_id.json"))))
                                        ) {
                                            String line;
                                            while ((line = br.readLine()) != null) {
                                                sb.append(line);
                                            }
                                        }

                                        JSONObject jsonObject = new JSONObject(sb.toString());
                                        jsonObject.remove("id");
                                        jsonObject.put("id", employeeId);

                                        exchange.getResponseSender().send(jsonObject.toString());
                                    }
                                })
                )
                .build();
    }

}
