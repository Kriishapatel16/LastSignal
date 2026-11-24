
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="util.HtmlUtil"%>
<%@page import="model.CheckInBean"%>
<%@page import="model.CheckInSettingsBean"%>
<%@page import="java.time.format.DateTimeFormatter"%>

<%
    Integer userId = (Integer) session.getAttribute("userId");

    if (userId == null || userId <= 0) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    String userName = (String) session.getAttribute("userName");

    if (userName == null || userName.trim().isEmpty()) {
        userName = "User";
    }

    CheckInBean currentCheckIn =
            (CheckInBean) request.getAttribute("currentCheckIn");

    CheckInBean latestCheckIn =
            (CheckInBean) request.getAttribute("latestCheckIn");

    CheckInSettingsBean settings =
            (CheckInSettingsBean) request.getAttribute("settings");

    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    boolean monitoringEnabled =
            settings != null && settings.isEnabled();

    String scheduleText = "Not configured";
    String nextCheckInText = "—";
    String lastConfirmationText = "No confirmation yet";
    String graceText = "—";

    if (monitoringEnabled) {
        scheduleText = settings.getIntervalHours() + " hours";
        graceText = settings.getGraceMinutes() + " minutes";
    }

    if (currentCheckIn != null
            && currentCheckIn.getScheduledAt() != null) {

        nextCheckInText = currentCheckIn.getScheduledAt()
                .toLocalDateTime()
                .format(formatter);
    }

    if (latestCheckIn != null
            && latestCheckIn.getConfirmedAt() != null) {

        lastConfirmationText = latestCheckIn.getConfirmedAt()
                .toLocalDateTime()
                .format(formatter);
    }

    String settingsMessage =
            (String) session.getAttribute("settingsMessage");

    String settingsError =
            (String) session.getAttribute("settingsError");

    String checkinMessage =
            (String) session.getAttribute("checkinMessage");

    String checkinError =
            (String) session.getAttribute("checkinError");

    session.removeAttribute("settingsMessage");
    session.removeAttribute("settingsError");
    session.removeAttribute("checkinMessage");
    session.removeAttribute("checkinError");

    String csrfToken =
            (String) session.getAttribute("csrfToken");
%>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Dashboard | LastSignal</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>

<body>

<div class="app">

    <header class="topbar">

        <a class="logo"
           href="${pageContext.request.contextPath}/DashboardServlet">
            LastSignal
        </a>

        <nav class="nav">

            <a class="current"
               href="${pageContext.request.contextPath}/DashboardServlet">
                Dashboard
            </a>

            <a href="${pageContext.request.contextPath}/contacts">
                Trusted Contacts
            </a>

            <a href="${pageContext.request.contextPath}/SettingsServlet">
                Settings
            </a>

            <form action="${pageContext.request.contextPath}/logout"
                  method="post">

                <input type="hidden"
                       name="csrfToken"
                       value="<%=HtmlUtil.escape(csrfToken == null ? "" : csrfToken)%>">

                <button type="submit">
                    Logout
                </button>

            </form>

        </nav>

    </header>

    <main class="content">

        <div class="dashboard-head">

            <div>
                <h1>
                    Hello, <%=HtmlUtil.escape(userName)%>
                </h1>

                <p class="muted" style="margin-top:7px">
                    Your safety check-in overview.
                </p>
            </div>

            <span class="status">
                <%=monitoringEnabled
                        ? "Monitoring active"
                        : "Monitoring disabled"%>
            </span>

        </div>

        <% if (settingsMessage != null) { %>
            <div class="success-message">
                <%=HtmlUtil.escape(settingsMessage)%>
            </div>
        <% } %>

        <% if (settingsError != null) { %>
            <div class="error-message">
                <%=HtmlUtil.escape(settingsError)%>
            </div>
        <% } %>

        <% if (checkinMessage != null) { %>
            <div class="success-message">
                <%=HtmlUtil.escape(checkinMessage)%>
            </div>
        <% } %>

        <% if (checkinError != null) { %>
            <div class="error-message">
                <%=HtmlUtil.escape(checkinError)%>
            </div>
        <% } %>

        <div class="dashboard-grid">

            <section>

                <div class="panel">

                    <h2>Monitoring overview</h2>

                    <div class="form-grid">

                        <div class="metric">
                            <label>Check-in schedule</label>
                            <strong>
                                <%=HtmlUtil.escape(scheduleText)%>
                            </strong>
                            <small>
                                Your configured check-in interval
                            </small>
                        </div>

                        <div class="metric">
                            <label>Next check-in</label>
                            <strong>
                                <%=HtmlUtil.escape(nextCheckInText)%>
                            </strong>
                            <small>
                                Scheduled check-in time
                            </small>
                        </div>

                        <div class="metric">
                            <label>Last confirmation</label>
                            <strong>
                                <%=HtmlUtil.escape(lastConfirmationText)%>
                            </strong>
                            <small>
                                Your latest recorded confirmation
                            </small>
                        </div>

                        <div class="metric">
                            <label>Grace period</label>
                            <strong>
                                <%=HtmlUtil.escape(graceText)%>
                            </strong>
                            <small>
                                Time allowed after a missed check-in
                            </small>
                        </div>

                    </div>

                </div>

                <div class="panel">

                    <h2>Quick access</h2>

                    <div class="form-actions">

                        <a class="btn secondary"
                           href="${pageContext.request.contextPath}/contacts">
                            Manage trusted contacts
                        </a>

                        <a class="btn outline"
                           href="${pageContext.request.contextPath}/SettingsServlet">
                            Check-in settings
                        </a>

                    </div>

                </div>

            </section>

            <aside class="checkin-cta">

                <span class="status">
                    Safety check-in
                </span>

                <h2>Are you safe?</h2>

                <p>
                    Confirm that you are safe when your check-in is due.
                    Your confirmation will update your check-in record
                    and schedule the next check-in.
                </p>

                <% if (!monitoringEnabled) { %>

                    <a class="btn primary"
                       href="${pageContext.request.contextPath}/SettingsServlet">
                        Enable monitoring
                    </a>

                <% } else if (currentCheckIn != null) { %>

                    <form action="${pageContext.request.contextPath}/CheckInServlet"
                          method="post">

                        <input type="hidden"
                               name="csrfToken"
                               value="<%=HtmlUtil.escape(csrfToken == null ? "" : csrfToken)%>">

                        <button type="submit" class="btn primary">
                            I'm Safe
                        </button>

                    </form>

                <% } else { %>

                    <p class="muted">
                        There is currently no pending check-in.
                        Please review your settings.
                    </p>

                    <a class="btn outline"
                       href="${pageContext.request.contextPath}/SettingsServlet">
                        Review settings
                    </a>

                <% } %>

            </aside>

        </div>

    </main>

    <footer class="footer">
        LastSignal · Stay connected with the people who matter.
    </footer>

</div>

</body>
</html>
