package id.payu.billing.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.interfaces.dto.BillerDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import id.payu.billing.exception.BillerNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * REST Controller for biller information.
 */
@RestController
@RequestMapping("/api/v1/billers")
@Tag(name = "Billers", description = "Biller information and catalog APIs")
public class BillerController {

    @GetMapping
    @Operation(summary = "List billers", description = "Retrieve list of available billers, optionally filtered by category")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billers retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillerDto.class)))
    public ApiResponse<List<BillerDto>> listBillers(
            @Parameter(description = "Filter by category (e.g., electricity, water, mobile, ewallet)")
            @RequestParam(required = false) String category) {

        List<BillerDto> billers = Arrays.stream(BillerType.values())
                .filter(type -> category == null || type.getCategory().equalsIgnoreCase(category))
                .map(BillerDto::from)
                .toList();

        return ApiResponse.success(billers);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get biller details", description = "Retrieve detailed information about a specific biller")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Biller found",
            content = @Content(schema = @Schema(implementation = BillerDto.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Biller not found")
    public ApiResponse<BillerDto> getBiller(
            @Parameter(description = "Biller code (e.g., PLN, PDAM, GOPAY)", required = true)
            @PathVariable String code) {

        return Arrays.stream(BillerType.values())
                .filter(type -> type.getCode().equalsIgnoreCase(code))
                .findFirst()
                .map(biller -> ApiResponse.success(BillerDto.from(biller)))
                .orElseThrow(() -> new BillerNotFoundException("Biller not found: " + code));
    }

    @GetMapping("/categories")
    @Operation(summary = "List biller categories", description = "Retrieve all available biller categories")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    public ApiResponse<List<String>> listCategories() {
        List<String> categories = Arrays.stream(BillerType.values())
                .map(BillerType::getCategory)
                .distinct()
                .toList();
        return ApiResponse.success(categories);
    }
}
