package org.base.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.FolderPrefix;
import org.base.core.anotation.Api;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
@Api
public class ResponseController {




    @PostMapping()
    public ResponseEntity<String> register() {
        return ResponseEntity.ok("HI");
    }

    @GetMapping(value = "/dashboardStats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<JsonNode> dashboardStats() {

        String str = """
               {"totalUsers": 1250,"activeUsers": 780,"newUsersToday": 25,"totalOrders": 5430,"revenueToday": 12500}
                """;
        /*
                HttpServletResponse response
                PrintWriter writer = response.getWriter();
                writer.write(jsonString);
                writer.flush();
        */

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode responseJson = objectMapper.readTree(str);
            return ResponseEntity.ok(responseJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/revenueChart", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<JsonNode> revenueChart() {

        Random random = new Random(); // Create single Random instance for better performance

        List<Map<String, Object>> data = IntStream.range(0, 30)
                .mapToObj(i -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", LocalDate.now().minusDays(29 - i).toString());
                    entry.put("value", random.nextInt(40001) + 10000);
                    return entry;
                })
                .collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode responseJson = objectMapper.valueToTree(data);
            return ResponseEntity.ok(responseJson);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GetMapping(value = "/usersChart", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<JsonNode> usersChart() {

        Random random = new Random();

        List<Map<String, Object>> chartData = IntStream.range(0, 30)
                .mapToObj(i -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", LocalDate.now().minusDays(29 - i).toString());
                    entry.put("value", random.nextInt(501) + 500); // Random between 500 and 1000
                    return entry;
                })
                .collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        //ObjectNode rootNode = objectMapper.createObjectNode();
        //rootNode.set("usersChart", objectMapper.valueToTree(chartData));
        try {
            JsonNode responseJson = objectMapper.valueToTree(chartData);
            return ResponseEntity.ok(responseJson);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
