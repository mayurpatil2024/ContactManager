package com.smart.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ssl.SslProperties.Bundles.Watch.File;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.smart.helper.Message;
import com.smart.model.Contact;
import com.smart.model.User;
import com.smart.repository.ContactRepository;
import com.smart.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")

public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();

		System.out.println("USERNAME " + userName);

		User user = userRepository.findByEmail(userName);
		System.out.println("USER " + user);

		model.addAttribute("user", user);

	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");

		// get the user using username(Email)

		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");

		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {

			String name = principal.getName();

			User user = this.userRepository.findByEmail(name);

			// processing and uploading file..

			if (file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is empty");
				contact.setImage("contact.png");
			} else {
				// file the file to folder and update the name to contact

				contact.setImage(file.getOriginalFilename());

				java.io.File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath(), file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image is uploaded");

			}

			user.getContacts().add(contact);
			contact.setUser(user);

			this.userRepository.save(user);

			System.out.println("DATA " + contact);

			System.out.println("Added to data base");

			// message success............
			session.setAttribute("message", new Message("Your contact is added !! Add more...", "success"));

		} catch (Exception e) {
			System.out.println("ERROR " + e.getMessage());
			e.printStackTrace();

			session.setAttribute("message", new Message("Some went wrong !! Try again..", "danger"));

			// message error
		}
		return "normal/add_contact_form";
	}

	public void removeMessageFromSession() {
		try {
			System.out.println("removing message from session");
			HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
					.getSession();
			session.removeAttribute("message");
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	// show contacts handler
	// per page = 5[n]
	// current page = 0 [page]

	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {

		m.addAttribute("title", "Show User Contacts");
		// contact ki list ko bhejni hai

		String userName = principal.getName();
		User user = this.userRepository.findByEmail(userName);

		PageRequest pageable = PageRequest.of(page, 5);

		// currentpage-page
		// Contact per page - 5

		Page<Contact> contacts = this.contactRepository.findAllByUserId(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";

	}

	// showing particular contact details.

	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);

		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.findByEmail(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());

		}

		System.out.println("CID " + cId);
		return "normal/contact_detail";
	}

	// delete contact handler

	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, Principal principal,
			HttpSession session) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);

		Contact contact = contactOptional.get();

		// check..
		String userName = principal.getName();
		User user = this.userRepository.findByEmail(userName);
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contactDeatils", contact);

		}
		
		
		
		User user1 = this.userRepository.findByEmail(principal.getName());
         
		user1.getContacts().remove(contact);
		this.userRepository.save(user);
		
		
		//		contact.setUser(null);
//		this.contactRepository.delete(contact);

		session.setAttribute("message", new Message("Contact deleted successfully....", "success"));

		return "redirect:/user/show-contacts/0";
	}

	// open update from handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {

		m.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cid).get();

		m.addAttribute("contact", contact);
		return "normal/update_form";

	}

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session,Principal principal) {

		try {
			
			//old contact details
			
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			//image..
			
			if(!file.isEmpty()) 
			{ 
				//file work..
				//rewrite
				
				//delete old photo
				
			java.io.File deleteFile = new ClassPathResource("static/img").getFile();
			
			java.io.File file1 = new java.io.File(deleteFile, oldcontactDetail.getImage());
		         file1.delete();
		         
		         
//				update new photo
				
				java.io.File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath(), file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
				
			}else {
				contact.setImage(oldcontactDetail.getImage());
			}
			
			User user = this.userRepository.findByEmail(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated...", "success"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("CONTACT NAME " + contact.getName());
		System.out.println("CONTACT ID " + contact.getcId());

		return "redirect:/user/"+contact.getcId()+"/contact";

	}
	
	//your profile handler
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "Profile Page");
		
		return "normal/profile";
		
	}
	
	//open settings handler
	
	@GetMapping("/settings")
	public String openSettings() 
	{
		
		return "normal/settings";
		
	} 
	
	
	//change password.. handler  
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassowrd, Principal principal,HttpSession session) {
		
		
		System.out.println("OLD PASSWORD " +oldPassword); 
		System.out.println("NEW PASSWORD " +newPassowrd); 
		
		String UserName = principal.getName(); 
		
		User currentUser = this.userRepository.findByEmail(UserName);
		
		System.out.println(currentUser.getPassword()); 
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			//change the password   
			
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassowrd));
			this.userRepository.save(currentUser); 
			
			session.setAttribute("message", new Message("Your password is Successfully changed...","success"));
		}else {
			
			//error... 
			
			session.setAttribute("message", new Message("Please Enter correct old password", "danger"));
			
			return "redirect:/user/settings";
		
		}
		return "redirect:/user/index";
		
	}
	

}
