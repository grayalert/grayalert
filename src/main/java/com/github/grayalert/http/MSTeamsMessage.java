package com.github.grayalert.http;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public class MSTeamsMessage {
    String text;
}
