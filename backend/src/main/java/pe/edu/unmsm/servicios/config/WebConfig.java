package pe.edu.unmsm.servicios.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Pages are serialized as { content: [...], page: { size, number, totalElements, totalPages } },
 * a stable contract for the frontend (see docs/07-api.md).
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
