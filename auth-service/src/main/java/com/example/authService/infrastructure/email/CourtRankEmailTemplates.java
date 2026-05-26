package com.example.authService.infrastructure.email;

import java.time.Year;

public final class CourtRankEmailTemplates {
    private CourtRankEmailTemplates() {
    }

    public static EmailTemplate verificationEmail(String link, String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return new EmailTemplate(
                    "Verify your CourtRank account",
                    verifyEmailEn(link)
            );
        }

        return new EmailTemplate(
                "Verifica tu cuenta de CourtRank",
                verifyEmailEs(link)
        );
    }

    public static EmailTemplate passwordReset(String otp, String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return new EmailTemplate(
                    "Reset your CourtRank password",
                    resetPasswordEn(otp)
            );
        }

        return new EmailTemplate(
                "Recupera tu contraseña de CourtRank",
                resetPasswordEs(otp)
        );
    }

    private static String verifyEmailEs(String link) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Verifica tu correo</title>
                  <style>
                    body { margin:0; padding:0; background-color:#F7F7FB; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; }
                    .wrapper { background-color:#F7F7FB; padding:40px 0; }
                    .card { background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #D0D0E8; max-width:560px; margin:0 auto; }
                    .header { background-color:#1A1A2E; padding:36px 40px; text-align:center; }
                    .header-brand { font-size:28px; font-weight:900; letter-spacing:2px; text-transform:uppercase; color:#FF5C00; margin:0; }
                    .header-sub { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:2px; color:#5A5A7A; margin:6px 0 0; }
                    .body { padding:40px 40px 32px; background-color:#ffffff; }
                    .heading { font-size:22px; font-weight:800; text-transform:uppercase; letter-spacing:1px; color:#1A1A2E; margin:0 0 12px; }
                    .text { font-size:15px; line-height:1.7; color:#5A5A7A; margin:0 0 32px; }
                    .btn-wrapper { text-align:center; padding:0 0 32px; }
                    .btn { display:inline-block; background-color:#FF5C00; color:#ffffff; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; font-size:15px; font-weight:700; text-decoration:none; border-radius:10px; padding:14px 40px; letter-spacing:0.5px; }
                    .link-fallback { font-size:12px; line-height:1.6; color:#5A5A7A; margin:0 0 24px; word-break:break-all; }
                    .link-fallback a { color:#FF5C00; text-decoration:none; }
                    .note { font-size:13px; line-height:1.6; color:#8A8A9A; margin:0; }
                    .footer { background-color:#EEEEF8; padding:20px 40px; border-top:1px solid #D0D0E8; text-align:center; }
                    .footer-text { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:1px; color:#8A8A9A; margin:0; }
                    .accent { color:#FF5C00; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <table class="card" width="560" cellpadding="0" cellspacing="0" align="center">

                      <tr>
                        <td class="header">
                          <p class="header-brand">CourtRank</p>
                          <p class="header-sub">Plataforma de Reserva de Canchas</p>
                        </td>
                      </tr>

                      <tr>
                        <td class="body">
                          <h2 class="heading">Verifica tu correo</h2>
                          <p class="text">
                            Haz clic en el botón para confirmar tu cuenta e ingresar tu contraseña. El enlace es válido por <strong style="color:#1A1A2E;">1 hora</strong>.
                          </p>

                          <div class="btn-wrapper">
                            <a href="%s" class="btn">Verificar mi cuenta</a>
                          </div>

                          <p class="link-fallback">
                            Si el botón no funciona, copia este enlace en tu navegador:<br/>
                            <a href="%s">%s</a>
                          </p>

                          <p class="note">
                            Si no creaste una cuenta en CourtRank, puedes ignorar este correo.
                          </p>
                        </td>
                      </tr>

                      <tr>
                        <td class="footer">
                          <p class="footer-text">
                            &copy; %d <span class="accent">CourtRank</span> &nbsp;&bull;&nbsp; Mensaje automático — no respondas
                          </p>
                        </td>
                      </tr>

                    </table>
                  </div>
                </body>
                </html>""".formatted(link, link, link, Year.now().getValue());
    }

    private static String verifyEmailEn(String link) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Verify your email</title>
                  <style>
                    body { margin:0; padding:0; background-color:#F7F7FB; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; }
                    .wrapper { background-color:#F7F7FB; padding:40px 0; }
                    .card { background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #D0D0E8; max-width:560px; margin:0 auto; }
                    .header { background-color:#1A1A2E; padding:36px 40px; text-align:center; }
                    .header-brand { font-size:28px; font-weight:900; letter-spacing:2px; text-transform:uppercase; color:#FF5C00; margin:0; }
                    .header-sub { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:2px; color:#5A5A7A; margin:6px 0 0; }
                    .body { padding:40px 40px 32px; background-color:#ffffff; }
                    .heading { font-size:22px; font-weight:800; text-transform:uppercase; letter-spacing:1px; color:#1A1A2E; margin:0 0 12px; }
                    .text { font-size:15px; line-height:1.7; color:#5A5A7A; margin:0 0 32px; }
                    .btn-wrapper { text-align:center; padding:0 0 32px; }
                    .btn { display:inline-block; background-color:#FF5C00; color:#ffffff; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; font-size:15px; font-weight:700; text-decoration:none; border-radius:10px; padding:14px 40px; letter-spacing:0.5px; }
                    .link-fallback { font-size:12px; line-height:1.6; color:#5A5A7A; margin:0 0 24px; word-break:break-all; }
                    .link-fallback a { color:#FF5C00; text-decoration:none; }
                    .note { font-size:13px; line-height:1.6; color:#8A8A9A; margin:0; }
                    .footer { background-color:#EEEEF8; padding:20px 40px; border-top:1px solid #D0D0E8; text-align:center; }
                    .footer-text { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:1px; color:#8A8A9A; margin:0; }
                    .accent { color:#FF5C00; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <table class="card" width="560" cellpadding="0" cellspacing="0" align="center">

                      <tr>
                        <td class="header">
                          <p class="header-brand">CourtRank</p>
                          <p class="header-sub">Court Booking Platform</p>
                        </td>
                      </tr>

                      <tr>
                        <td class="body">
                          <h2 class="heading">Verify your email</h2>
                          <p class="text">
                            Click the button below to confirm your account and set your password. The link is valid for <strong style="color:#1A1A2E;">1 hour</strong>.
                          </p>

                          <div class="btn-wrapper">
                            <a href="%s" class="btn">Verify my account</a>
                          </div>

                          <p class="link-fallback">
                            If the button doesn't work, copy this link into your browser:<br/>
                            <a href="%s">%s</a>
                          </p>

                          <p class="note">
                            If you didn't create a CourtRank account, you can safely ignore this email.
                          </p>
                        </td>
                      </tr>

                      <tr>
                        <td class="footer">
                          <p class="footer-text">
                            &copy; %d <span class="accent">CourtRank</span> &nbsp;&bull;&nbsp; Automated message — do not reply
                          </p>
                        </td>
                      </tr>

                    </table>
                  </div>
                </body>
                </html>""".formatted(link, link, link, Year.now().getValue());
    }

    private static String resetPasswordEs(String otp) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Recupera tu contraseña</title>
                  <style>
                    body { margin:0; padding:0; background-color:#F7F7FB; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; }
                    .wrapper { background-color:#F7F7FB; padding:40px 0; }
                    .card { background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #D0D0E8; max-width:560px; margin:0 auto; }
                    .header { background-color:#1A1A2E; padding:36px 40px; text-align:center; }
                    .header-brand { font-size:28px; font-weight:900; letter-spacing:2px; text-transform:uppercase; color:#FF5C00; margin:0; }
                    .header-sub { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:2px; color:#5A5A7A; margin:6px 0 0; }
                    .body { padding:40px 40px 32px; background-color:#ffffff; }
                    .heading { font-size:22px; font-weight:800; text-transform:uppercase; letter-spacing:1px; color:#1A1A2E; margin:0 0 12px; }
                    .text { font-size:15px; line-height:1.7; color:#5A5A7A; margin:0 0 28px; }
                    .otp-wrapper { text-align:center; padding:0 0 28px; }
                    .otp-box { display:inline-block; background-color:#EEEEF8; border:1.5px solid #D0D0E8; border-radius:12px; padding:20px 48px; }
                    .otp-label { font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:2px; color:#5A5A7A; margin:0 0 10px; }
                    .otp-code { font-size:48px; font-weight:900; letter-spacing:10px; color:#FF5C00; display:block; font-variant-numeric:tabular-nums; }
                    .otp-expiry { font-size:12px; font-weight:600; color:#8A8A9A; text-transform:uppercase; letter-spacing:1px; margin:10px 0 0; }
                    .warning { background-color:#EEEEF8; border-left:3px solid #FF5C00; border-radius:0 6px 6px 0; padding:14px 16px; margin-top:4px; }
                    .warning-text { font-size:13px; line-height:1.6; color:#5A5A7A; margin:0; }
                    .footer { background-color:#EEEEF8; padding:20px 40px; border-top:1px solid #D0D0E8; text-align:center; }
                    .footer-text { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:1px; color:#8A8A9A; margin:0; }
                    .accent { color:#FF5C00; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <table class="card" width="560" cellpadding="0" cellspacing="0" align="center">

                      <tr>
                        <td class="header">
                          <p class="header-brand">CourtRank</p>
                          <p class="header-sub">Plataforma de Reserva de Canchas</p>
                        </td>
                      </tr>

                      <tr>
                        <td class="body">
                          <h2 class="heading">Recuperación de contraseña</h2>
                          <p class="text">
                            Recibimos una solicitud para restablecer tu contraseña. Usa el código a continuación — expira en <strong style="color:#1A1A2E;">30 minutos</strong>.
                          </p>

                          <div class="otp-wrapper">
                            <div class="otp-box">
                              <p class="otp-label">Código de recuperación</p>
                              <span class="otp-code">%s</span>
                              <p class="otp-expiry">Expira en 30 min</p>
                            </div>
                          </div>

                          <div class="warning">
                            <p class="warning-text">
                              <strong style="color:#1A1A2E;">¿No solicitaste esto?</strong>
                              Tu cuenta está segura — no se realizará ningún cambio a menos que uses este código.
                            </p>
                          </div>
                        </td>
                      </tr>

                      <tr>
                        <td class="footer">
                          <p class="footer-text">
                            &copy; %d <span class="accent">CourtRank</span> &nbsp;&bull;&nbsp; Mensaje automático — no respondas
                          </p>
                        </td>
                      </tr>

                    </table>
                  </div>
                </body>
                </html>""".formatted(otp, Year.now().getValue());
    }

    private static String resetPasswordEn(String otp) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Reset your password</title>
                  <style>
                    body { margin:0; padding:0; background-color:#F7F7FB; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; }
                    .wrapper { background-color:#F7F7FB; padding:40px 0; }
                    .card { background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #D0D0E8; max-width:560px; margin:0 auto; }
                    .header { background-color:#1A1A2E; padding:36px 40px; text-align:center; }
                    .header-brand { font-size:28px; font-weight:900; letter-spacing:2px; text-transform:uppercase; color:#FF5C00; margin:0; }
                    .header-sub { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:2px; color:#5A5A7A; margin:6px 0 0; }
                    .body { padding:40px 40px 32px; background-color:#ffffff; }
                    .heading { font-size:22px; font-weight:800; text-transform:uppercase; letter-spacing:1px; color:#1A1A2E; margin:0 0 12px; }
                    .text { font-size:15px; line-height:1.7; color:#5A5A7A; margin:0 0 28px; }
                    .otp-wrapper { text-align:center; padding:0 0 28px; }
                    .otp-box { display:inline-block; background-color:#EEEEF8; border:1.5px solid #D0D0E8; border-radius:12px; padding:20px 48px; }
                    .otp-label { font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:2px; color:#5A5A7A; margin:0 0 10px; }
                    .otp-code { font-size:48px; font-weight:900; letter-spacing:10px; color:#FF5C00; display:block; font-variant-numeric:tabular-nums; }
                    .otp-expiry { font-size:12px; font-weight:600; color:#8A8A9A; text-transform:uppercase; letter-spacing:1px; margin:10px 0 0; }
                    .warning { background-color:#EEEEF8; border-left:3px solid #FF5C00; border-radius:0 6px 6px 0; padding:14px 16px; margin-top:4px; }
                    .warning-text { font-size:13px; line-height:1.6; color:#5A5A7A; margin:0; }
                    .footer { background-color:#EEEEF8; padding:20px 40px; border-top:1px solid #D0D0E8; text-align:center; }
                    .footer-text { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:1px; color:#8A8A9A; margin:0; }
                    .accent { color:#FF5C00; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <table class="card" width="560" cellpadding="0" cellspacing="0" align="center">

                      <tr>
                        <td class="header">
                          <p class="header-brand">CourtRank</p>
                          <p class="header-sub">Court Booking Platform</p>
                        </td>
                      </tr>

                      <tr>
                        <td class="body">
                          <h2 class="heading">Password reset</h2>
                          <p class="text">
                            We received a request to reset your password. Use the code below to proceed — it expires in <strong style="color:#1A1A2E;">30 minutes</strong>.
                          </p>

                          <div class="otp-wrapper">
                            <div class="otp-box">
                              <p class="otp-label">Reset code</p>
                              <span class="otp-code">%s</span>
                              <p class="otp-expiry">Expires in 30 min</p>
                            </div>
                          </div>

                          <div class="warning">
                            <p class="warning-text">
                              <strong style="color:#1A1A2E;">Didn't request this?</strong>
                              Your account is safe — no changes will be made unless you use this code.
                            </p>
                          </div>
                        </td>
                      </tr>

                      <tr>
                        <td class="footer">
                          <p class="footer-text">
                            &copy; %d <span class="accent">CourtRank</span> &nbsp;&bull;&nbsp; Automated message — do not reply
                          </p>
                        </td>
                      </tr>

                    </table>
                  </div>
                </body>
                </html>""".formatted(otp, Year.now().getValue());
    }
}
