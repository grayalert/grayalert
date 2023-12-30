package com.github.grayalert.core;

import com.github.grayalert.dto.LogBucket;
import com.github.grayalert.dto.LogEntry;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.util.List;
import java.util.Map;

public class ConsoleProgressNotifier implements ProgressNotifier {
    private final ProgressBar pb;

    public ConsoleProgressNotifier(Map.Entry<LogBucket, List<LogEntry>> entry) {
        LogBucket logBucket = entry.getKey();
        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setTaskName(logBucket.toString())
                .setInitialMax(entry.getValue().size())
                .setStyle(ProgressBarStyle.ASCII)
                .setMaxRenderedLength(200)
                .setUpdateIntervalMillis(100);

        this.pb = pbb.build();
    }

    @Override
    public void increment(LogBucket logBucket) {
        pb.step();
    }

    @Override
    public void close() {
        pb.close();
    }
}
