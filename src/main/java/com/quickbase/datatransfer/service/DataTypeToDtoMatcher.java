package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.BaseDTO;
import com.quickbase.datatransfer.dto.UserDTO;
import com.quickbase.datatransfer.common.DataType;

import java.util.Map;

/*
 * This class sole purpose is to provide a check for a match between data type and DTO class,
 * without them knowing about each other.
 */
public class DataTypeToDtoMatcher {
    private static final Map<DataType, Class<? extends BaseDTO>> DATA_TYPE_TO_DTO = Map.of(
            DataType.USER, UserDTO.class
    );

    public static boolean dataTypeMatchesDTO(DataType dataType, Class<? extends BaseDTO> dtoClass) {
        return DATA_TYPE_TO_DTO.containsKey(dataType)
                && DATA_TYPE_TO_DTO.get(dataType) == dtoClass;
    }
}
