package CS209A.project.demo;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DataAnalysisService {

    public List<String> getJavaTopics(int topN) {
        // 示例：假设我们从StackOverflow获取了热门Java话题，返回前N个话题
        return Arrays.asList("Generics", "Collections", "Multithreading", "I/O", "Lambda");
    }

    public List<String> getUserEngagement(int topN) {
        // 示例：返回一些用户活动频繁的话题
        return Arrays.asList("Multithreading", "Lambda", "Sockets");
    }

    public List<String> getCommonMistakes(int topN) {
        // 示例：返回常见的错误或异常
        return Arrays.asList("NullPointerException", "ArrayIndexOutOfBoundsException", "OutOfMemoryError");
    }

    public List<String> getAnswerQualityFactors(int topN) {
        // 示例：返回影响回答质量的因素
        return Arrays.asList("Elapsed Time", "Reputation of the Answerer", "Answer Popularity");
    }
}
