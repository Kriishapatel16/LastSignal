
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="util.HtmlUtil,util.CsrfUtil" %>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String csrfToken = CsrfUtil.getOrCreateToken(request);
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f7f6f1">
    <title>Create Account | LastSignal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="${pageContext.request.contextPath}/js/app.js" defer></script>
</head>
<body class="auth-page">

<div class="auth-layout">

    <aside class="auth-aside register-aside">
        <a class="auth-brand" href="${pageContext.request.contextPath}/index.html">
            LastSignal<span>.</span>
        </a>

        <div class="auth-story">
            <div class="auth-kicker">START WITH A SIMPLE STEP</div>

            <h1>
                A little check-in.<br>
                <span>A lot of reassurance.</span>
            </h1>

            <p>
                Build your safety routine, choose your check-in
                schedule and add the people you trust.
            </p>

            <div class="auth-aside-note">
                <span class="auth-note-line"></span>
                <span>Your trusted circle starts here.</span>
            </div>
        </div>

        <div class="auth-aside-footer">
            LASTSIGNAL &nbsp;·&nbsp; STAY CONNECTED
        </div>

        <div class="auth-orbit auth-orbit-one"></div>
        <div class="auth-orbit auth-orbit-two"></div>
        <div class="auth-orbit auth-orbit-three"></div>
    </aside>

    <main class="auth-main auth-main-register">
        <section class="auth-card auth-card-register">

            <div class="auth-mobile-brand">
                <a class="auth-brand" href="${pageContext.request.contextPath}/index.html">
                    LastSignal<span>.</span>
                </a>
            </div>

            <div class="auth-card-top">
                <span class="auth-kicker">GET STARTED</span>
                <span class="auth-step">01 / 01</span>
            </div>

            <h2>Create your account.</h2>
            <p class="auth-description">
                Tell us a little about yourself to get started.
            </p>

            <% if (error != null) { %>
                <div class="notice error" role="alert">
                    <%= HtmlUtil.escape(error) %>
                </div>
            <% } %>

            <form action="${pageContext.request.contextPath}/register"
                  method="post"
                  autocomplete="off"
                  data-auth-form>

                <input type="hidden" name="csrfToken"
                       value="<%= HtmlUtil.escape(csrfToken) %>">

                <div class="field auth-field">
                    <label for="name">Full name</label>
                    <input class="control auth-control"
                           id="name"
                           type="text"
                           name="name"
                           minlength="2"
                           maxlength="100"
                           autocomplete="off"
                           data-auth-field
                           readonly
                           placeholder="Your full name"
                           required>
                </div>

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
                           placeholder="you@example.com"
                           required>
                </div>

                <div class="field auth-field">
                    <label for="phone">
                        Phone number <span class="auth-optional">Optional</span>
                    </label>
                    <input class="control auth-control"
                           id="phone"
                           type="tel"
                           name="phone"
                           maxlength="20"
                           autocomplete="off"
                           data-auth-field
                           readonly
                           placeholder="Your phone number">
                </div>

                <div class="field auth-field">
                    <label for="password">Password</label>
                    <div class="password auth-password">
                        <input class="control auth-control"
                               id="password"
                               type="password"
                               name="password"
                               minlength="8"
                               maxlength="128"
                               autocomplete="off"
                               data-auth-field
                               readonly
                               placeholder="At least 8 characters"
                               required>

                        <button class="show-password"
                                type="button"
                                data-password-toggle="password"
                                aria-label="Show password">
                            Show
                        </button>
                    </div>
                </div>

                <div class="field auth-field">
                    <label for="confirmPassword">Confirm password</label>
                    <div class="password auth-password">
                        <input class="control auth-control"
                               id="confirmPassword"
                               type="password"
                               name="confirmPassword"
                               minlength="8"
                               maxlength="128"
                               autocomplete="off"
                               data-auth-field
                               readonly
                               placeholder="Enter your password again"
                               required>

                        <button class="show-password"
                                type="button"
                                data-password-toggle="confirmPassword"
                                aria-label="Show confirmation password">
                            Show
                        </button>
                    </div>
                </div>

                <button class="btn primary full auth-submit" type="submit">
                    Create account <span aria-hidden="true">→</span>
                </button>
            </form>

            <div class="auth-foot">
                <span>Already have an account?</span>
                <a href="${pageContext.request.contextPath}/login.jsp">
                    Sign in
                </a>
            </div>

            <div class="auth-privacy">
                <span class="auth-privacy-icon">✓</span>
                Your details are kept private.
            </div>

        </section>
    </main>

</div>
</body>
</html>
