package CS209A.project.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.*;

@SpringBootApplication
public class AnswerAnalyze {
    public static void main(String[] args) {
        SpringApplication.run(AnswerAnalyze.class, args);
    }
}

@RestController
class AnswerController {

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "java2";
    private static final String PASSWORD = "Java2";

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/fetchAnswer")
    public List<Map<String, Object>> getData() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {

            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
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

            // 4. 将分类数据转换为List
            resultList = convertCategoriesToList(categorizedData);

            // 5. 关闭资源
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
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

    // 将分类数据转换为 List
    private static List<Map<String, Object>> convertCategoriesToList(Map<String, List<DataPoint>> categorizedData) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        // 处理文本长度类别
        addCategoryToList(resultList, categorizedData.get("textLengthCategoriesShort"), "文本长度-短");
        addCategoryToList(resultList, categorizedData.get("textLengthCategoriesMedium"), "文本长度-中等");
        addCategoryToList(resultList, categorizedData.get("textLengthCategoriesLong"), "文本长度-长");

        // 处理声誉类别
        addCategoryToList(resultList, categorizedData.get("reputationCategoriesLow"), "声誉-低");
        addCategoryToList(resultList, categorizedData.get("reputationCategoriesMedium"), "声誉-中");
        addCategoryToList(resultList, categorizedData.get("reputationCategoriesHigh"), "声誉-高");

        // 处理时间类别
        addCategoryToList(resultList, categorizedData.get("timeCategoriesShort"), "时间-1年内");
        addCategoryToList(resultList, categorizedData.get("timeCategoriesMedium"), "时间-1-5年");
        addCategoryToList(resultList, categorizedData.get("timeCategoriesLong"), "时间-5-10年");
        addCategoryToList(resultList, categorizedData.get("timeCategoriesVeryLong"), "时间-10年以上");

        return resultList;
    }

    // 向结果列表中添加分类数据
    private static void addCategoryToList(List<Map<String, Object>> resultList, List<DataPoint> dataPoints, String category) {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("category", category);
        categoryData.put("count", dataPoints.size());
        categoryData.put("avgTextLength", calculateAverageTextLength(dataPoints));
        categoryData.put("avgReputation", calculateAverageReputation(dataPoints));
        categoryData.put("avgTimeInterval", calculateAverageTimeInterval(dataPoints));
        categoryData.put("avgVoteNum", calculateAverageVoteNum(dataPoints));

        resultList.add(categoryData);
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
