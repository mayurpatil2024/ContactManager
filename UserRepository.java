package com.smart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smart.model.User;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>{
  
	public User  findByEmail(String email);
	

//	@Query("SElECT u FROM User u WHERE u.email = : email" )
//	
//   public  User getUserByUserName(@Param("email") String email);
}
