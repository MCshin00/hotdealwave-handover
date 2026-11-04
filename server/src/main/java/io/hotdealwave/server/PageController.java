package io.hotdealwave.server;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint, topic and service bodies as HTML. Search and MCP read the same bodies as text. */
@Controller
class PageController {
    private final Analysis analysis;
    private final Pages pages;

    PageController(Analysis analysis, Pages pages) { this.analysis = analysis; this.pages = pages; }

    @GetMapping("/")
    String index(Model model) {
        model.addAttribute("services", pages.services());
        model.addAttribute("topics", pages.topics());
        var counts = new LinkedHashMap<String, Integer>();
        analysis.checks().properties().forEach(e -> counts.put(e.getKey(), e.getValue().path("warningCount").asInt()));
        model.addAttribute("warnings", counts);
        return "index";
    }

    @GetMapping("/endpoints/{id}")
    String endpoint(@PathVariable String id, Model model) { return show("/endpoints/" + id, model); }

    @GetMapping("/topics/{name}")
    String topic(@PathVariable String name, Model model) { return show("/topics/" + name, model); }

    @GetMapping("/services/{name}")
    String service(@PathVariable String name, Model model) { return show("/services/" + name, model); }

    private String show(String path, Model model) {
        var page = pages.page(path);
        if (page == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAllAttributes(page.model());
        return page.template();
    }
}
