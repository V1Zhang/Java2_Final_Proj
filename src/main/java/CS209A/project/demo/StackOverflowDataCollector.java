package CS209A.project.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class StackOverflowDataCollector {

    // PostgreSQL 配置
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/stackoverflow";
    private static final String DB_USER = "java2";
    private static final String DB_PASSWORD = "Java2";

    // Stack Overflow API URL
    private static final String API_BASE_URL = "https://api.stackexchange.com/2.3/questions";
    private static final String CLIENT_ID = "30246";
    private static final String CLIENT_SECRET = "c4OQJG01jjxHIA75XdERXg((";
    private static final String KEY = "rl_2RYr5ZWkfUF7sjAvHERYHVqeB";

    // HTTP 客户端
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("成功连接到 PostgreSQL 数据库");

            // 创建数据库表
            //createTable(connection);

            // 获取数据
            List<Map<String, Object>> questions =
        fetchData("java", 10, 100);

            // 插入数据到数据库
            insertData(connection, questions);

            System.out.println("数据存储完成");
        } catch (Exception e) {
            e.printStackTrace();
       }
    }

    // 创建数据库表
    private static void createTable(Connection connection) throws Exception {
        String createQuestionsTableSQL = """
            CREATE TABLE IF NOT EXISTS questions (
                question_id BIGINT PRIMARY KEY,
                tags TEXT[],
                owner_id BIGINT,
                is_answered BOOLEAN,
                view_count INT,
                accepted_answer_id INT,
                answer_count INT,
                score INT,
                last_activity_date BIGINT,
                creation_date BIGINT,
                content_license TEXT,
                link TEXT,
                title TEXT,
                FOREIGN KEY (owner_id) REFERENCES stackoverflowUser(account_id) ON DELETE CASCADE
                FOREIGN KEY (_id) REFERENCES stackoverflowUser(account_id) ON DELETE CASCADE
            )
            """;

        // SQL语句用于创建user表
        String createUserTableSQL = """
            CREATE TABLE IF NOT EXISTS stackoverflowUser (
                account_id BIGINT PRIMARY KEY,
                reputation INT,
                user_id BIGINT,
                user_type TEXT,
                profile_image TEXT,
                
                display_name TEXT,
                link TEXT
            )
            """;

        // 创建questions表
        try (PreparedStatement statement = connection.prepareStatement(createQuestionsTableSQL)) {
            statement.execute();
            System.out.println("questions表创建完成");
        }

        // 创建user表
        try (PreparedStatement statement = connection.prepareStatement(createUserTableSQL)) {
            statement.execute();
            System.out.println("user表创建完成");
        }
    }

    // 从 Stack Overflow API 获取数据
    private static List<Map<String, Object>> fetchData(String tag, int pages, int pageSize) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        Gson gson = new Gson();

        for (int page = 1; page <= pages; page++) {
            String url = API_BASE_URL + "?order=desc&sort=creation&tagged=" + tag +
                    "&site=stackoverflow&pagesize=" + pageSize + "&page=" + page +
                    "&client_id=" + CLIENT_ID +
                    "&client_secret=" + CLIENT_SECRET +
                    "&key=" + KEY;

            // https://api.stackexchange.com/2.3/questions?order=desc&sort=creation&tagged=java&site=stackoverflow&pagesize=10&page=100&client_id=30246&client_secret=c4OQJG01jjxHIA75XdERXg((&key=rl_2RYr5ZWkfUF7sjAvHERYHVqeB

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                JsonArray items = jsonResponse.getAsJsonArray("items");

                for (var item : items) {
                    results.add(gson.fromJson(item, Map.class));
                }

                System.out.println("成功获取第 " + page + " 页数据");
            } else {
                System.out.println("获取第 " + page + " 页数据失败，状态码：" + response.statusCode() + response.body());
            }

            // 防止触发速率限制
            Thread.sleep(1000);
        }

        return results;
    }

    // 插入数据到数据库
    private static void insertData(Connection connection, List<Map<String, Object>> data) throws Exception {
        String insertSQL = """
            INSERT INTO questions (question_id, title, body, tags, creation_date, score, view_count, owner_id, is_answered, answer_count, last_activity_date, content_license, link)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (question_id) DO NOTHING
            """;

        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            for (Map<String, Object> item : data) {
                // Setting the values for the columns
                statement.setLong(1, ((Double) item.get("question_id")).longValue()); // question_id
                statement.setString(2, (String) item.get("title")); // title
                statement.setString(3, (String) item.getOrDefault("body", "")); // body
                statement.setArray(4, connection.createArrayOf("TEXT", ((List<String>) item.get("tags")).toArray())); // tags (TEXT[])
                statement.setLong(5, ((Double) item.get("creation_date")).longValue()); // creation_date
                statement.setInt(6, ((Double) item.get("score")).intValue()); // score
                statement.setInt(7, ((Double) item.get("view_count")).intValue()); // view_count
                statement.setLong(8, ((Double) item.get("owner_id")).longValue()); // owner_id
                statement.setBoolean(9, (Boolean) item.get("is_answered")); // is_answered
                statement.setInt(10, ((Double) item.get("answer_count")).intValue()); // answer_count
                statement.setLong(11, ((Double) item.get("last_activity_date")).longValue()); // last_activity_date
                statement.setString(12, (String) item.getOrDefault("content_license", "")); // content_license
                statement.setString(13, (String) item.getOrDefault("link", "")); // link

                statement.addBatch();
            }

            statement.executeBatch();
            System.out.println("数据插入完成");
        }
    }

}
