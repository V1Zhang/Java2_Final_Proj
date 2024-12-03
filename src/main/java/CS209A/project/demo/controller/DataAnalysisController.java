package CS209A.project.demo.controller;

import CS209A.project.demo.DataAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    @Autowired
    public DataAnalysisController(DataAnalysisService dataAnalysisService) {
        this.dataAnalysisService = dataAnalysisService;
    }

    // API 1: 获取热门Java话题
    @GetMapping("/api/java-topics")
    public List<String> getJavaTopics(@RequestParam int topN) {
        return dataAnalysisService.getJavaTopics(topN);
    }

    // API 2: 获取高声誉用户的参与度
    @GetMapping("/api/user-engagement")
    public List<String> getUserEngagement(@RequestParam int topN) {
        return dataAnalysisService.getUserEngagement(topN);
    }

    // API 3: 获取常见的错误和异常
    @GetMapping("/api/common-mistakes")
    public List<String> getCommonMistakes(@RequestParam int topN) {
        return dataAnalysisService.getCommonMistakes(topN);
    }

    // API 4: 获取高质量答案的因素分析
    @GetMapping("/api/answer-quality")
    public List<String> getAnswerQuality(@RequestParam int topN) {
        return dataAnalysisService.getAnswerQualityFactors(topN);
    }
}
