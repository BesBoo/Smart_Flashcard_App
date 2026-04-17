using System.Security.Claims;

namespace SmartFlashcardAPI.Security;

public static class ClaimsPrincipalExtensions
{
    /// <summary>Resolves user id from JWT (short <c>sub</c> or mapped <see cref="ClaimTypes.NameIdentifier"/>).</summary>
    public static Guid? TryGetUserId(this ClaimsPrincipal principal)
    {
        var s = principal.FindFirst(JwtClaimNames.Sub)?.Value
            ?? principal.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrWhiteSpace(s)) return null;
        return Guid.TryParse(s, out var id) ? id : null;
    }
}
