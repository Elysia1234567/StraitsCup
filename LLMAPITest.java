import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LLMAPITest {
	public static void main(String[] args) {
		try {
			String apiKey = System.getenv("SPRINTAI_API_KEY");
			if (apiKey == null || apiKey.isEmpty()) {
				System.err.println("请先设置环境变量 SPRINTAI_API_KEY（你的 SprintAI API Key）");
				System.exit(1);
			}

			String endpoint = "https://api.sprintai.com/v1/chat/completions";
			String model = "qianwen-1"; // 根据 SprintAI 文档调整模型名
			String userPrompt = "请用中文说: HelloWorld";

			String jsonBody = "{"
					+ "\"model\":\"" + model + "\"," 
					+ "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}],"
					+ "\"max_tokens\":200"
					+ "}";

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey)
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println("Status: " + response.statusCode());
			System.out.println(response.body());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}
}
