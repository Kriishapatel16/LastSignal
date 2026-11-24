<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="util.HtmlUtil"%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            Check-in | LastSignal
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    </head>
    <body>
        <div class="app">
            <header class="topbar">
                <a class="logo" href="${pageContext.request.contextPath}/dashboard.jsp">
                    LastSignal
                </a>
                <nav class="nav">
                    <a href="${pageContext.request.contextPath}/dashboard.jsp">
                        Dashboard
                    </a>
                    <a href="${pageContext.request.contextPath}/contacts">
                        Trusted Contacts
                    </a>
                    <a href="${pageContext.request.contextPath}/settings.jsp">
                        Settings
                    </a>
                    <form action="${pageContext.request.contextPath}/logout" method="post">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <button>
                            Logout
                        </button>
                    </form>
                </nav>
            </header>
            <main class="content" style="max-width:760px">
                <div class="page-title">
                    <h1>
                        Safety check-in
                    </h1>
                    <p>
                        Let LastSignal know that you are safe.
                    </p>
                </div>
                <section class="panel" style="text-align:center;padding:48px 28px">
                    <span class="status">
                        Your confirmation
                    </span>
                    <h2 style="font:600 30px 'Playfair Display',serif;margin:20px 0 10px">
                        I'm safe
                    </h2>
                    <p class="muted" style="line-height:1.8;max-width:480px;margin:0 auto 25px">
                        Confirming will record your check-in and restart your configured check-in cycle once the check-in functionality is connected.
                    </p>
                    <form action="${pageContext.request.contextPath}/CheckInServlet" method="post">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <button class="btn primary" type="submit">
                            Confirm I'm Safe
                        </button>
                    </form>
                    <div class="form-actions" style="justify-content:center">
                        <a class="btn outline" href="${pageContext.request.contextPath}/dashboard.jsp">
                            Back to dashboard
                        </a>
                    </div>
                </section>
            </main>
            <footer class="footer">
                LastSignal · Your safety matters
            </footer>
        </div>
    </body>
</html>
