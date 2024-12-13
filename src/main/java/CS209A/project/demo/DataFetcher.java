package CS209A.project.demo;

import java.io.FileWriter;
import java.sql.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataFetcher {

    // 定义数据点
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

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "java2";
    private static final String PASSWORD = "Java2";

    public static void main(String[] args) {

        Map<Integer, List<DataPoint>> categorizedData = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String query = "SELECT\n" +
                    "                             a.answer_id,\n" +
                    "                             LENGTH(a.answer_text) AS text_length,\n" +
                    "                             a.owner_reputation,\n" +
                    "                             EXTRACT(EPOCH FROM (a.answer_date - TO_TIMESTAMP(q.date, 'Mon DD, YYYY at HH24:MI'))) /\n" +
                    "                             (24 * 3600) AS days_diff,\n" +
                    "                             a.vote_num\n" +
                    "                      FROM stackoverflow_answers a\n" +
                    "                               JOIN stackoverflow_questions q ON a.question_id = q.links\n" +
                    "                      WHERE EXTRACT(EPOCH FROM (a.answer_date - TO_TIMESTAMP(q.date, 'Mon DD, YYYY at HH24:MI'))) /\n" +
                    "                            (24 * 3600) > 0 and answer_text != '';";

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    int answerId = rs.getInt("answer_id");
                    int textLength = rs.getInt("text_length");
                    String rp = rs.getString("owner_reputation");
                    int vn = rs.getInt("vote_num");

                    if (rp == null) {
                        continue;
                    }
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

                    // 以组号为键，分类数据
                    categorizedData.computeIfAbsent(group, k -> new ArrayList<>()).add(point);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 使用 Jackson 构建 JSON 数据
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonData = mapper.createArrayNode();

        for (Map.Entry<Integer, List<DataPoint>> entry : categorizedData.entrySet()) {
            ObjectNode group = mapper.createObjectNode();
            group.put("group", entry.getKey());  // 使用组号作为标识
            ArrayNode pointsArray = mapper.createArrayNode();
            for (DataPoint dp : entry.getValue()) {
                ObjectNode point = mapper.createObjectNode();
                point.put("textLength", dp.textLength);
                point.put("ownerReputation", dp.ownerReputation);
                point.put("daysDiff", dp.daysDiff);
                pointsArray.add(point);
            }
            group.set("points", pointsArray);
            jsonData.add(group);
        }

        try {
            try (FileWriter file = new FileWriter("point3.json")) {
                file.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData));
                System.out.println("JSON文件已成功生成");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 根据 voteNum 分类数据
    private static int getVoteNumGroup(int voteNum) {
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
