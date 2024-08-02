package com.quickbase.datatransfer.gateway;

import com.quickbase.datatransfer.dto.UserDataDTO;
import com.quickbase.datatransfer.service.DataDownloader;
import com.quickbase.datatransfer.service.DataDownloaderIdentifier;
import com.quickbase.datatransfer.service.ExternalSystem;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;

public class GitHubGateway implements ExternalSystem {
    @Service
    public static class UserDataDownloader implements DataDownloader<UserDataDTO> {
        @Override
        public UserDataDTO downloadData(Map<String, String> params) {
            return new UserDataDTO("kzhelyazkova");
        }

        @Override
        public boolean test(DataDownloaderIdentifier dataDownloaderIdentifier) {
            return isGitHubSystemType(dataDownloaderIdentifier.systemType)
                    && UserDataDTO.isUserDataType(dataDownloaderIdentifier.dataType);
        }
    }

    public static boolean isGitHubSystemType(String systemType) {
        return "github".equals(systemType);
    }
}
