package com.askaragoz.supportagent.mapper;

import com.askaragoz.supportagent.domain.Feedback;
import com.askaragoz.supportagent.dto.response.FeedbackResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Feedback → FeedbackResponse.
 */
@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    /**
     * Maps Feedback entity → FeedbackResponse DTO.
     *
     * 'aiResponseId' does not exist directly on Feedback — it is nested at
     * feedback.aiResponse.id. The dot-notation in source = "aiResponse.id"
     * tells MapStruct to traverse the relationship to get the value.
     */
    @Mapping(target = "aiResponseId", source = "aiResponse.id")
    FeedbackResponse toResponse(Feedback feedback);
}
