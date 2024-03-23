package com.jvnyor.cryptographychallenge.entities;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userDocument;

    @Column(nullable = false)
    private String creditCardToken;

    @Column(nullable = false)
    private double value;

    public Transaction() {
    }

    public Transaction(Long id, String userDocument, String creditCardToken, double value) {
        this.id = id;
        this.userDocument = userDocument;
        this.creditCardToken = creditCardToken;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getUserDocument() {
        return userDocument;
    }

    public void setUserDocument(String userDocument) {
        this.userDocument = userDocument;
    }

    public String getCreditCardToken() {
        return creditCardToken;
    }

    public void setCreditCardToken(String creditCardToken) {
        this.creditCardToken = creditCardToken;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userDocument='" + userDocument + '\'' +
                ", creditCardToken='" + creditCardToken + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
