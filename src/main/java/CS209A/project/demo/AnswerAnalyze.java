package CS209A.project.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AnswerAnalyze {
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "java2";
    private static final String PASSWORD = "Java2";

    public static void main(String[] args) {
        try {
            // 1. 建立数据库连接
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // 2. 执行查询
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

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            // 3. 数据分类处理
            Map<String, List<DataPoint>> categorizedData = categorizeData(rs);

            // 4. 导出JSON
            exportToJson(categorizedData);

            // 5. 关闭资源
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

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

    // 数据分类方法
    private static Map<String, List<DataPoint>> categorizeData(ResultSet rs) throws SQLException {
        Map<String, List<DataPoint>> categories = new HashMap<>();

        categories.put("textLengthCategoriesShort", new ArrayList<>());
        categories.put("textLengthCategoriesMedium", new ArrayList<>());
        categories.put("textLengthCategoriesLong", new ArrayList<>());

        categories.put("reputationCategoriesLow", new ArrayList<>());
        categories.put("reputationCategoriesMedium", new ArrayList<>());
        categories.put("reputationCategoriesHigh", new ArrayList<>());

        categories.put("timeCategoriesShort", new ArrayList<>());
        categories.put("timeCategoriesMedium", new ArrayList<>());
        categories.put("timeCategoriesLong", new ArrayList<>());
        categories.put("timeCategoriesVeryLong", new ArrayList<>());

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

            DataPoint point = new DataPoint(answerId, textLength, ownerReputation, daysDiff, vn);

            // 文本长度分类
            if (textLength < 200) {
                categories.get("textLengthCategoriesShort").add(point);
            } else if (textLength <= 1000) {
                categories.get("textLengthCategoriesMedium").add(point);
            } else {
                categories.get("textLengthCategoriesLong").add(point);
            }

            // 声誉分类
            if (ownerReputation < 10000) {
                categories.get("reputationCategoriesLow").add(point);
            } else if (ownerReputation <= 10000) {
                categories.get("reputationCategoriesMedium").add(point);
            } else {
                categories.get("reputationCategoriesHigh").add(point);
            }

            // 时间间隔分类
            if (daysDiff < 365) {
                categories.get("timeCategoriesShort").add(point);
            } else if (daysDiff >= 365 && daysDiff < 1825) {
                categories.get("timeCategoriesMedium").add(point);
            } else if (daysDiff >= 1825 && daysDiff < 3650) {
                categories.get("timeCategoriesLong").add(point);
            } else {
                categories.get("timeCategoriesVeryLong").add(point);
            }
        }

        return categories;
    }

    // JSON导出方法
    private static void exportToJson(Map<String, List<DataPoint>> categorizedData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonOutput = objectMapper.createObjectNode();

        // 处理文本长度类别
        ArrayNode textLengthArray = processCategory(
                categorizedData.get("textLengthCategoriesShort"),
                "textLength",
                "短答案"
        );
        textLengthArray.addAll(processCategory(
                categorizedData.get("textLengthCategoriesMedium"),
                "textLength",
                "中等答案"
        ));
        textLengthArray.addAll(processCategory(
                categorizedData.get("textLengthCategoriesLong"),
                "textLength",
                "长答案"
        ));
        jsonOutput.set("textLengthCategories", textLengthArray);

        // 处理声誉类别
        ArrayNode reputationArray = processCategory(
                categorizedData.get("reputationCategoriesLow"),
                "reputation",
                "低声誉"
        );
        reputationArray.addAll(processCategory(
                categorizedData.get("reputationCategoriesMedium"),
                "reputation",
                "中声誉"
        ));
        reputationArray.addAll(processCategory(
                categorizedData.get("reputationCategoriesHigh"),
                "reputation",
                "高声誉"
        ));
        jsonOutput.set("reputationCategories", reputationArray);

        // 处理时间类别
        ArrayNode timeArray = processCategory(
                categorizedData.get("timeCategoriesShort"),
                "timeInterval",
                "1年内"
        );
        timeArray.addAll(processCategory(
                categorizedData.get("timeCategoriesMedium"),
                "timeInterval",
                "1-5年"
        ));
        timeArray.addAll(processCategory(
                categorizedData.get("timeCategoriesLong"),
                "timeInterval",
                "5-10年"
        ));
        timeArray.addAll(processCategory(
                categorizedData.get("timeCategoriesVeryLong"),
                "timeInterval",
                "10年以上"
        ));
        jsonOutput.set("timeCategories", timeArray);

        // 写入JSON文件
        try (FileWriter file = new FileWriter("answer_analysis7.json")) {
            file.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonOutput));
            System.out.println("JSON文件已成功生成");
        }
    }

    // 处理每个类别的统计
    private static ArrayNode processCategory(List<DataPoint> dataPoints, String categoryName, String categoryLabel) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode resultArray = objectMapper.createArrayNode();

        ObjectNode categoryData = objectMapper.createObjectNode();
        categoryData.put("category", categoryLabel);
        categoryData.put("count", dataPoints.size());
        categoryData.put("avgTextLength", calculateAverageTextLength(dataPoints));
        categoryData.put("avgReputation", calculateAverageReputation(dataPoints));
        categoryData.put("avgTimeInterval", calculateAverageTimeInterval(dataPoints));
        categoryData.put("avgVoteNum", calculateAverageVoteNum(dataPoints));

        resultArray.add(categoryData);

        return resultArray;
    }

    private static double calculateAverageTextLength(List<DataPoint> dataPoints) {
        return dataPoints.stream()
                .mapToInt(dp -> dp.textLength)
                .average()
                .orElse(0);
    }

    private static double calculateAverageReputation(List<DataPoint> dataPoints) {
        return dataPoints.stream()
                .mapToDouble(dp -> dp.ownerReputation)
                .average()
                .orElse(0);
    }

    private static double calculateAverageTimeInterval(List<DataPoint> dataPoints) {
        return dataPoints.stream()
                .mapToDouble(dp -> dp.daysDiff)
                .average()
                .orElse(0);
    }
    private static double calculateAverageVoteNum(List<DataPoint> dataPoints) {
        return dataPoints.stream()
                .mapToInt(dp -> dp.voteNum)
                .average()
                .orElse(0);
    }
}
