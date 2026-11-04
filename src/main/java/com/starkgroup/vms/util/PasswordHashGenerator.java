package com.starkgroup.vms.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utility to generate a BCrypt password hash safely.
 * Prompts via Console or Standard Input so the raw password never appears in shell history.
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        try {
            String password = null;
            if (System.console() != null) {
                char[] passwordChars = System.console().readPassword("Enter password to hash: ");
                if (passwordChars != null) {
                    password = new String(passwordChars);
                }
            } else {
                System.out.print("Enter password to hash: ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                password = reader.readLine();
            }

            if (password == null || password.trim().isEmpty()) {
                System.err.println("Error: Password cannot be empty.");
                System.exit(1);
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hash = encoder.encode(password);

            System.out.println("\nGenerated BCrypt Hash (configure as COURSE_USER_PASSWORD_HASH):");
            System.out.println(hash);
        } catch (Exception e) {
            System.err.println("Failed to generate hash: " + e.getMessage());
            System.exit(1);
        }
    }
}
