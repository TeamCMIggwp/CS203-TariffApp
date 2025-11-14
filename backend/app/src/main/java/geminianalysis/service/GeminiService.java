package geminianalysis.service;

import geminianalysis.GeminiAnalyzer;
import geminianalysis.GeminiResponse;
import geminianalysis.dto.AnalysisRequest;
import geminianalysis.dto.AnalysisResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String ANALYSIS_TYPE_STRUCTURED = "structured";
    private static final String ANALYSIS_TYPE_TEXT = "text";

    @Autowired
    private GeminiAnalyzer geminiAnalyzer;

    /**
     * Analyzes data using Gemini AI
     */
    public AnalysisResponse analyzeData(AnalysisRequest request) {
        logger.info("Processing analysis request");

        try {
            GeminiResponse geminiResponse = geminiAnalyzer.analyzeData(
                request.getData(),
                request.getPrompt()
            );

            if (geminiResponse.isSuccess()) {
                return buildSuccessResponse(geminiResponse);
            } else {
                logger.warn("Analysis failed: {}", geminiResponse.getErrorMessage());
                return new AnalysisResponse(false, geminiResponse.getErrorMessage());
            }

        } catch (IOException e) {
            logger.error("Error communicating with Gemini API", e);
            return new AnalysisResponse(false, "Failed to communicate with Gemini API: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during analysis", e);
            return new AnalysisResponse(false, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Builds a successful analysis response from Gemini response
     */
    private AnalysisResponse buildSuccessResponse(GeminiResponse geminiResponse) {
        String analysisType = geminiResponse.hasJsonAnalysis()
            ? ANALYSIS_TYPE_STRUCTURED
            : ANALYSIS_TYPE_TEXT;

        Object analysis = geminiResponse.hasJsonAnalysis()
            ? geminiResponse.getAnalysisJson()
            : geminiResponse.getRawResponse();

        AnalysisResponse response = new AnalysisResponse(
            true,
            analysisType,
            analysis,
            geminiResponse.getSummary(),
            geminiResponse.getConfidence()
        );

        response.setId(UUID.randomUUID().toString());

        logger.info("Analysis completed successfully");
        return response;
    }
}
