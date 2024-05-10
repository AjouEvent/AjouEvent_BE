package com.example.ajouevent.dto;
import com.google.api.client.util.DateTime;

import lombok.Data;

@Data
public class CalendarStoreDto {
    private String summary;
    private String description;
    private String startDate;
    private String endDate;
}
