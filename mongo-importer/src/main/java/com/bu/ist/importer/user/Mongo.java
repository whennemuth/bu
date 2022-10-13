package com.bu.ist.importer.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class Mongo {

	private static final String EXAMPLE_USER =
	"{\r\n" + 
	"    \"_id\" : ObjectId(\"59ba75183762d000a5aba712\"),\r\n" + 
	"    \"updatedAt\" : ISODate(\"2017-09-14T12:24:56.624Z\"),\r\n" + 
	"    \"createdAt\" : ISODate(\"2017-09-14T12:24:56.624Z\"),\r\n" + 
	"    \"passwordDigest\" : \"$2a$10$0k4yTzrurTa.hIt7ldgFeu04HDdovGI7eIaBINLLhyNPu/0XFmoHi\",\r\n" + 
	"    \"lowerUsername\" : \"cgagnon\",\r\n" + 
	"    \"uid\" : \"cgagnon\",\r\n" + 
	"    \"email\" : \"cgagnon@bu.edu\",\r\n" + 
	"    \"username\" : \"cgagnon\",\r\n" + 
	"    \"firstName\" : \"Christine\",\r\n" + 
	"    \"lastName\" : \"Gagnon\",\r\n" + 
	"    \"name\" : null,\r\n" + 
	"    \"updatedBy\" : {\r\n" + 
	"        \"id\" : \"kuali-system\"\r\n" + 
	"    },\r\n" + 
	"    \"approved\" : true,\r\n" + 
	"    \"role\" : \"user\",\r\n" + 
	"    \"schoolId\" : \"U10212518\"\r\n" + 
	"}";
	
	public Mongo(Oracle user) {
//		this.passwordDigest = UUID.randomUUID();
//		this.lowerUsername = "";
//		this.uid = "";
//		this.email = "";
//		this.
	}
	public static String byteArrayToHex(byte[] a) {
	   StringBuilder sb = new StringBuilder(a.length * 2);
	   for(byte b: a)
	      sb.append(String.format("%02x", b));
	   return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(UUID.randomUUID());
		
		MessageDigest salt = MessageDigest.getInstance("SHA-256");
		salt.update(UUID.randomUUID().toString().getBytes("UTF-8"));
		String digest = byteArrayToHex(salt.digest());
		System.out.println(digest);
		
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
		messageDigest.update(UUID.randomUUID().toString().getBytes("UTF-8"));
		String encryptedString = new String(messageDigest.digest());
		System.out.println(byteArrayToHex(encryptedString.getBytes()));
	}
}
