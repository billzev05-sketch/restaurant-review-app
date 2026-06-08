package com.example.restaurantreviewapp;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "owners")
// Λόγω κληρονομικότητας όλοι οι users θεωρητικά τοποθετούνται στον ίδιο table με διαφορετικό DTYPE
//To Table annotation χρησιμοποιείται μόνο σε περίπτωση αλλαγής της δομής της βάσης
public class Owner extends User {

    private int totalRestaurants;

    public Owner() {}

    public Owner(String username, String password, String firstName, String lastName, int totalRestaurants) {
        super(username, password, firstName, lastName);
        this.totalRestaurants = totalRestaurants;
    }


}
