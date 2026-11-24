<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List,model.ContactBean,util.HtmlUtil"%>
<%
    List<ContactBean> contacts = (List<ContactBean>) request.getAttribute("contacts");
    String csrfToken = (String) session.getAttribute("csrfToken");
    String message = (String) request.getAttribute("message");
    String error = (String) request.getAttribute("error");
    String ctx = request.getContextPath();
    int activeCount = 0;
    if (contacts != null) {
        for (ContactBean contact : contacts) {
            if (contact.isActive()) activeCount++;
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Trusted Contacts | LastSignal</title>
    <link rel="stylesheet" href="<%=ctx%>/css/style.css">
    <script src="<%=ctx%>/js/app.js" defer></script>
</head>
<body>
<div class="app">
    <header class="topbar">
        <a class="logo" href="<%=ctx%>/dashboard.jsp">LastSignal</a>
        <nav class="nav" aria-label="Main navigation">
            <a href="<%=ctx%>/dashboard.jsp">Dashboard</a>
            <a class="current" href="<%=ctx%>/contacts">Trusted Contacts</a>
            <a href="<%=ctx%>/settings.jsp">Settings</a>
            <form action="<%=ctx%>/logout" method="post">
                <input type="hidden" name="csrfToken" value="<%=HtmlUtil.escape(csrfToken)%>">
                <button type="submit">Logout</button>
            </form>
        </nav>
    </header>

    <main class="content">
        <div class="page-title">
            <h1>Trusted Contacts</h1>
            <p>Add and manage the people who may be notified if you miss a check-in.</p>
        </div>

        <% if (message != null) { %>
            <div class="notice" role="status"><%=HtmlUtil.escape(message)%></div>
        <% } %>
        <% if (error != null) { %>
            <div class="notice error" role="alert"><%=HtmlUtil.escape(error)%></div>
        <% } %>

        <div class="split">
            <section class="panel">
                <h2>Add a trusted contact</h2>
                <form action="<%=ctx%>/contacts" method="post">
                    <input type="hidden" name="csrfToken" value="<%=HtmlUtil.escape(csrfToken)%>">
                    <input type="hidden" name="action" value="add">

                    <div class="field">
                        <label for="contactName">Contact name</label>
                        <input class="control" id="contactName" name="contactName" type="text"
                               minlength="2" maxlength="100" autocomplete="name" required>
                    </div>
                    <div class="field">
                        <label for="contactEmail">Email address</label>
                        <input class="control" id="contactEmail" name="email" type="email"
                               maxlength="254" autocomplete="email" required>
                    </div>
                    <div class="field">
                        <label for="priority">Notification priority</label>
                        <select class="control" id="priority" name="priority" required>
                            <option value="">Choose priority</option>
                            <option value="1">Priority 1 — First contact</option>
                            <option value="2">Priority 2 — Second contact</option>
                            <option value="3">Priority 3 — Third contact</option>
                        </select>
                    </div>
                    <button class="btn primary full" type="submit">Add contact</button>
                </form>
                <p class="hint">You can assign one contact to each priority. Only active contacts are considered for notifications.</p>
            </section>

            <section class="panel">
                <div class="dashboard-head">
                    <div>
                        <h2>Your contacts</h2>
                        <p class="muted"><%=contacts == null ? 0 : contacts.size()%> saved · <%=activeCount%> active</p>
                    </div>
                </div>

                <% if (contacts == null || contacts.isEmpty()) { %>
                    <div class="empty">
                        <strong>No trusted contacts yet</strong>
                        Add your first trusted contact using the form.
                    </div>
                <% } else { %>
                    <% for (ContactBean c : contacts) { int id = c.getContactId(); %>
                        <article class="contact">
                            <div class="contact-head">
                                <div>
                                    <div class="contact-name"><%=HtmlUtil.escape(c.getContactName())%></div>
                                    <div class="contact-email"><%=HtmlUtil.escape(c.getEmail())%></div>
                                </div>
                                <span class="status">Priority <%=c.getPriorityLevel()%></span>
                            </div>
                            <p class="hint"><span class="status <%=c.isActive() ? "" : "off"%>"><%=c.isActive() ? "Active" : "Inactive"%></span></p>

                            <div class="contact-actions">
                                <button class="btn outline small" type="button" data-toggle-target="edit-<%=id%>" aria-controls="edit-<%=id%>">Edit</button>

                                <form action="<%=ctx%>/contacts" method="post">
                                    <input type="hidden" name="csrfToken" value="<%=HtmlUtil.escape(csrfToken)%>">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="contactId" value="<%=id%>">
                                    <input type="hidden" name="active" value="<%=!c.isActive()%>">
                                    <button class="btn secondary small" type="submit"><%=c.isActive() ? "Deactivate" : "Activate"%></button>
                                </form>

                                <form action="<%=ctx%>/contacts" method="post">
                                    <input type="hidden" name="csrfToken" value="<%=HtmlUtil.escape(csrfToken)%>">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="contactId" value="<%=id%>">
                                    <button class="btn danger small" type="submit" data-confirm="Delete this trusted contact?">Delete</button>
                                </form>
                            </div>

                            <div id="edit-<%=id%>" hidden style="margin-top:16px;padding:16px;background:#f7f7f1;border-radius:8px">
                                <form action="<%=ctx%>/contacts" method="post">
                                    <input type="hidden" name="csrfToken" value="<%=HtmlUtil.escape(csrfToken)%>">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="contactId" value="<%=id%>">
                                    <div class="field">
                                        <label for="name-<%=id%>">Contact name</label>
                                        <input class="control" id="name-<%=id%>" name="contactName" type="text"
                                               value="<%=HtmlUtil.escape(c.getContactName())%>" minlength="2" maxlength="100" required>
                                    </div>
                                    <div class="field">
                                        <label for="email-<%=id%>">Email address</label>
                                        <input class="control" id="email-<%=id%>" name="email" type="email"
                                               value="<%=HtmlUtil.escape(c.getEmail())%>" maxlength="254" required>
                                    </div>
                                    <div class="field">
                                        <label for="priority-<%=id%>">Notification priority</label>
                                        <select class="control" id="priority-<%=id%>" name="priority" required>
                                            <option value="1" <%=c.getPriorityLevel() == 1 ? "selected" : ""%>>Priority 1</option>
                                            <option value="2" <%=c.getPriorityLevel() == 2 ? "selected" : ""%>>Priority 2</option>
                                            <option value="3" <%=c.getPriorityLevel() == 3 ? "selected" : ""%>>Priority 3</option>
                                        </select>
                                    </div>
                                    <div class="form-actions">
                                        <button class="btn primary small" type="submit">Save changes</button>
                                        <button class="btn outline small" type="button" data-toggle-target="edit-<%=id%>">Cancel</button>
                                    </div>
                                </form>
                            </div>
                        </article>
                    <% } %>
                <% } %>
            </section>
        </div>
    </main>
    <footer class="footer">LastSignal · Trusted contact management</footer>
</div>
</body>
</html>
