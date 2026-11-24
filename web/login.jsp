
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="util.HtmlUtil,util.CsrfUtil" %>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String csrfToken = CsrfUtil.getOrCreateToken(request);
    String registrationMessage = (String) session.getAttribute("registrationMessage");
    String error = (String) request.getAttribute("error");

    if (registrationMessage != null) {
        session.removeAttribute("registrationMessage");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f7f6f1">
    <title>Sign in | LastSignal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="${pageContext.request.contextPath}/js/app.js" defer></script>
</head>
<body class="auth-page">

<div class="auth-layout">

    <aside class="auth-aside">
        <a class="auth-brand" href="${pageContext.request.contextPath}/index.html">
            LastSignal<span>.</span>
        </a>

        <div class="auth-story">
            <div class="auth-kicker">A LITTLE PEACE OF MIND</div>

            <h1>
                Stay close.<br>
                <span>Even from afar.</span>
            </h1>

            <p>
                A simple check-in system that helps the people
                you trust know you're safe.
            </p>

            <div class="auth-aside-note">
                <span class="auth-note-line"></span>
                <span>Because being connected matters.</span>
            </div>
        </div>

        <div class="auth-aside-footer">
            LASTSIGNAL &nbsp;·&nbsp; YOUR SAFETY CHECK-IN COMPANION
        </div>

        <div class="auth-orbit auth-orbit-one"></div>
        <div class="auth-orbit auth-orbit-two"></div>
        <div class="auth-orbit auth-orbit-three"></div>
    </aside>

    <main class="auth-main">
        <section class="auth-card">

            <div class="auth-mobile-brand">
                <a class="auth-brand" href="${pageContext.request.contextPath}/index.html">
                    LastSignal<span>.</span>
                </a>
            </div>

            <div class="auth-card-top">
                <span class="auth-kicker">WELCOME BACK</span>
                <span class="auth-step">01 / 01</span>
            </div>

            <h2>Good to have you back.</h2>
            <p class="auth-description">
                Sign in to manage your check-ins and stay connected
                with your trusted circle.
            </p>

            <% if (registrationMessage != null) { %>
                <div class="notice" role="status">
                    <%= HtmlUtil.escape(registrationMessage) %>
                </div>
            <% } %>

            <% if (error != null) { %>
                <div class="notice error" role="alert">
                    <%= HtmlUtil.escape(error) %>
                </div>
            <% } %>

            <form action="${pageContext.request.contextPath}/login"
                  method="post"
                  autocomplete="off"
                  data-auth-form>

                <input type="hidden" name="csrfToken"
                       value="<%= HtmlUtil.escape(csrfToken) %>">

                <div class="field auth-field">
                    <label for="email">Email address</label>
                    <input class="control auth-control"
                           id="email"
                           type="email"
                           name="email"
                           maxlength="254"
                           autocomplete="off"
                           autocapitalize="none"
                           spellcheck="false"
                           data-auth-field
                           readonly
                           placeholder="Enter your email address"
                           required>
                </div>

                <div class="field auth-field">
                    <label for="password">Password</label>

                    <div class="password auth-password">
                        <input class="control auth-control"
                               id="password"
                               type="password"
                               name="password"
                               maxlength="128"
                               autocomplete="off"
                               data-auth-field
                               readonly
                               placeholder="Enter your password"
                               required>

                        <button class="show-password"
                                type="button"
                                data-password-toggle="password"
                                aria-label="Show password">
                            Show
                        </button>
                    </div>
                </div>

                <button class="btn primary full auth-submit" type="submit">
                    Sign in <span aria-hidden="true">→</span>
                </button>
            </form>

            <div class="auth-foot">
                <span>New to LastSignal?</span>
                <a href="${pageContext.request.contextPath}/register.jsp">
                    Create an account
                </a>
            </div>

            <div class="auth-privacy">
                <span class="auth-privacy-icon">✓</span>
                Your account information stays private.
            </div>

        </section>
    </main>

</div>
</body>
</html>
