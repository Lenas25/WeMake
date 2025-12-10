package com.utp.wemake.utils;

import android.util.Log;

import com.utp.wemake.BuildConfig;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {
    private static final String SENDER_EMAIL = BuildConfig.EMAIL_SENDER;
    private static final String SENDER_PASSWORD = BuildConfig.EMAIL_SECRET;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public static void sendInvitationEmail(String recipientEmail, String userName, String boardName, EmailCallback callback) {
        executor.execute(() -> {
            try {
                if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                    callback.onFailure("El usuario no tiene un correo válido.");
                    return;
                }

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "WeMake App"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject("¡Invitación a colaborar en WeMake!");

                String htmlContent = getHtmlTemplate(userName, boardName);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                Log.d("EmailService", "Correo enviado exitosamente a " + recipientEmail);
                callback.onSuccess();

            } catch (AuthenticationFailedException e) {
                Log.e("EmailService", "Error de autenticación", e);
                callback.onFailure("Error: Credenciales de correo inválidas.");
            } catch (SendFailedException e) {
                Log.e("EmailService", "Error al enviar", e);
                callback.onFailure("No se pudo entregar el correo (dirección inválida o bloqueada).");
            } catch (MessagingException e) {
                Log.e("EmailService", "Error general de correo", e);
                callback.onFailure("Error al enviar el correo: " + e.getMessage());
            } catch (Exception e) {
                Log.e("EmailService", "Error inesperado", e);
                callback.onFailure("Ocurrió un error inesperado al enviar el correo.");
            }
        });
    }

    private static String getHtmlTemplate(String userName, String boardName) {
        String colorPrimary = "#445E91";       // md_theme_primary
        String colorBackground = "#F9F9FF";    // md_theme_background
        String colorSurface = "#FFFFFF";       // md_theme_surfaceContainerLowest
        String colorTextMain = "#1A1B20";      // md_theme_onSurface
        String colorTextSecondary = "#46483C"; // md_theme_onSurfaceVariant
        String colorFooterBg = "#EEEDF4";      // md_theme_surfaceContainer

        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: \"Roboto\", Arial, sans-serif; margin: 0; padding: 0; background-color: " + colorBackground + ";'>" +
                "  <div style='max-width: 600px; margin: 20px auto; background-color: " + colorSurface + "; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); border: 1px solid #E3E4D3;'>" +

                "    " +
                "    <div style='background-color: " + colorPrimary + "; padding: 36px 30px; text-align: center;'>" +
                "      <h1 style='color: #FFFFFF; margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 1px;'>WeMake</h1>" +
                "    </div>" +

                "    " +
                "    <div style='padding: 40px 30px; color: " + colorTextMain + ";'>" +
                "      <h2 style='color: " + colorPrimary + "; margin-top: 0; font-size: 22px;'>¡Hola, " + userName + "!</h2>" +
                "      <p style='font-size: 16px; line-height: 1.6; color: " + colorTextMain + ";'>Has sido invitado a formar parte del equipo en el tablero:</p>" +

                "      " +
                "      <div style='background-color: " + colorBackground + "; border-left: 4px solid " + colorPrimary + "; padding: 15px 20px; margin: 20px 0; border-radius: 4px;'>" +
                "        <p style='margin: 0; font-weight: bold; font-size: 18px; color: " + colorPrimary + ";'>" + boardName + "</p>" +
                "      </div>" +

                "      <p style='font-size: 16px; line-height: 1.6;'>Ahora puedes colaborar, gestionar tareas y alcanzar tus objetivos junto a tu equipo.</p>" +

                "      " +
                "      <div style='text-align: center; margin: 35px 0;'>" +
                "        <a href='https://play.google.com/store/apps' style='background-color: " + colorPrimary + "; color: #FFFFFF; padding: 16px 32px; text-decoration: none; border-radius: 100px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 2px 5px rgba(0,0,0,0.2);'>Ingresar al Tablero</a>" +
                "      </div>" +

                "      <p style='font-size: 14px; color: " + colorTextSecondary + "; text-align: center; margin-top: 30px;'>¿No tienes la app instalada? El botón te llevará a la tienda.</p>" +
                "    </div>" +

                "    " +
                "    <div style='background-color: " + colorFooterBg + "; padding: 24px; text-align: center; font-size: 12px; color: " + colorTextSecondary + "; border-top: 1px solid #E2E2E9;'>" +
                "      <p style='margin: 5px 0;'>Estás recibiendo este correo porque fuiste añadido a un equipo en WeMake.</p>" +
                "      <p style='margin: 5px 0; font-weight: bold;'>© 2024 WeMake Project - UTP</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}