package com.example.restaurantreviewapp.Critic;

import com.example.restaurantreviewapp.User.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
// Λόγω κληρονομικότητας όλοι οι users θεωρητικά τοποθετούνται στον ίδιο table με διαφορετικό DTYPE
//To Table annotation χρησιμοποιείται μόνο σε περίπτωση αλλαγής της δομής της βάσης
@Table(name = "critics")
public class Critic extends User {

    private int totalReviews;

    public Critic() {}

    public Critic(String username, String password, String firstName, String lastName, int totalReviews) {
        super(username, password, firstName, lastName);
        this.totalReviews = totalReviews;
    }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
}
