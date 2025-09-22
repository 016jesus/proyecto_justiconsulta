package com.justiconsulta.store.repository;

import com.justiconsulta.store.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

}
