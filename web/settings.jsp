
<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="model.CheckInSettingsBean"%>
<%@page import="util.HtmlUtil"%>
<%
    CheckInSettingsBean settings =
            (CheckInSettingsBean) request.getAttribute("settings");

    if (settings == null) {
        response.sendRedirect(
                request.getContextPath() + "/SettingsServlet");
        return;
    }

    String settingsMessage =
            (String) session.getAttribute("settingsMessage");

    String settingsError =
            (String) session.getAttribute("settingsError");

    session.removeAttribute("settingsMessage");
    session.removeAttribute("settingsError");
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Check-in Settings | LastSignal</title>
        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/style.css">
    </head>

    <body>
        <div class="app">
            <header class="topbar">
                <a class="logo"
                   href="${pageContext.request.contextPath}/dashboard.jsp">
                    LastSignal
                </a>

                <nav class="nav">
                    <a href="${pageContext.request.contextPath}/dashboard.jsp">
                        Dashboard
                    </a>

                    <a href="${pageContext.request.contextPath}/contacts">
                        Trusted Contacts
                    </a>

                    <a class="current"
                       href="${pageContext.request.contextPath}/SettingsServlet">
                        Settings
                    </a>

                    <form action="${pageContext.request.contextPath}/logout"
                          method="post">
                        <input type="hidden" name="csrfToken"
                               value="<%= HtmlUtil.escape(
                                       (String) session.getAttribute("csrfToken")) %>">
                        <button type="submit">Logout</button>
                    </form>
                </nav>
            </header>

            <main class="content" style="max-width:850px">
                <div class="page-title">
                    <h1>Check-in settings</h1>
                    <p>
                        Choose how often you check in and how LastSignal
                        should handle a missed check-in.
                    </p>
                </div>

                <% if (settingsMessage != null) { %>
                    <div class="notice">
                        <%= HtmlUtil.escape(settingsMessage) %>
                    </div>
                <% } %>

                <% if (settingsError != null) { %>
                    <div class="notice error">
                        <%= HtmlUtil.escape(settingsError) %>
                    </div>
                <% } %>

                <form action="${pageContext.request.contextPath}/SettingsServlet"
                      method="post">

                    <input type="hidden" name="csrfToken"
                           value="<%= HtmlUtil.escape(
                                   (String) session.getAttribute("csrfToken")) %>">

                    <section class="panel">
                        <h2>Monitoring schedule</h2>

                        <div class="setting-row">
                            <div>
                                <strong>Enable monitoring</strong>
                                <small>
                                    Turn your check-in routine on or off.
                                </small>
                            </div>

                            <label class="status">
                                <input type="checkbox"
                                       name="monitoringEnabled"
                                       value="true"
                                       <%= settings.isEnabled()
                                               ? "checked" : "" %>>
                                Enabled
                            </label>
                        </div>

                        <div class="field" style="margin-top:20px">
                            <label for="interval">Check-in interval</label>

                            <select class="control" id="interval"
                                    name="intervalHours" required>

                                <option value="1"
                                    <%= settings.getIntervalHours() == 1
                                            ? "selected" : "" %>>
                                    Every 1 minute (testing)
                                </option>

                                <option value="4"
                                    <%= settings.getIntervalHours() == 4
                                            ? "selected" : "" %>>
                                    Every 4 hours
                                </option>

                                <option value="8"
                                    <%= settings.getIntervalHours() == 8
                                            ? "selected" : "" %>>
                                    Every 8 hours
                                </option>

                                <option value="12"
                                    <%= settings.getIntervalHours() == 12
                                            ? "selected" : "" %>>
                                    Every 12 hours
                                </option>

                                <option value="24"
                                    <%= settings.getIntervalHours() == 24
                                            ? "selected" : "" %>>
                                    Every 24 hours
                                </option>

                                <option value="48"
                                    <%= settings.getIntervalHours() == 48
                                            ? "selected" : "" %>>
                                    Every 48 hours
                                </option>
                            </select>
                        </div>

                        <div class="field">
                            <label for="grace">Grace period</label>

                            <select class="control" id="grace"
                                    name="graceMinutes" required>

                                <option value="15"
                                    <%= settings.getGraceMinutes() == 15
                                            ? "selected" : "" %>>
                                    15 minutes
                                </option>

                                <option value="30"
                                    <%= settings.getGraceMinutes() == 30
                                            ? "selected" : "" %>>
                                    30 minutes
                                </option>

                                <option value="60"
                                    <%= settings.getGraceMinutes() == 60
                                            ? "selected" : "" %>>
                                    1 hour
                                </option>

                                <option value="120"
                                    <%= settings.getGraceMinutes() == 120
                                            ? "selected" : "" %>>
                                    2 hours
                                </option>

                                <option value="240"
                                    <%= settings.getGraceMinutes() == 240
                                            ? "selected" : "" %>>
                                    4 hours
                                </option>
                            </select>
                        </div>

                        <div class="field">
                            <label for="delay">
                                Delay between contact alerts
                            </label>

                            <select class="control" id="delay"
                                    name="escalationDelay" required>

                                <option value="15"
                                    <%= settings.getEscalationDelay() == 15
                                            ? "selected" : "" %>>
                                    15 minutes
                                </option>

                                <option value="30"
                                    <%= settings.getEscalationDelay() == 30
                                            ? "selected" : "" %>>
                                    30 minutes
                                </option>

                                <option value="60"
                                    <%= settings.getEscalationDelay() == 60
                                            ? "selected" : "" %>>
                                    1 hour
                                </option>

                                <option value="120"
                                    <%= settings.getEscalationDelay() == 120
                                            ? "selected" : "" %>>
                                    2 hours
                                </option>
                            </select>
                        </div>

                        <div class="form-actions">
                            <button class="btn primary" type="submit">
                                Save settings
                            </button>

                            <a class="btn outline"
                               href="${pageContext.request.contextPath}/dashboard.jsp">
                                Cancel
                            </a>
                        </div>

                        <p class="hint">
                            After a check-in becomes overdue, the grace
                            period begins. If no confirmation is received,
                            active contacts are notified in priority order.
                            The escalation delay is the wait between
                            each contact alert.
                        </p>
                    </section>
                </form>

                <section class="panel">
                    <h2>How escalation works</h2>

                    <div class="setting-row">
                        <div>
                            <strong>1. Check-in becomes due</strong>
                            <small>
                                You receive a reminder to confirm that you are safe.
                            </small>
                        </div>
                    </div>

                    <div class="setting-row">
                        <div>
                            <strong>2. Grace period expires</strong>
                            <small>
                                Priority 1 contact is selected for notification.
                            </small>
                        </div>
                    </div>

                    <div class="setting-row">
                        <div>
                            <strong>3. Escalation continues</strong>
                            <small>
                                Priority 2 and then Priority 3 are notified
                                after the configured delay if escalation
                                remains unresolved.
                            </small>
                        </div>
                    </div>
                </section>
            </main>

            <footer class="footer">
                LastSignal · Check-in preferences
            </footer>
        </div>
    </body>
</html>
