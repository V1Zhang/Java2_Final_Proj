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

@SpringBootApplication
public class UserTagCounter {
    public static void main(String[] args) {
        SpringApplication.run(UserTagCounter.class, args);
    }
}

@RestController
class User_TagController {
    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/Usertags")
    public List<Map<String, Object>> getTags(@RequestParam(defaultValue = "10") int limit) {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "java2";
        String password = "Java2";

        Map<String, Integer> tagCountMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Step 1: Get user_id from stackoverflow_users with reputation_score > 2000
            String userSql = "SELECT question_id FROM stackoverflow_users WHERE CAST(REPLACE(reputation_score, ',', '') AS INTEGER) > 2000";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(userSql)) {
                while (rs.next()) {
                    int questionId = rs.getInt("question_id");
                    String questionSql = "SELECT tags FROM stackoverflow_questions WHERE links = ?";
                    try (PreparedStatement questionStmt = conn.prepareStatement(questionSql)) {
                        questionStmt.setInt(1, questionId);
                        try (ResultSet questionRs = questionStmt.executeQuery()) {
                            while (questionRs.next()) {
                                Array linksArray = questionRs.getArray("tags");
                                if (linksArray != null) {
                                    String[] tags = (String[]) linksArray.getArray();
                                    for (String tag : tags) {
                                        if (tag != null&&!tag.equals("java")) {
                                            tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Step 3: Sort tags by count and limit the number of tags
        List<Map.Entry<String, Integer>> sortedList = tagCountMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        // Step 4: Prepare result data
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
