package io.hotdealwave.server;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SearchController {
    private final Search search;

    SearchController(Search search) { this.search = search; }

    @GetMapping("/api/search")
    List<Search.Result> search(@RequestParam String q) { return search.search(q); }
}
