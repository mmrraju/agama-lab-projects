package co

import javax.mail.*
import javax.mail.internet.*

class AgamaEmail {

    static boolean sendMail(String host, String port, String email, String password, String to, String subject, String content ) {
            
        Properties props = System.getProperties()
        props.put("mail.smtp.user", email)
        props.put("mail.smtp.host", host)
        props.put("mail.smtp.port", port)
        props.put("mail.smtp.starttls.enable","true")
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        props.put("mail.smtp.ssl.trust", host) 


       MimeMessage message = new MimeMessage(Session.getDefaultInstance(props))
       message.setFrom(new InternetAddress(addresser))
       message.addRecipients(Message.RecipientType.TO, new InternetAddress(to))

       message.setSubject(subject)
       message.setContent(content)

       try {
           Transport.send(message, email, password)
       } catch (MessagingException e) {
           e.printStackTrace()
       }


    }

    static int generateOtp(){
       // 6 digit random number
       int i = new Random().nextInt(900000) + 100000;

       return i
    }

    static Object updateUser(Object user, String password){

          user.setAttribute("userPassword",password)
          return user
    }

}