package CS209A.project.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {
    /**
     * In this demo, you can visit localhost:9091 or localhost:9091/demo to see the result.
     * @return the name of the view to be rendered
     * You can find the static HTML file in src/main/resources/templates/demo.html
     */
    @GetMapping("/demo")
    public String demo() {
        return "demo";
    }

}
