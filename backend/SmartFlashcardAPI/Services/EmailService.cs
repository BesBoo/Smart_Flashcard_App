using System.Net;
using System.Net.Mail;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Sends emails via SMTP. Configured from appsettings.json → Smtp section.
/// </summary>
public class EmailService
{
    private readonly IConfiguration _config;
    private readonly ILogger<EmailService> _logger;

    public EmailService(IConfiguration config, ILogger<EmailService> logger)
    {
        _config = config;
        _logger = logger;
    }

    /// <summary>Send a password reset OTP email.</summary>
    public async Task SendPasswordResetEmailAsync(string toEmail, string otpCode)
    {
        var subject = "Smart Flashcard - Mã xác nhận đặt lại mật khẩu";
        var body = $@"
<html>
<body style='font-family: Arial, sans-serif; background: #0F172A; color: #E2E8F0; padding: 32px;'>
  <div style='max-width: 480px; margin: 0 auto; background: #1E293B; border-radius: 16px; padding: 32px;'>
    <h2 style='color: #60A5FA; text-align: center;'>🔑 Đặt lại mật khẩu</h2>
    <p style='text-align: center; font-size: 16px;'>Mã xác nhận của bạn là:</p>
    <div style='text-align: center; margin: 24px 0;'>
      <span style='font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #38BDF8; 
                   background: #0F172A; padding: 12px 24px; border-radius: 12px; display: inline-block;'>
        {otpCode}
      </span>
    </div>
    <p style='text-align: center; color: #94A3B8; font-size: 14px;'>
      Mã này sẽ hết hạn sau <strong>10 phút</strong>.<br/>
      Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
    </p>
    <hr style='border-color: #334155; margin: 24px 0;'/>
    <p style='text-align: center; color: #64748B; font-size: 12px;'>
      Smart Flashcard © 2026
    </p>
  </div>
</body>
</html>";

        await SendEmailAsync(toEmail, subject, body, isHtml: true);
    }

    /// <summary>Send email via SMTP.</summary>
    private async Task SendEmailAsync(string to, string subject, string body, bool isHtml = false)
    {
        var host = _config["Smtp:Host"] ?? "smtp.gmail.com";
        var port = _config.GetValue("Smtp:Port", 587);
        var username = _config["Smtp:Username"];
        var password = _config["Smtp:Password"];
        var from = _config["Smtp:From"] ?? username;

        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            // Dev mode: log OTP instead of sending email
            _logger.LogWarning("SMTP not configured. Email to {To}: {Subject} | Body preview: {Body}",
                to, subject, body.Length > 100 ? body[..100] : body);
            return;
        }

        try
        {
            using var client = new SmtpClient(host, port)
            {
                Credentials = new NetworkCredential(username, password),
                EnableSsl = true
            };

            var message = new MailMessage(from!, to, subject, body)
            {
                IsBodyHtml = isHtml
            };

            await client.SendMailAsync(message);
            _logger.LogInformation("Email sent to {To}: {Subject}", to, subject);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send email to {To}", to);
            throw new InvalidOperationException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}
