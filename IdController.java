package com.example.idgen;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.LongStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ids")
public class IdController {

    private final SnowflakeIdGenerator generator;

    public IdController(SnowflakeIdGenerator generator) {
        this.generator = generator;
    }

    @GetMapping
    public Map<String, Long> one() {
        return Map.of("id", generator.nextId());
    }

    @GetMapping("/batch")
    public List<Long> batch(@RequestParam(defaultValue = "10") int count) {
        int n = Math.min(Math.max(count, 1), 1000);
        return LongStream.range(0, n).map(i -> generator.nextId()).boxed().toList();
    }
}
