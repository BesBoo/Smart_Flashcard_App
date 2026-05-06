using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Sends emails via SMTP using MailKit (replaces deprecated System.Net.Mail).
/// MailKit provides proper timeout control, modern TLS handling, and reliability
/// on cloud platforms like Render where the old SmtpClient often hangs.
/// </summary>
public class EmailService
{
    private readonly IConfiguration _config;
    private readonly ILogger<EmailService> _logger;

    /// <summary>Connection + send timeout in seconds (default 15s to avoid Render request timeouts)</summary>
    private const int SmtpTimeoutSeconds = 15;

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

    /// <summary>Send email via SMTP using MailKit with proper timeout handling.</summary>
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

        // Build MIME message
        var message = new MimeMessage();
        message.From.Add(MailboxAddress.Parse(from));
        message.To.Add(MailboxAddress.Parse(to));
        message.Subject = subject;
        message.Body = new TextPart(isHtml ? "html" : "plain") { Text = body };

        // Use CancellationToken with timeout to prevent hanging
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(SmtpTimeoutSeconds));

        try
        {
            using var client = new MailKit.Net.Smtp.SmtpClient();

            // Set timeout on the underlying socket operations
            client.Timeout = SmtpTimeoutSeconds * 1000; // milliseconds

            _logger.LogInformation("Connecting to SMTP {Host}:{Port}...", host, port);

            // Connect with STARTTLS (port 587) or auto-detect
            await client.ConnectAsync(host, port, SecureSocketOptions.StartTls, cts.Token);

            // Authenticate
            await client.AuthenticateAsync(username, password, cts.Token);

            // Send
            await client.SendAsync(message, cts.Token);
            await client.DisconnectAsync(true, cts.Token);

            _logger.LogInformation("Email sent successfully to {To}: {Subject}", to, subject);
        }
        catch (OperationCanceledException)
        {
            _logger.LogError("SMTP timeout after {Seconds}s sending to {To}", SmtpTimeoutSeconds, to);
            throw new InvalidOperationException(
                $"Gửi email bị timeout sau {SmtpTimeoutSeconds} giây. Máy chủ SMTP không phản hồi.");
        }
        catch (MailKit.Security.AuthenticationException ex)
        {
            _logger.LogError(ex, "SMTP authentication failed for {Username}", username);
            throw new InvalidOperationException(
                "Xác thực SMTP thất bại. Vui lòng kiểm tra cấu hình email.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send email to {To}: {Error}", to, ex.Message);
            throw new InvalidOperationException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}
