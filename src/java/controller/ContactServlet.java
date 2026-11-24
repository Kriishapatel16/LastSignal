package controller;

import dao.ContactDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import model.ContactBean;
import service.ContactService;

import java.io.IOException;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/contacts")
public class ContactServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(ContactServlet.class.getName());

    private ContactService contactService;

    @Override
    public void init() throws ServletException {
        contactService = new ContactService();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("userId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try {
            List<ContactBean> contacts =
                    contactService.getContacts(userId);

            request.setAttribute("contacts", contacts);

            Object message = session.getAttribute("contactMessage");
            Object error = session.getAttribute("contactError");

            if (message != null) {
                request.setAttribute("message", message);
                session.removeAttribute("contactMessage");
            }

            if (error != null) {
                request.setAttribute("error", error);
                session.removeAttribute("contactError");
            }

            request.getRequestDispatcher("/contacts.jsp")
                    .forward(request, response);

        } catch (SQLException e) {

            LOGGER.log(Level.SEVERE,
                    "Unable to retrieve trusted contacts.", e);
                request.setAttribute(
                       "contacts",
                       java.util.Collections.emptyList()
                );

            request.setAttribute(
                    "error",
                    "Unable to load contacts. Please try again."        
            );

            request.getRequestDispatcher("/contacts.jsp")
                    .forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("userId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        // Verify CSRF token.
        String sessionToken =
                (String) session.getAttribute("csrfToken");

        String requestToken =
                request.getParameter("csrfToken");

        if (sessionToken == null || requestToken == null
                || !MessageDigest.isEqual(
                        sessionToken.getBytes(StandardCharsets.UTF_8),
                        requestToken.getBytes(StandardCharsets.UTF_8))) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Invalid request."
            );
            return;
        }

        // User ID always comes from the authenticated session.
        int userId = (Integer) session.getAttribute("userId");

        String action = request.getParameter("action");

        try {

            if ("add".equals(action)) {
                addContact(request, userId);
            } else if ("update".equals(action)) {
                updateContact(request, userId);
            } else if ("delete".equals(action)) {
                deleteContact(request, userId);
            } else if ("status".equals(action)) {
                changeStatus(request, userId);
            } else {
                throw new IllegalArgumentException(
                        "Invalid contact operation."
                );
            }

        } catch (IllegalArgumentException e) {

            session.setAttribute("contactError", e.getMessage());

        } catch (SQLException e) {

            LOGGER.log(Level.WARNING,
                    "Trusted contact operation failed.", e);

            if ("23505".equals(e.getSQLState())) {
                session.setAttribute(
                        "contactError",
                        "This priority is already assigned to another contact."
                );
            } else {
                session.setAttribute(
                        "contactError",
                        "Unable to complete the operation. Please try again."
                );
            }
        }

        // Post/Redirect/Get prevents accidental form resubmission.
        response.sendRedirect(
                request.getContextPath() + "/contacts"
        );
    }

    private void addContact(HttpServletRequest request,
                            int userId) throws SQLException {

        ContactBean contact = new ContactBean();

        contact.setUserId(userId);
        contact.setContactName(
                request.getParameter("contactName")
        );
        contact.setEmail(
                request.getParameter("email")
        );
        contact.setPriorityLevel(
                parsePriority(request.getParameter("priority"))
        );
        contact.setActive(true);

        boolean added = contactService.addContact(contact);

                if (added) {
                    request.getSession().setAttribute(
                            "contactMessage",
                            "Trusted contact added successfully."
                    );
                } else {
                    request.getSession().setAttribute(
                            "contactError",
                            "Contact could not be added."
                    );
                }
        
    }

    private void updateContact(HttpServletRequest request,
                               int userId) throws SQLException {

        ContactBean contact = new ContactBean();

        contact.setContactId(
                parseId(request.getParameter("contactId"))
        );
        contact.setUserId(userId);
        contact.setContactName(
                request.getParameter("contactName")
        );
        contact.setEmail(
                request.getParameter("email")
        );
        contact.setPriorityLevel(
                parsePriority(request.getParameter("priority"))
        );

        boolean updated = contactService.updateContact(contact);

        request.getSession().setAttribute(
                updated ? "contactMessage" : "contactError",
                updated
                        ? "Contact updated successfully."
                        : "Contact not found or access denied."
        );
    }

    private void deleteContact(HttpServletRequest request,
                               int userId) throws SQLException {

        int contactId = parseId(
                request.getParameter("contactId")
        );

        boolean deleted =
                contactService.deleteContact(contactId, userId);

        request.getSession().setAttribute(
                deleted ? "contactMessage" : "contactError",
                deleted
                        ? "Contact deleted successfully."
                        : "Contact not found or access denied."
        );
    }

    private void changeStatus(HttpServletRequest request,
                              int userId) throws SQLException {

        int contactId = parseId(
                request.getParameter("contactId")
        );

        String status = request.getParameter("active");

        if (!"true".equals(status) && !"false".equals(status)) {
            throw new IllegalArgumentException(
                    "Invalid contact status."
            );
        }

        boolean active = Boolean.parseBoolean(status);

        boolean updated = contactService.changeContactStatus(
                contactId, userId, active
        );

        request.getSession().setAttribute(
                updated ? "contactMessage" : "contactError",
                updated
                        ? "Contact status updated successfully."
                        : "Contact not found or access denied."
        );
    }

    private int parseId(String value) {

        try {
            int id = Integer.parseInt(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid contact ID."
            );
        }
    }

    private int parsePriority(String value) {

        try {
            int priority = Integer.parseInt(value);

            if (priority < 1 || priority > 3) {
                throw new NumberFormatException();
            }

            return priority;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Priority must be between 1 and 3."
            );
        }
    }
}