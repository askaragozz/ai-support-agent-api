package com.askaragoz.supportagent.mapper;

import com.askaragoz.supportagent.domain.AiResponse;
import com.askaragoz.supportagent.domain.SupportTicket;
import com.askaragoz.supportagent.dto.response.AiResponseDto;
import com.askaragoz.supportagent.dto.response.TicketDetailResponse;
import com.askaragoz.supportagent.dto.response.TicketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for SupportTicket → response DTOs.
 *
 * MapStruct reads these interface methods at compile time and generates a concrete
 * implementation class (TicketMapperImpl). Because we set
 * -Amapstruct.defaultComponentModel=spring in pom.xml, the generated class is
 * annotated with @Component and Spring injects it wherever TicketMapper is used.
 *
 * @Mapper(componentModel = "spring") — equivalent to the global compiler flag;
 * explicitly documented here so the intent is clear at the class level.
 *
 * Field mapping works by NAME — MapStruct matches source and target fields with
 * the same name automatically. Mismatches must be declared with @Mapping.
 */
@Mapper(componentModel = "spring")
public interface TicketMapper {

    /** Maps to the lightweight list view (no description, no aiResponse). */
    TicketResponse toTicketResponse(SupportTicket ticket);

    /**
     * Maps to the full detail view.
     *
     * The 'aiResponse' field on SupportTicket is a JPA entity (AiResponse).
     * MapStruct automatically uses toAiResponseDto() below to convert it,
     * because the source type (AiResponse) matches that method's parameter.
     * When aiResponse is null (PENDING/IN_PROGRESS tickets), MapStruct returns null.
     */
    @Mapping(target = "aiResponse", source = "aiResponse")
    TicketDetailResponse toTicketDetailResponse(SupportTicket ticket);

    /** Maps the nested AiResponse entity to its DTO. */
    AiResponseDto toAiResponseDto(AiResponse aiResponse);
}
