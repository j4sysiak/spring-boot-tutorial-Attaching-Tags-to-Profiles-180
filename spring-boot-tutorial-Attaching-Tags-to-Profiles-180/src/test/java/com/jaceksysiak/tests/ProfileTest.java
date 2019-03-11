package com.jaceksysiak.tests;

import static org.junit.Assert.*;

import java.util.HashSet;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.jaceksysiak.App;
import com.jaceksysiak.model.Interest;
import com.jaceksysiak.model.Profile;
import com.jaceksysiak.model.SiteUser;
import com.jaceksysiak.service.InterestService;
import com.jaceksysiak.service.ProfileService;
import com.jaceksysiak.service.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(App.class)
@WebAppConfiguration
@Transactional
public class ProfileTest {

	@Autowired 
	private UserService userService;
	
	@Autowired 
	private ProfileService profileService;
	
	@Autowired
	private InterestService interestService;
	
	private SiteUser[] users = {
		new SiteUser("ljlkj@jaceksysiak.com", "lkjlkjlk"),
		new SiteUser("dafdf@jaceksysiak.com", "gfhgfh"),
		new SiteUser("ghdgfhg@jaceksysiak.com", "wereretr")
	};
	
	private String[][] interests = {
			{"music", "guitar_xxxxxx", "plants"},
			{"music", "music", "philosophy_lkjlkjlk"},
			{"philosophy_lkjlkjlk", "football"}
};
	
	@Test
	public void testInterests() {
		
		
		for(int i=0; i<users.length; i++) {
			SiteUser user = users[i];
			String[] interestArray = interests[i];
			
			userService.register(user);
			
			HashSet<Interest> interestSet = new HashSet<>();
			
			for(String interestText: interestArray) {
				Interest interest = interestService.createIfNotExists(interestText);
				interestSet.add(interest);
				
				assertNotNull("Interest should not be null", interest);
				assertNotNull("Interest should have ID", interest.getId());
				assertEquals("Text should match", interestText, interest.getName());
			}
			 
			Profile profile = new Profile(user);
			profile.setInterests(interestSet);
			profileService.save(profile);
		
			Profile retrievedProfile = profileService.getUserProfile(user);
		
			assertEquals("Interest sets should match", interestSet, retrievedProfile.getInterests());
		}
	}
}


































