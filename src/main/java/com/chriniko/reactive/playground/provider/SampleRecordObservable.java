package com.chriniko.reactive.playground.provider;

import com.chriniko.reactive.playground.Chapter1;
import com.chriniko.reactive.playground.domain.SampleRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.Observer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class SampleRecordObservable extends Observable<SampleRecord> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void subscribeActual(Observer<? super SampleRecord> observer) {

        try {

            URI uri = Chapter1.class.getClassLoader().getResource("sample_data.json").toURI();
            Path path = Paths.get(uri);
            String sampleDataAsString = Files.lines(path).collect(Collectors.joining());

            List<JsonNode> sampleData = MAPPER.readValue(sampleDataAsString, new TypeReference<List<JsonNode>>() {
            });

            for (JsonNode sampleDatum : sampleData) {

                String sampleDatumAsString = MAPPER.writeValueAsString(sampleDatum);
                SampleRecord sampleRecord = MAPPER.readValue(sampleDatumAsString, SampleRecord.class);

                observer.onNext(sampleRecord);
            }
            observer.onComplete();
        } catch (Exception error) {
            observer.onError(error);
        }

    }
}
