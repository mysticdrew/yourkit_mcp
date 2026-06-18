package com.mysticdrew.yourkitmcp.snapshot;

import java.util.List;

public record AnalysisResult(String exportDir, List<HotSpot> hotSpots, List<MemoryClass> topClasses) {}
