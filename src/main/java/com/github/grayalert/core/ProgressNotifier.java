package com.github.grayalert.core;

import com.github.grayalert.dto.LogBucket;

public interface ProgressNotifier {
    void increment(LogBucket logBucket);

    void close();
}
