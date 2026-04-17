package Util;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LLMAPITest {
	public static void main(String[] args) {
		try {
			// 将 API Key 与 Base URL 直接写入文件（替换下面的占位符为你的实际值）
			String apiKey = "sk-a83fcef78af344cda2b94c0500c15fce"; // <- 替换为你的真实 API Key
			String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"; // <- 如需自定义 base URL，请修改
			String endpoint = baseUrl + "/chat/completions";
			String model = "qwen3.6-flash"; // 根据 SprintAI 文档调整模型名
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
