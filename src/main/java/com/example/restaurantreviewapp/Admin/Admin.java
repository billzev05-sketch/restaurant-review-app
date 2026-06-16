package com.example.restaurantreviewapp.Admin;

import com.example.restaurantreviewapp.User.User;
import jakarta.persistence.Entity;

@Entity
public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(String username, String password, String firstName, String lastName) {
        super(username, password, firstName, lastName);
    }
}