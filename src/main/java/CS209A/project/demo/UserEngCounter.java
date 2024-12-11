package CS209A.project.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class UserEngCounter {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/postgres";  // 替换为你的数据库URL
        String user = "java2";  // 替换为你的数据库用户名
        String password = "Java2";  // 替换为你的数据库密码

        Map<String, Integer> tagCountMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            String sql = "SELECT tags FROM stackoverflow_users";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {

                    Array tagsArray = rs.getArray("tags");
                    if (tagsArray != null) {
                        String[] tags = (String[]) tagsArray.getArray();

                        for (String tag : tags) {
                            tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode jsonArray = objectMapper.createArrayNode();

        // 1. 按照 value 对 tagCountMap 排序，从大到小
        List<Map.Entry<String, Integer>> sortedList = tagCountMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // 降序排序
                .collect(Collectors.toList());

        // 2. 将排序后的数据转化为 JSON 格式并加入到 jsonArray
        for (Map.Entry<String, Integer> entry : sortedList) {
            var tagObject = objectMapper.createObjectNode();
            tagObject.put("name", entry.getKey());
            tagObject.put("value", entry.getValue());
            jsonArray.add(tagObject);
        }

        // 3. 将排序后的 JSON 写入文件
        try {
            objectMapper.writeValue(new File("user_tag_counts.json"), jsonArray);
            System.out.println("success tag_counts.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
