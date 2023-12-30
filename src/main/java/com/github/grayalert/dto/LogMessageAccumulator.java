package com.github.grayalert.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(of={"first"})
@SuperBuilder(toBuilder = true)
public class LogMessageAccumulator {
    LogOccurrence first;
    LogOccurrence last;
    @Builder.Default
    Integer count = 1;
}
