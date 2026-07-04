package com.eagle.generator;

public interface AiProvider {
    String call(String systemPrompt, String userPrompt) throws Exception;
    String getModelName();
    String getProviderName();
}
