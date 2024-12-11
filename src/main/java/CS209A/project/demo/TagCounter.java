package CS209A.project.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
/*
@RestController 会自动将返回值（如 List<Map<String, Object>>）转换为 JSON 格式进行响应。
Spring Boot 默认使用 Jackson 库将 Java 对象序列化为 JSON 格式
 */
@SpringBootApplication
public class TagCounter {
    public static void main(String[] args) {
        SpringApplication.run(TagCounter.class, args);
    }
}

@RestController
class TagController {

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/tags")
    public List<Map<String, Object>> getTags(@RequestParam(defaultValue = "10") int limit) {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "java2";
        String password = "Java2";

        Map<String, Integer> tagCountMap = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT tags FROM stackoverflow_questions";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Array tagsArray = rs.getArray("tags");
                    if (tagsArray != null) {
                        String[] tags = (String[]) tagsArray.getArray();
                        for (String tag : tags) {
                            if(!tag.equals("java")){
                                tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
                            }

                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Map.Entry<String, Integer>> sortedList = tagCountMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedList) {
            Map<String, Object> tag = new HashMap<>();
            tag.put("name", entry.getKey());
            tag.put("value", entry.getValue());
            result.add(tag);
        }
        return result;
    }
}
