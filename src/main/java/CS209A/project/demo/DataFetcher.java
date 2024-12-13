package CS209A.project.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import java.sql.*;
import java.util.*;

@SpringBootApplication
public class DataFetcher {

    public static void main(String[] args) {
        SpringApplication.run(DataFetcher.class, args);
    }
}

@RestController
class DataFetcherController {

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "java2";
    private static final String PASSWORD = "Java2";

    static class DataPoint {
        int answerId;
        int textLength;
        double ownerReputation;
        double daysDiff;
        int voteNum;

        DataPoint(int answerId, int textLength, double ownerReputation, double daysDiff, int voteNum) {
            this.answerId = answerId;
            this.textLength = textLength;
            this.ownerReputation = ownerReputation;
            this.daysDiff = daysDiff;
            this.voteNum = voteNum;
        }
    }
    @CrossOrigin(origins = "http://localhost:63342")

    @GetMapping("/fetchPoint")
    public List<Map<String, Object>> getData() {
        Map<Integer, List<DataPoint>> categorizedData = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String query = "SELECT\n" +
                    " a.answer_id,\n" +
                    " LENGTH(a.answer_text) AS text_length,\n" +
                    " a.owner_reputation,\n" +
                    " EXTRACT(EPOCH FROM (a.answer_date - TO_TIMESTAMP(q.date, 'Mon DD, YYYY at HH24:MI'))) /\n" +
                    " (24 * 3600) AS days_diff,\n" +
                    " a.vote_num\n" +
                    "FROM stackoverflow_answers a\n" +
                    "JOIN stackoverflow_questions q ON a.question_id = q.links\n" +
                    "WHERE EXTRACT(EPOCH FROM (a.answer_date - TO_TIMESTAMP(q.date, 'Mon DD, YYYY at HH24:MI'))) /\n" +
                    " (24 * 3600) > 0 and answer_text != '';";

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    int answerId = rs.getInt("answer_id");
                    int textLength = rs.getInt("text_length");
                    String rp = rs.getString("owner_reputation");
                    int vn = rs.getInt("vote_num");

                    if (rp == null) {
                        continue;
                    }

                    // 处理 reputation 字段
                    rp = rp.replace(",", "");
                    double ownerReputation = 0;
                    if (rp.contains("k")) {
                        rp = rp.replace("k", "");
                        ownerReputation = Double.parseDouble(rp) * 1000;
                    } else if (rp.contains("m")) {
                        rp = rp.replace("m", "");
                        ownerReputation = Double.parseDouble(rp) * 1000000;
                    } else {
                        ownerReputation = Integer.parseInt(rp);
                    }
                    double daysDiff = rs.getDouble("days_diff");

                    // 根据 voteNum 分类
                    int group = getVoteNumGroup(vn);
                    DataPoint point = new DataPoint(answerId, textLength, ownerReputation, daysDiff, vn);

                    // 分类数据
                    categorizedData.computeIfAbsent(group, k -> new ArrayList<>()).add(point);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 转换数据为返回格式
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (Map.Entry<Integer, List<DataPoint>> entry : categorizedData.entrySet()) {
            Map<String, Object> groupMap = new HashMap<>();
            groupMap.put("group", entry.getKey());  // 使用组号作为标识

            List<Map<String, Object>> pointsList = new ArrayList<>();
            for (DataPoint dp : entry.getValue()) {
                Map<String, Object> pointMap = new HashMap<>();
                pointMap.put("textLength", dp.textLength);
                pointMap.put("ownerReputation", dp.ownerReputation);
                pointMap.put("daysDiff", dp.daysDiff);
                pointsList.add(pointMap);
            }

            groupMap.put("points", pointsList);
            responseList.add(groupMap);
        }

        return responseList;
    }

    // 根据 voteNum 分类数据
    private int getVoteNumGroup(int voteNum) {
        if (voteNum > 20) {
            return 1;  // 高质量答案
        } else if (voteNum >= 5) {
            return 2;  // 中高质量答案
        } else if (voteNum >= 1) {
            return 3;  // 中质量答案
        } else {
            return 4;  // 低质量答案
        }
    }
}
