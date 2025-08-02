package org.yunusgedik.booking.Helper;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper m = new ModelMapper();
        m.getConfiguration()
            .setPropertyCondition(Conditions.isNotNull())
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
            .setPreferNestedProperties(false)
            .setFieldMatchingEnabled(true);
        return m;
    }
}
