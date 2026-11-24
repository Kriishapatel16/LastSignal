
package service;

import java.util.Scanner;
import model.UserBean;

public class UserModuleTest {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        UserService service = new UserService();

        System.out.println("===== LASTSIGNAL USER MODULE TEST =====");

        System.out.print("Enter Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Email: ");
        String email = sc.nextLine();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        System.out.print("Enter Phone: ");
        String phone = sc.nextLine();

        System.out.println("\n--- Registration ---");

        String result = service.register(
                name, email, password, phone
        );

        System.out.println(result);

        System.out.println("\n--- Login Test ---");

        System.out.print("Enter Email: ");
        String loginEmail = sc.nextLine();

        System.out.print("Enter Password: ");
        String loginPassword = sc.nextLine();

        UserBean user = service.login(loginEmail, loginPassword);

        if (user != null) {
            System.out.println("Login Successful!");
            System.out.println("Welcome, " + user.getName());
            System.out.println("User ID: " + user.getUserId());
            System.out.println("Role: " + user.getRole());
        } else {
            System.out.println("Login Failed!");
        }

        sc.close();
    }
}
