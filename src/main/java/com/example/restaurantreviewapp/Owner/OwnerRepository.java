package com.example.restaurantreviewapp.Owner;

import com.example.restaurantreviewapp.Owner.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRepository extends JpaRepository<Owner,Long> {
}
