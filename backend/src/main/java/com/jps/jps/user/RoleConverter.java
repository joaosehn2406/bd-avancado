package com.jps.jps.user;

import com.fasterxml.jackson.databind.util.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class RoleConverter {

    @ReadingConverter
    public static abstract class Read implements Converter<Integer, Role> {
        public Role convert(Integer id) {
            return Role.fromId(id);
        }
    }

    @WritingConverter
    public static abstract class Write implements Converter<Role, Integer> {
        public Integer convert(Role role) {
            return role.getId();
        }
    }
}
