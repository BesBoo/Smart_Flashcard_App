namespace SmartFlashcardAPI.Security;

/// <summary>JWT short claim names used when MapInboundClaims is false (.NET 8+ JwtBearer default).</summary>
public static class JwtClaimNames
{
    public const string Sub = "sub";
    public const string Role = "role";
}
