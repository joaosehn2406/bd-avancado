package com.jps.jps.user;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class RoleConverter {

    @ReadingConverter
    public static class Read implements Converter<Integer, Role> {
        @Override
        public Role convert(Integer id) {
            return Role.fromId(id);
        }
    }

    @WritingConverter
    public static class Write implements Converter<Role, Integer> {
        @Override
        public Integer convert(Role role) {
            return role.getId();
        }
    }
}
