/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 24 mars 2004
 * Time: 19:16:29
 */

package com.xpn.xwiki.user;

import org.securityfilter.authenticator.persistent.DefaultPersistentLoginManager;

import javax.crypto.Cipher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyPersistentLoginManager extends DefaultPersistentLoginManager {
    protected String cookiePath = "/";

    public void setCookiePath(String cp) {
        cookiePath = cp;
    }
    /**
     * Remember a specific login
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param username the username tha's being remembered
     * @param password the password that's being remembered
     */
    public void rememberLogin(
       HttpServletRequest request,
       HttpServletResponse response,
       String username,
       String password
    ) throws IOException, ServletException {
       if (protection.equals(PROTECTION_ALL) || protection.equals(PROTECTION_ENCRYPTION)) {
          username = encryptText(username);
          password = encryptText(password);
          if (username == null || password == null) {
             System.out.println("ERROR!!");
             System.out.println("There was a problem encrypting the username or password!!");
             System.out.println("Remember Me function will be disabled!!");
             return;
          }
       }
       // create client cookie to store username and password
       Cookie usernameCookie = new Cookie(COOKIE_USERNAME, username);
       usernameCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
       usernameCookie.setPath(cookiePath);
       response.addCookie(usernameCookie);
       Cookie passwdCookie = new Cookie(COOKIE_PASSWORD, password);
       passwdCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
       passwdCookie.setPath(cookiePath);
       response.addCookie(passwdCookie);
       Cookie rememberCookie = new Cookie(COOKIE_REMEMBERME, "true");
       rememberCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
       rememberCookie.setPath(cookiePath);
       response.addCookie(rememberCookie);
       if (protection.equals(PROTECTION_ALL) || protection.equals(PROTECTION_VALIDATION)) {
          String validationHash = getValidationHash(username, password, request.getRemoteAddr());
          if (validationHash != null) {
             Cookie validationCookie = new Cookie(COOKIE_VALIDATION, validationHash);
             validationCookie.setMaxAge(60 * 60 * 24 * Integer.parseInt(cookieLife));
             validationCookie.setPath(cookiePath);
             response.addCookie(validationCookie);
          } else {
             System.out.println("WARNING!!! WARNING!!!");
             System.out.println("PROTECTION=ALL or PROTECTION=VALIDATION was specified");
             System.out.println("but Validation Hash could NOT be generated");
             System.out.println("Validation has been disabled!!!!");
          }
       }
       return;
    }

    /**
     * Get validation hash for the specified parameters.
     *
     * @param username
     * @param password
     * @param clientIP
     * @return validation hash
     */
    private String getValidationHash(String username, String password, String clientIP) {
       if (validationKey == null) {
          System.out.println("ERROR! >> validationKey not spcified....");
          System.out.println("ERROR! >> you are REQUIRED to specify the validatonkey in the config xml");
          return null;
       }
       MessageDigest md5 = null;
       StringBuffer sbValueBeforeMD5 = new StringBuffer();

       try {
          md5 = MessageDigest.getInstance("MD5");
       } catch (NoSuchAlgorithmException e) {
          System.out.println("Error: " + e);
       }

       try {
          sbValueBeforeMD5.append(username.toString());
          sbValueBeforeMD5.append(":");
          sbValueBeforeMD5.append(password.toString());
          sbValueBeforeMD5.append(":");
          if (useIP.equals("true")) {
             sbValueBeforeMD5.append(clientIP.toString());
             sbValueBeforeMD5.append(":");
          }
          sbValueBeforeMD5.append(validationKey.toString());

          valueBeforeMD5 = sbValueBeforeMD5.toString();
          md5.update(valueBeforeMD5.getBytes());

          byte[] array = md5.digest();
          StringBuffer sb = new StringBuffer();
          for (int j = 0; j < array.length; ++j) {
             int b = array[j] & 0xFF;
             if (b < 0x10) sb.append('0');
             sb.append(Integer.toHexString(b));
          }
          valueAfterMD5 = sb.toString();
       } catch (Exception e) {
          System.out.println("Error:" + e);
       }
       return valueAfterMD5;
    }

    /**
     * Encrypt a string.
     *
     * @param clearText
     * @return clearText, encrypted
     */
    private String encryptText(String clearText) {
       sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
       try {
          Cipher c1 = Cipher.getInstance(cipherParameters);
          if (secretKey != null) {
             c1.init(c1.ENCRYPT_MODE, secretKey);
             byte clearTextBytes[];
             clearTextBytes = clearText.getBytes();
             byte encryptedText[] = c1.doFinal(clearTextBytes);
             String encryptedEncodedText = encoder.encode(encryptedText);
             return encryptedEncodedText;
          } else {
             System.out.println("ERROR! >> SecretKey not generated ....");
             System.out.println("ERROR! >> you are REQUIRED to specify the encryptionKey in the config xml");
             return null;
          }
       } catch (Exception e) {
          System.out.println("Error: " + e);
          e.printStackTrace();
          return null;
       }
    }


      /**
    * Forget a login
    *
    * @param request the servlet request
    * @param response the servlet response
    */
   public void forgetLogin(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
      removeCookie(request, response, COOKIE_USERNAME);
      removeCookie(request, response, COOKIE_PASSWORD);
      removeCookie(request, response, COOKIE_REMEMBERME);
      removeCookie(request, response, COOKIE_VALIDATION);
      return;
   }

      /**
    * Given an array of cookies and a name, this method tries
    * to find and return the cookie from the array that has
    * the given name. If there is no cookie matching the name
    * in the array, null is returned.
    */
    private static Cookie getCookie(Cookie[] cookies, String cookieName) {
      if (cookies != null) {
         for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookieName.equals(cookie.getName())) {
               return (cookie);
            }
         }
      }
      return null;
   }

    /**
    * Remove a cookie.
    *
    * @param request
    * @param response
    * @param cookieName
    */
   private void removeCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
      Cookie cookie = getCookie(request.getCookies(), cookieName);
      if (cookie != null) {
         cookie.setMaxAge(0);
         cookie.setPath(cookiePath);
         response.addCookie(cookie);
      }
   }

}
