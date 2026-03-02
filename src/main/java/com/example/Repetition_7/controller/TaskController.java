package com.example.Repetition_7.controller;

import com.example.Repetition_7.response.TaskResponse;
import com.example.Repetition_7.request.CreateTaskRequest;
import com.example.Repetition_7.request.UpdateTaskRequest;
import com.example.Repetition_7.service.TaskService;
import com.example.Repetition_7.validation.TaskPageableValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Operations related to task management")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class TaskController {

    private final TaskService taskService;
    private final TaskPageableValidator pageableValidator;

    @GetMapping("/tasks")
    @Operation(summary = "Search all tasks", description = "Search tasks with optional filters and pagination")
    public Page<TaskResponse> getAll(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
                                Pageable pageable,
                                     @RequestParam (required = false) Boolean completed,
                                     @RequestParam (required = false) String title,
                                     @RequestParam (required = false) String description,
                                     @RequestParam (required = false) Long createdByUserId) {

        pageableValidator.validate(pageable);

        return taskService.search(pageable, completed, title, description, createdByUserId);
    }

    @PostMapping("/tasks")
    @Operation(summary = "Create new task", description = "Create new task with title and completed status")
    public ResponseEntity<TaskResponse> createNewTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get task by id", description = "Find task by id")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable @Min(1) Long id) {

        return ResponseEntity.ok(taskService.getById(id));
    }

    @PatchMapping("/tasks/{id}")
    @Operation(summary = "Update task", description = "Update completion status or/and title")
    public ResponseEntity<TaskResponse> updateTaskById(@PathVariable @Min(1) Long id, @Valid @RequestBody UpdateTaskRequest request) {

        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete task by id", description = "Delete task by id")
    public ResponseEntity<Void> deleteTaskById(@PathVariable @Min(1) Long id) {

        taskService.delete(id);

        return ResponseEntity.noContent().build();
    }

}
