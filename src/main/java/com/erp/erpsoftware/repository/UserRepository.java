package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

}

