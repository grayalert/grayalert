package com.github.grayalert.persistence;


//import io.quarkus.hibernate.orm.panache.PanacheEntity;
//import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name="log_example")
@Data
public class LogExample  {
    @Id
    public String id;
    @Column(length = 65536)
    public String appName;
    @Column(length = 65536)
    public String loggerName;

    @Column(length = 65536)
    public String message;
    @Column(length = 500)
    public String baseUrl;

    @Column(length = 500)
    public String linkHtml;

    @Column(length = 500)
    public String url;
    @Column(length = 500)
    public String shortMessage;

    @Column(length = 500)
    public Integer count;
    @Column(length = 500)
    public Long firstTimestamp;
    @Column(length = 500)
    public String firstGraylogId;
    @Column(length = 500)
    public String firstTraceId;



    public Long lastTimestamp;
    @Column(length = 500)
    public String lastGraylogId;
    @Column(length = 500)
    public String lastTraceId;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public String getFirstSeen() {
        return formatDateTime(firstTimestamp);
    }

    public Long calculateLastTimestamp() {
        if (lastTimestamp != null) {
            return lastTimestamp;
        }
        return firstTimestamp;
    }
    public String getLastSeen() {
        if (lastTimestamp != null) {
            return formatDateTime(lastTimestamp);
        }
        return "";
    }

    private String formatDateTime(Long ts) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.of("UTC"));

        return formatter.format(localDateTime);
    }
}

