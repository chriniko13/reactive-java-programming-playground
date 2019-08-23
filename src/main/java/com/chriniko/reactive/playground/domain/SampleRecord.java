package com.chriniko.reactive.playground.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SampleRecord {

    @JsonProperty("_id")
    private String id;

    private int index;

    private String guid;

    @JsonProperty("isActive")
    private boolean active;

    private String balance;

    private String picture;

    private int age;

    private String eyeColor;

    private String name;

    private String gender;

    private String company;

    private String email;

    private String phone;

    private String address;

    private String about;

    private String registered;

    private double latitude;

    private double longitude;

    private List<String> tags;

    private List<Friend> friends;

    private String greeting;

    private String favoriteFruit;

    @Data
    @NoArgsConstructor
    public static class Friend {
        private int id;
        private String name;
    }

}
