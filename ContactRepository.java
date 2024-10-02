package com.smart.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.model.Contact;
import com.smart.model.User;

public interface ContactRepository extends JpaRepository<Contact, Integer>{
	//pagination...  
	
//	@Query("from Contact as c where c.user.id =: userId")
//	public List<Contact> findContactByUser(@Param("userId")int userId);
	
// public List<Contact> findById(@Param("id")int id); 
	
	//currentpage-page 
	//Contact per page - 5
 
 public Page<Contact> findAllByUserId(int cId, Pageable pepageable);


 
   public List<Contact> findByNameContainingAndUser(String name,User user);

}
