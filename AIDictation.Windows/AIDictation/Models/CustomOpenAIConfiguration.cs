using System;
using System.IO;
using Newtonsoft.Json;

namespace AIDictation.Models;

/// <summary>Non-secret defaults loaded from %APPDATA%\AIDictation\custom-openai.json.</summary>
public sealed class CustomOpenAIConfiguration
{
    [JsonProperty("version")] public int Version { get; set; } = 1;
    [JsonProperty("transcription")] public CustomOpenAITranscriptionConfiguration Transcription { get; set; } = new();
    [JsonProperty("cleanup")] public CustomOpenAICleanupConfiguration Cleanup { get; set; } = new();

    public static CustomOpenAIConfiguration LoadDefaults()
    {
        try
        {
            var path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "AIDictation", "custom-openai.json");
            return File.Exists(path)
                ? JsonConvert.DeserializeObject<CustomOpenAIConfiguration>(File.ReadAllText(path)) ?? new()
                : new();
        }
        catch (JsonException) { return new(); }
        catch (IOException) { return new(); }
    }
}

public sealed class CustomOpenAITranscriptionConfiguration
{
    [JsonProperty("baseUrl")] public string BaseUrl { get; set; } = string.Empty;
    [JsonProperty("model")] public string Model { get; set; } = string.Empty;
    [JsonProperty("realtimeUrl")] public string RealtimeUrl { get; set; } = string.Empty;
    [JsonProperty("realtimeModel")] public string RealtimeModel { get; set; } = string.Empty;
}

public sealed class CustomOpenAICleanupConfiguration
{
    [JsonProperty("baseUrl")] public string BaseUrl { get; set; } = string.Empty;
    [JsonProperty("model")] public string Model { get; set; } = string.Empty;
}

public static class CustomOpenAIEndpoint
{
    public static bool TryDerive(string baseUrl, string suffix, out Uri endpoint)
    {
        endpoint = null!;
        if (!Uri.TryCreate(baseUrl.Trim().TrimEnd('/'), UriKind.Absolute, out var baseUri)) return false;
        var local = baseUri.Host.Equals("localhost", StringComparison.OrdinalIgnoreCase) ||
                    baseUri.Host.Equals("127.0.0.1", StringComparison.OrdinalIgnoreCase) ||
                    baseUri.Host.Equals("::1", StringComparison.OrdinalIgnoreCase);
        if (baseUri.Scheme != Uri.UriSchemeHttps && !(local && baseUri.Scheme == Uri.UriSchemeHttp)) return false;
        endpoint = new Uri(baseUri.AbsoluteUri.TrimEnd('/') + suffix);
        return true;
    }

    public static bool IsAllowedRealtimeUrl(string value) =>
        Uri.TryCreate(value, UriKind.Absolute, out var uri) &&
        (uri.Scheme == "wss" || ((uri.Host.Equals("localhost", StringComparison.OrdinalIgnoreCase) || uri.Host == "127.0.0.1" || uri.Host == "::1") && uri.Scheme == "ws"));
}
