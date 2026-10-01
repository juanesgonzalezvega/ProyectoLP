package com.example.proyectolp.service;

import com.example.proyectolp.model.Stage;
import java.util.List;

public class ProjectProgressService {
    public record Progress(int completed, int total) {
        public boolean hasStages() { return total > 0; }
        public int percent() { return hasStages() ? Math.round(completed * 100.0f / total) : 0; }
    }
    public Progress calculate(List<Stage> stages) {
        int completed = (int) stages.stream().filter(s -> "completada".equalsIgnoreCase(s.getStatus())).count();
        return new Progress(completed, stages.size());
    }
    public String display(List<Stage> stages) {
        Progress progress = calculate(stages);
        return progress.hasStages() ? progress.percent() + "%" : "Sin etapas";
    }
}
