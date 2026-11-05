package io.hotdealwave.server;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** The analysed repository for page headers rendered by MVC views (search, response). */
@ControllerAdvice
class TargetAdvice {
    private final Analysis analysis;

    TargetAdvice(Analysis analysis) { this.analysis = analysis; }

    @ModelAttribute("target")
    Analysis.Target target() { return analysis.target(); }
}
