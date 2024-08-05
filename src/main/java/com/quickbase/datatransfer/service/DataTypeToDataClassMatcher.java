package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.data.BaseData;
import com.quickbase.datatransfer.data.UserData;
import com.quickbase.datatransfer.common.DataType;

import java.util.Map;

/*
 * This class sole purpose is to provide a check for a match between data type and data class,
 * without them knowing about each other.
 */
public class DataTypeToDataClassMatcher {
    private static final Map<DataType, Class<? extends BaseData>> DATA_TYPE_TO_DATA_CLASS = Map.of(
            DataType.USER, UserData.class
    );

    public static boolean dataTypeMatchesDataClass(DataType dataType, Class<? extends BaseData> dataClass) {
        return DATA_TYPE_TO_DATA_CLASS.containsKey(dataType)
                && DATA_TYPE_TO_DATA_CLASS.get(dataType) == dataClass;
    }
}
