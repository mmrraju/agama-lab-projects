package org.gluu.agama.user;

import java.util.Map;
import java.util.Set;

import org.gluu.agama.smtp.jans.user.registration.JansUserRegistration;


public abstract class UserRegistration {

    public abstract String addNewUser(Map<String, String> profile, Set<String> attributes) throws Exception;
    
    public static UserRegistration getInstance(){
        return  JansUserRegistration.getInstance();
    }
}
