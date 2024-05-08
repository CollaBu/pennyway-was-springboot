package kr.co.pennyway.api.apis.ledge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TargetAmountDto {
    @Schema(title = "목표 금액 등록/수정 요청 파라미터")
    public record UpdateParamReq(
            @Schema(description = "등록하려는 목표 금액 날짜 (당일)", example = "2024-05-08", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "date 값은 필수입니다.")
            @JsonSerialize(using = LocalDateSerializer.class)
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate date,
            @Schema(description = "등록하려는 목표 금액 (0이상의 정수)", example = "100000", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "amount 값은 필수입니다.")
            @Min(value = 0, message = "amount 값은 0 이상이어야 합니다.")
            Integer amount
    ) {

    }
}
