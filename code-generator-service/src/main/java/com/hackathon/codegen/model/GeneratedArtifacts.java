package com.hackathon.codegen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedArtifacts {

    private String serviceName;
    private List<GeneratedFile> files;
    private String azureDevOpsBranch;
    private String pullRequestUrl;
    private String commitMessage;
    private Boolean success;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedFile {
        private String fileName;
        private String filePath;
        private String content;
        private FileType fileType;
    }

    public enum FileType {
        KUBERNETES_DEPLOYMENT,
        SPRING_PROPERTIES,
        HELM_VALUES,
        README,
        DOCKERFILE,
        AZURE_PIPELINE
    }
}
