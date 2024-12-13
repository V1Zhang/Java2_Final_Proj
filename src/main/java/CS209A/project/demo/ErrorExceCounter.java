package CS209A.project.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ErrorExceCounter {

    public static void main(String[] args) {
        SpringApplication.run(ErrorExceCounter.class, args);
    }
}

@RestController
class ErrorExceController {

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/errorStats")
    public String getErrorStatistics(@RequestParam(defaultValue = "20") int limit) {
        // 数据库连接信息
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "java2";
        String password = "Java2";

        // 存储错误数据
        Map<String, List<String>> errorData = new HashMap<>();

        try (Connection connection = DriverManager.getConnection(url, user, password)) {

            // 查询数据库获取错误类型的统计
            String query = "SELECT\n" +
                    "    q.links, q.question_title,\n" +
                    "    COUNT(*) AS answer_count,\n" +
                    "    CASE\n" +
                    "        WHEN a.answer_text LIKE '%OutOfMemoryError%' OR q.question_title LIKE '%OutOfMemoryError%' THEN 'OutOfMemoryError'\n" +
                    "        WHEN a.answer_text LIKE '%out of memory%' OR q.question_title LIKE '%out of memory%' THEN 'OutOfMemoryError'\n" +
                    "        WHEN a.answer_text LIKE '%NullPointer%' OR q.question_title LIKE '%NullPointer%' THEN 'NullPointerException'\n" +
                    "        WHEN a.answer_text LIKE '%null pointer%' OR q.question_title LIKE '%null pointer%' THEN 'NullPointerException'\n" +
                    "        WHEN a.answer_text LIKE '%IOException%' OR q.question_title LIKE '%IOException%' THEN 'IOException'\n" +
                    "        WHEN a.answer_text LIKE '%SQLException%' OR q.question_title LIKE '%SQLException%' THEN 'SQLException'\n" +
                    "        WHEN a.answer_text LIKE '%IndexOutOfBounds%' OR q.question_title LIKE '%IndexOutOfBounds%' THEN 'ArrayIndexOutOfBoundsException'\n" +
                    "        WHEN a.answer_text LIKE '%StackOverflow%' OR q.question_title LIKE '%StackOverflow%' THEN 'StackOverflowError'\n" +
                    "        WHEN a.answer_text LIKE '%Arithmetic%' OR q.question_title LIKE '%Arithmetic%' THEN 'ArithmeticException'\n" +
                    "        WHEN a.answer_text LIKE '%NotFoundException%' OR q.question_title LIKE '%NotFoundException%' THEN 'NotFoundException'\n" +
                    "        WHEN a.answer_text LIKE '%ParseException%' OR q.question_title LIKE '%ParseException%' THEN 'ParseException'\n" +
                    "        WHEN a.answer_text LIKE '%ClassCastException%' OR q.question_title LIKE '%ClassCastException%' THEN 'ClassCastException'\n" +
                    "        WHEN a.answer_text LIKE '%Illegal%' OR q.question_title LIKE '%Illegal%' THEN 'IllegalArgumentException'\n" +
                    "        WHEN a.answer_text LIKE '%NumberFormatException%' OR q.question_title LIKE '%NumberFormatException%' THEN 'NumberFormatException'\n" +
                    "        WHEN a.answer_text LIKE '%UnsupportedOperationException%' OR q.question_title LIKE '%UnsupportedOperationException%' THEN 'UnsupportedOperationException'\n" +
                    "        WHEN a.answer_text LIKE '%ConcurrentModificationException%' OR q.question_title LIKE '%ConcurrentModificationException%' THEN 'ConcurrentModificationException'\n" +
                    "        WHEN a.answer_text LIKE '%SecurityException%' OR q.question_title LIKE '%SecurityException%' THEN 'SecurityException'\n" +
                    "        WHEN a.answer_text LIKE '%MalformedURLException%' OR q.question_title LIKE '%MalformedURLException%' THEN 'MalformedURLException'\n" +
                    "        WHEN a.answer_text LIKE '%CloneNotSupportedException%' OR q.question_title LIKE '%CloneNotSupportedException%' THEN 'CloneNotSupportedException'\n" +
                    "        WHEN a.answer_text LIKE '%UnsupportedClassVersionError%' OR q.question_title LIKE '%UnsupportedClassVersionError%' THEN 'UnsupportedClassVersionError'\n" +
                    "        ELSE 'Other Error'\n" +
                    "    END AS error_type\n" +
                    "FROM stackoverflow_answers a\n" +
                    "JOIN stackoverflow_questions q ON a.question_id = q.links\n" +
                    "WHERE a.answer_text != '' AND a.question_id NOT IN (\n" +
                    "    SELECT question_id FROM stackoverflow_answers WHERE answer_text = '' GROUP BY question_id\n" +
                    ")\n" +
                    "GROUP BY q.links, q.question_title,error_type;";

            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String errorType = rs.getString("error_type");
                    String questionTitle = rs.getString("question_title");

                    errorData.putIfAbsent(errorType, new ArrayList<>());
                    errorData.get(errorType).add(questionTitle);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 转换数据为 JSON 格式
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode jsonArray = objectMapper.createArrayNode();

        // 生成前 `limit` 条统计数据
        int count = 0;
        for (Map.Entry<String, List<String>> entry : errorData.entrySet()) {
            if (count >= limit) break;
            if(!entry.getKey().equals("Other Error")){
                ObjectNode node = objectMapper.createObjectNode();
                node.put("name", entry.getKey());  // 错误类型
                node.put("value", entry.getValue().size());  // 错误类型对应的问题个数
                node.putPOJO("title", entry.getValue().toArray(new String[0]));  // 错误类型对应的问题标题数组
                jsonArray.add(node);
                count++;
            }
        }
        try {
            // 将结果作为 JSON 字符串返回
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonArray);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
