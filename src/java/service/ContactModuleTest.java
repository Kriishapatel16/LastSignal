package service;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import model.ContactBean;

public class ContactModuleTest {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        ContactService contactService = new ContactService();

        System.out.println("===== LASTSIGNAL CONTACT MODULE TEST =====");

        try {
            System.out.print("Enter your registered User ID: ");
            int userId = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter Contact Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Contact Email: ");
            String email = sc.nextLine();

            System.out.print("Enter Priority (1-3): ");
            int priority = Integer.parseInt(sc.nextLine().trim());

            ContactBean contact = new ContactBean();

            contact.setUserId(userId);
            contact.setContactName(name);
            contact.setEmail(email);
            contact.setPriorityLevel(priority);
            contact.setActive(true);

            // Add contact
            System.out.println("\n--- Add Contact ---");

            boolean added = contactService.addContact(contact);

            if (!added) {
                System.out.println("Contact was not added.");
                return;
            }

            System.out.println("Contact added successfully.");

            // Display contacts
            System.out.println("\n--- Contact List ---");

            List<ContactBean> contacts =
                    contactService.getContacts(userId);

            for (ContactBean c : contacts) {
                System.out.println(
                        "ID: " + c.getContactId()
                        + " | Name: " + c.getContactName()
                        + " | Email: " + c.getEmail()
                        + " | Priority: " + c.getPriorityLevel()
                        + " | Active: " + c.isActive()
                );
            }

            // Update contact
            System.out.println("\n--- Update Contact ---");

            System.out.print("Enter the Contact ID to update: ");
            int contactId = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter Updated Name: ");
            String updatedName = sc.nextLine();

            ContactBean updatedContact = new ContactBean();

            updatedContact.setContactId(contactId);
            updatedContact.setUserId(userId);
            updatedContact.setContactName(updatedName);
            updatedContact.setEmail(email);
            updatedContact.setPriorityLevel(priority);

            boolean updated =
                    contactService.updateContact(updatedContact);

            System.out.println(
                    updated ? "Contact updated successfully."
                            : "Contact not found or access denied."
            );

            // Test ownership protection
            System.out.println("\n--- Ownership Security Test ---");

            boolean unauthorizedDelete =
                    contactService.deleteContact(
                            contactId, userId + 999999
                    );

            System.out.println(
                    unauthorizedDelete
                            ? "FAILED: Unauthorized deletion occurred."
                            : "PASSED: Another user cannot delete this contact."
            );

            // Change contact status
            System.out.println("\n--- Contact Status Test ---");

            boolean statusChanged =
                    contactService.changeContactStatus(
                            contactId, userId, false
                    );

            System.out.println(
                    statusChanged
                            ? "Contact deactivated successfully."
                            : "Contact not found."
            );

            // Optional cleanup
            System.out.println("\n--- Cleanup ---");

            System.out.print(
                    "Do you want to delete this test contact? (yes/no): "
            );

            String choice = sc.nextLine().trim();

            if (choice.equalsIgnoreCase("yes")) {

                boolean deleted =
                        contactService.deleteContact(
                                contactId, userId
                        );

                System.out.println(
                        deleted
                                ? "Test contact deleted successfully."
                                : "Contact could not be deleted."
                );

            } else {
                System.out.println("Test contact retained in database.");
            }

            System.out.println("\nContact module test completed.");

        } catch (NumberFormatException e) {

            System.out.println("Please enter valid numeric IDs and priority.");

        } catch (IllegalArgumentException e) {

            System.out.println("Validation Error: " + e.getMessage());

        } catch (SQLException e) {

            if ("23505".equals(e.getSQLState())) {
                System.out.println(
                        "This priority is already assigned to another contact."
                );
            } else {
                System.out.println(
                        "Database operation failed. Check the database connection and table structure."
                );
            }
        }
    }
}