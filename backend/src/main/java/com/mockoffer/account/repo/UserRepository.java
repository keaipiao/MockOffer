package com.mockoffer.account.repo;

import com.mockoffer.account.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
