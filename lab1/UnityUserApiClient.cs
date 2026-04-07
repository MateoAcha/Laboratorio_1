using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace GameApiClient
{
    public sealed class UserApiClient
    {
        private static readonly JsonSerializerOptions JsonOptions = new()
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase
        };

        private readonly HttpClient _httpClient;

        public UserApiClient(string baseUrl)
        {
            _httpClient = new HttpClient
            {
                BaseAddress = new Uri(baseUrl.TrimEnd('/') + "/")
            };
        }

        public async Task<UserResponse> CreateUserAsync(
            string username,
            string email,
            string password,
            bool isPremium = false,
            DateTime? premiumUntilUtc = null,
            int? userId = null,
            CancellationToken cancellationToken = default)
        {
            var nowUtc = DateTime.UtcNow;

            var payload = new CreateUserRequest
            {
                UserId = userId ?? GenerateUserId(),
                Username = username,
                Email = email,
                IsPremium = isPremium,
                PremiumSince = isPremium ? nowUtc : null,
                PremiumUntil = isPremium ? premiumUntilUtc : null,
                Password = password
            };

            var json = JsonSerializer.Serialize(payload, JsonOptions);
            using var content = new StringContent(json, Encoding.UTF8, "application/json");
            using var response = await _httpClient.PostAsync("users", content, cancellationToken);
            var responseBody = await response.Content.ReadAsStringAsync(cancellationToken);

            if (!response.IsSuccessStatusCode)
            {
                throw new InvalidOperationException(
                    $"CreateUser failed ({(int)response.StatusCode}): {responseBody}");
            }

            var createdUser = JsonSerializer.Deserialize<UserResponse>(responseBody, JsonOptions);
            if (createdUser == null)
            {
                throw new InvalidOperationException("CreateUser returned an empty response.");
            }

            return createdUser;
        }

        private static int GenerateUserId()
        {
            var unixSeconds = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            return (int)(unixSeconds % int.MaxValue);
        }
    }

    public sealed class CreateUserRequest
    {
        public int UserId { get; set; }
        public string Username { get; set; } = "";
        public string Email { get; set; } = "";
        public bool IsPremium { get; set; }
        public DateTime? PremiumSince { get; set; }
        public DateTime? PremiumUntil { get; set; }
        public string Password { get; set; } = "";
    }

    public sealed class UserResponse
    {
        public int UserId { get; set; }
        public string Username { get; set; } = "";
        public string Email { get; set; } = "";
        public bool IsPremium { get; set; }
        public DateTime? PremiumSince { get; set; }
        public DateTime? PremiumUntil { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    public static class CreateUserDemo
    {
        public static async Task RunAsync()
        {
            var client = new UserApiClient("http://localhost:8080");

            var created = await client.CreateUserAsync(
                username: "unity_player_01",
                email: "unity_player_01@example.com",
                password: "pass1234",
                isPremium: false);

            Console.WriteLine(
                $"Created user #{created.UserId} ({created.Username}) at {created.CreatedAt:O}");
        }
    }
}
