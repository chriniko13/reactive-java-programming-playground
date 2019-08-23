package com.chriniko.reactive.playground;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Subjects
public class Chapter4 {


    public static void main(String[] args) throws Exception {

        //example1();

        System.out.println();

        //example2();

        System.out.println();

        //example3();

        System.out.println();

        example5();

        System.out.println();
    }

    private static void example1() throws Exception {

        PublishSubject<String> subject = PublishSubject.create();


        subject.subscribe(
                s -> {
                    System.out.println("onNext: " + s);
                },
                error -> {
                    System.err.println("onError: " + error);
                },
                () -> {
                    System.out.println("onComplete");
                },
                disposable -> {
                    System.out.println("onSubscribe");
                }
        );


        char info = 'a';
        for (int i = 1; i <= 10; i++) {
            subject.onNext(String.valueOf(info++));
        }
        subject.onComplete();


        TimeUnit.SECONDS.sleep(30);

    }

    private static void example2() throws Exception {


        Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS);

        Subject<Long> subject = PublishSubject.create();

        interval.subscribe(subject);

        subject.subscribe(new Observer<Long>() {
            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                System.out.println("first sequence completed");
            }

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Long item) {
                System.out.println("first sequence, item: " + item);
            }
        });

        subject.subscribe(new Observer<Long>() {
            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                System.out.println("second sequence completed");
            }

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Long item) {
                System.out.println("second sequence, item: " + item);
            }
        });

        TimeUnit.SECONDS.sleep(30);

    }

    private static void example3() throws Exception {

        //Subject<Integer> subject = PublishSubject.create();
        //Subject<Integer> subject = BehaviorSubject.createDefault(-100);
        //Subject<Integer> subject = ReplaySubject.create(10_000);
        Subject<Integer> subject = AsyncSubject.create();


        subject.subscribe(i -> System.out.println("1ST subscriber, i == " + i + "\n"));

        Random r = new Random();

        subject.onNext(r.nextInt());

        subject.subscribe(i -> System.out.println("2ND subscriber, i == " + i + "\n"));

        subject.onNext(r.nextInt());
        subject.onNext(r.nextInt());

        subject.onComplete();
    }

    private static void example5() throws Exception {


        ReactiveArrayList<Integer> l = new ReactiveArrayList<>();

        l.observeAdds().subscribe(elem -> System.out.println("just added: " + elem));
        l.observeRemoves().subscribe(elem -> System.out.println("just removed: " + elem));


        l.add(1);
        l.add(2);
        l.remove(new Integer(1));
        l.remove(new Integer(2));

        TimeUnit.SECONDS.sleep(30);
    }

    // -----------------------------------------------------------------------------------------------------------------

    static class ReactiveArrayList<T> extends ArrayList<T> {

        private Subject<T> addSubject = PublishSubject.create();
        private Subject<T> removeSubject = PublishSubject.create();

        @Override
        public boolean add(T t) {
            if (super.add(t)) {
                addSubject.onNext(t);
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (super.remove(o)) {
                removeSubject.onNext((T) o);
                return true;
            }
            return false;
        }

        public Observable<T> observeRemoves() {
            return removeSubject.hide();
        }

        public Observable<T> observeAdds() {
            return addSubject.hide();
        }
    }

}