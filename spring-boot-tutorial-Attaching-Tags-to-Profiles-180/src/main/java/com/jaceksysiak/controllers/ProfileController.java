package com.jaceksysiak.controllers;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.validation.Valid;

import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.jaceksysiak.exceptions.ImageTooSmallException;
import com.jaceksysiak.exceptions.InvalidFileException;
import com.jaceksysiak.model.FileInfo;
import com.jaceksysiak.model.Interest;
import com.jaceksysiak.model.Profile;
import com.jaceksysiak.model.SiteUser;
import com.jaceksysiak.service.FileService;
import com.jaceksysiak.service.InterestService;
import com.jaceksysiak.service.ProfileService;
import com.jaceksysiak.service.UserService;
import com.jaceksysiak.status.PhotoUploadStatus;

@Controller
public class ProfileController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ProfileService profileService;
	
	@Autowired
	private InterestService interestService;
	
	@Autowired
	private PolicyFactory htmlPolicy;
	
	@Autowired
	FileService fileService;
	
	@Value("${photo.upload.ok}")
	private String photoStatusOK;
	 
	@Value("${photo.upload.invalid}")
	private String photoStatusInvalid;
	
	@Value("${photo.upload.ioexception}")
	private String photoStatusIOException;
	
	@Value("${photo.upload.toosmall}")
	private String photoStatusTooSmall;
	
	@Value("${photo.upload.directory}")
	private String photoUploadDirectory;
	
	
	private SiteUser getUser() {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = auth.getName();
		
		return userService.get(email);
	}
	
	private ModelAndView showProfile(SiteUser user) {
		
		ModelAndView modelAndView = new ModelAndView();
		 
		if(user == null) {
			modelAndView.setViewName("redirect:/");
			return modelAndView;
		}
 
		Profile profile = profileService.getUserProfile(user);
		
 		if(profile == null) {
 			profile = new Profile();
 			profile.setUser(user);
 			profileService.save(profile);
 		}
 		
 		Profile webProfile = new Profile();
 		webProfile.safeCopyFrom(profile);
 		  
 		modelAndView.getModel().put("userId", user.getId());
 		modelAndView.getModel().put("profile", webProfile);
 		
		modelAndView.setViewName("app.profile");
		
		return modelAndView;
	}

	@RequestMapping(value="/profile")
	public ModelAndView showProfile() {
		 
		SiteUser user = getUser();
		
		ModelAndView modelAndView = showProfile(user);
		 
		return modelAndView;
	}

	@RequestMapping(value = "/profile/{id}")
	public ModelAndView showProfile(@PathVariable("id") Long id) {

		SiteUser user = userService.get(id);
		
		ModelAndView modelAndView = showProfile(user);
		
		return modelAndView;
	}
	

	@RequestMapping(value="/edit-profile-about", method=RequestMethod.GET)
	public ModelAndView editProfileAbout(ModelAndView modelAndView) {
		
		SiteUser user = getUser();
		Profile profile = profileService.getUserProfile(user);
		
		Profile webProfile = new Profile();
		webProfile.safeCopyFrom(profile);
		
		modelAndView.getModel().put("profile", webProfile);
		modelAndView.setViewName("app.editProfileAbout");
		
		return modelAndView;
	}
	
	
	@RequestMapping(value="/edit-profile-about", method=RequestMethod.POST)
	public ModelAndView editProfileAbout(ModelAndView modelAndView, @Valid Profile webProfile, BindingResult result) {
		
		modelAndView.setViewName("app.editProfileAbout");
		
		SiteUser user = getUser();
		Profile profile = profileService.getUserProfile(user);
		
		profile.safeMergeFrom(webProfile, htmlPolicy);
		
		modelAndView.getModel().put("profile", webProfile);
		
 		if(!result.hasErrors()) {
 			profileService.save(profile);
 			modelAndView.setViewName("redirect:/profile");
 		}
		 
		return modelAndView;
	}
	
	
	
	@RequestMapping(value = "/upload-profile-photo", method = RequestMethod.POST)
    @ResponseBody // Return data in JSON format
	public ResponseEntity<PhotoUploadStatus> handlePhotoUploads(@RequestParam("file") MultipartFile file) {
		
		SiteUser user = getUser();
		Profile profile = profileService.getUserProfile(user);
		
		Path oldPhotoPath = profile.getPhoto(photoUploadDirectory);
		
		PhotoUploadStatus status = new PhotoUploadStatus(photoStatusOK);
		
		try {
			FileInfo photoInfo = fileService.saveImageFile(file, photoUploadDirectory, "photos", "p" + user.getId(), 100, 100);
			//System.out.println(photoInfo);
 			profile.setPhotoDetails(photoInfo);
 			profileService.save(profile);
 
 			if (oldPhotoPath != null) {
 				Files.delete(oldPhotoPath);
 			}
			
		} catch (InvalidFileException e) {
			status.setMessage(photoStatusInvalid);
			e.printStackTrace();
		} catch (IOException e) {
			status.setMessage(photoStatusIOException);
			e.printStackTrace();
		} catch (ImageTooSmallException e) {
			status.setMessage(photoStatusTooSmall);
			e.printStackTrace();
		}
  
        return new ResponseEntity(status, HttpStatus.OK);
	}
	
	
	@RequestMapping(value = "/profilephoto/{id}", method = RequestMethod.GET)
	@ResponseBody
	ResponseEntity<InputStreamResource> servePhoto(@PathVariable Long id) throws IOException {
		
		SiteUser user = userService.get(id);
		Profile profile = profileService.getUserProfile(user);

		Path photoPath = Paths.get(photoUploadDirectory, "default", "avatar.jpg");

		if (profile != null && profile.getPhoto(photoUploadDirectory) != null) {
			photoPath = profile.getPhoto(photoUploadDirectory);
		}

		return ResponseEntity
				.ok()
				.contentLength(Files.size(photoPath))
				.contentType(MediaType.parseMediaType(URLConnection.guessContentTypeFromName(photoPath.toString())))
				.body(new InputStreamResource(Files.newInputStream(photoPath, StandardOpenOption.READ)));
	}
	
	
	@RequestMapping(value="/save-interest", method=RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> saveInterest(@RequestParam("name") String interestName) {
		
		//System.out.println("Hello there - saving interest in ProfileController.saveInterest()");
		
		SiteUser user = getUser();
		Profile profile = profileService.getUserProfile(user);
		
		String cleanedInterestName = htmlPolicy.sanitize(interestName);
		
		Interest interest = interestService.createIfNotExists(cleanedInterestName);
		
		profile.addInterest(interest);
		profileService.save(profile);
		
		return new ResponseEntity<>(null, HttpStatus.OK);
	}
	
	
	@RequestMapping(value="/delete-interest", method=RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> deleteInterest(@RequestParam("name") String interestName) {
		
		SiteUser user = getUser();
		Profile profile = profileService.getUserProfile(user);
	
		profile.removeInterest(interestName);
	
		profileService.save(profile);
		
		return new ResponseEntity<>(null, HttpStatus.OK);
	}
}





































