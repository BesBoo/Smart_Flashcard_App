using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Sends emails via HTTP API (Resend / SMTP fallback).
/// 
/// Gmail SMTP is blocked from cloud platforms like Render.
/// Resend provides a free HTTP-based email API (100 emails/day free)
/// that works reliably from any server.
///
/// Config (appsettings / env vars):
///   Email:Provider = "Resend" | "Smtp"
///   Email:Resend:ApiKey = "re_..."
///   Email:Resend:From = "MemoHop &lt;noreply@yourdomain.com&gt;"
///   (SMTP fallback uses existing Smtp:* config)
/// </summary>
public class EmailService
{
    private readonly IConfiguration _config;
    private readonly ILogger<EmailService> _logger;
    private readonly IHttpClientFactory _httpClientFactory;

    public EmailService(IConfiguration config, ILogger<EmailService> logger, IHttpClientFactory httpClientFactory)
    {
        _config = config;
        _logger = logger;
        _httpClientFactory = httpClientFactory;
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

    private async Task SendEmailAsync(string to, string subject, string body, bool isHtml = false)
    {
        var provider = _config["Email:Provider"]?.Trim();

        // Use Resend if configured, otherwise fall back to SMTP
        if (string.Equals(provider, "Resend", StringComparison.OrdinalIgnoreCase))
        {
            await SendViaResendAsync(to, subject, body, isHtml);
        }
        else
        {
            await SendViaSmtpAsync(to, subject, body, isHtml);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  RESEND (HTTP API — works from any cloud platform)
    // ─────────────────────────────────────────────────────────

    private async Task SendViaResendAsync(string to, string subject, string body, bool isHtml)
    {
        var apiKey = _config["Email:Resend:ApiKey"];
        var from = _config["Email:Resend:From"] ?? "MemoHop <onboarding@resend.dev>";

        if (string.IsNullOrEmpty(apiKey))
        {
            _logger.LogWarning("Resend API key not configured. Email to {To}: {Subject}", to, subject);
            return;
        }

        var client = _httpClientFactory.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);
        client.Timeout = TimeSpan.FromSeconds(10);

        var payload = new
        {
            from = from,
            to = new[] { to },
            subject = subject,
            html = isHtml ? body : null,
            text = isHtml ? null : body
        };

        var json = JsonSerializer.Serialize(payload);
        var content = new StringContent(json, Encoding.UTF8, "application/json");

        try
        {
            var response = await client.PostAsync("https://api.resend.com/emails", content);
            var responseBody = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                _logger.LogInformation("Email sent via Resend to {To}: {Subject}", to, subject);
            }
            else
            {
                _logger.LogError("Resend API error {Status}: {Body}", response.StatusCode, responseBody);
                throw new InvalidOperationException($"Không thể gửi email (Resend: {response.StatusCode}).");
            }
        }
        catch (TaskCanceledException)
        {
            _logger.LogError("Resend API timeout sending to {To}", to);
            throw new InvalidOperationException("Gửi email bị timeout. Vui lòng thử lại.");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  SMTP FALLBACK (MailKit — for local dev or non-cloud)
    // ─────────────────────────────────────────────────────────

    private async Task SendViaSmtpAsync(string to, string subject, string body, bool isHtml)
    {
        var host = _config["Smtp:Host"] ?? "smtp.gmail.com";
        var port = _config.GetValue("Smtp:Port", 587);
        var username = _config["Smtp:Username"];
        var password = _config["Smtp:Password"];
        var from = _config["Smtp:From"] ?? username;

        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            _logger.LogWarning("SMTP not configured. Email to {To}: {Subject}", to, subject);
            return;
        }

        var message = new MimeKit.MimeMessage();
        message.From.Add(MimeKit.MailboxAddress.Parse(from));
        message.To.Add(MimeKit.MailboxAddress.Parse(to));
        message.Subject = subject;
        message.Body = new MimeKit.TextPart(isHtml ? "html" : "plain") { Text = body };

        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(15));

        try
        {
            using var client = new MailKit.Net.Smtp.SmtpClient();
            client.Timeout = 15_000;

            await client.ConnectAsync(host, port, MailKit.Security.SecureSocketOptions.StartTls, cts.Token);
            await client.AuthenticateAsync(username, password, cts.Token);
            await client.SendAsync(message, cts.Token);
            await client.DisconnectAsync(true, cts.Token);

            _logger.LogInformation("Email sent via SMTP to {To}: {Subject}", to, subject);
        }
        catch (OperationCanceledException)
        {
            _logger.LogError("SMTP timeout after 15s sending to {To}", to);
            throw new InvalidOperationException("Gửi email bị timeout. SMTP server không phản hồi.");
        }
        catch (MailKit.Security.AuthenticationException ex)
        {
            _logger.LogError(ex, "SMTP auth failed for {Username}", username);
            throw new InvalidOperationException("Xác thực SMTP thất bại.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send email via SMTP to {To}", to);
            throw new InvalidOperationException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}
